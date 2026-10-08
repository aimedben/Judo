package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.MemberDao
import com.example.data.model.Attendance
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CheckInResult {
    data class Success(val member: Member, val attendance: Attendance) : CheckInResult()
    data class AlreadyCheckedIn(val member: Member, val existingAttendance: Attendance) : CheckInResult()
    data class UnknownQr(val rawCode: String) : CheckInResult()
    data class Error(val message: String) : CheckInResult()
}

data class MemberAttendanceStat(
    val member: Member,
    val totalAttendances: Int,
    val attendancePercentage: Int, // e.g. 98%
    val rank: Int = 0
)

class JudoRepository(
    private val memberDao: MemberDao,
    private val attendanceDao: AttendanceDao
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val activeMembersCount: Flow<Int> = memberDao.countActiveMembersFlow()
    val allAttendance: Flow<List<AttendanceWithMember>> = attendanceDao.getAllAttendanceWithMembers()
    val recentAttendance: Flow<List<AttendanceWithMember>> = attendanceDao.getRecentAttendance(15)

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTodayAttendance(): Flow<List<AttendanceWithMember>> {
        return attendanceDao.getAttendanceForDate(getTodayDateString())
    }

    fun getAttendanceForDate(dateString: String): Flow<List<AttendanceWithMember>> {
        return attendanceDao.getAttendanceForDate(dateString)
    }

    fun getAttendanceForMember(memberId: Long): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForMember(memberId)
    }

    fun getMemberById(memberId: Long): Flow<Member?> {
        return memberDao.getMemberById(memberId)
    }

    suspend fun getMemberByQr(qrCode: String): Member? {
        return memberDao.getMemberByQrCode(qrCode.trim())
    }

    suspend fun processCheckIn(rawCode: String, method: String = "QR_SCAN"): CheckInResult {
        val cleanCode = rawCode.trim()
        var member = memberDao.getMemberByQrCode(cleanCode)

        if (member == null) {
            val extracted = com.example.util.CardDataParser.parseCardData(cleanCode)
            if (extracted.qrCode.isNotBlank()) {
                member = memberDao.getMemberByQrCode(extracted.qrCode)
            }
            if (member == null && extracted.cardNumber.isNotBlank()) {
                member = memberDao.getMemberByQrCode(extracted.cardNumber)
                    ?: memberDao.getMemberByQrCode("JCS-${extracted.cardNumber}")
            }
        }

        if (member == null) {
            return CheckInResult.UnknownQr(cleanCode)
        }

        val today = getTodayDateString()
        val existing = attendanceDao.findTodayAttendance(member.id, today)
        if (existing != null) {
            return CheckInResult.AlreadyCheckedIn(member, existing)
        }

        val nowTime = getCurrentTimeString()
        val attendance = Attendance(
            memberId = member.id,
            dateString = today,
            timeString = nowTime,
            timestamp = System.currentTimeMillis(),
            checkInMethod = method
        )
        val insertedId = attendanceDao.insertAttendance(attendance)
        return CheckInResult.Success(member, attendance.copy(id = insertedId))
    }

    suspend fun manualCheckIn(memberId: Long): CheckInResult {
        val member = memberDao.getMemberByIdDirect(memberId) ?: return CheckInResult.Error("Membre introuvable")
        val today = getTodayDateString()
        val existing = attendanceDao.findTodayAttendance(member.id, today)
        if (existing != null) {
            return CheckInResult.AlreadyCheckedIn(member, existing)
        }
        val attendance = Attendance(
            memberId = member.id,
            dateString = today,
            timeString = getCurrentTimeString(),
            timestamp = System.currentTimeMillis(),
            checkInMethod = "MANUAL"
        )
        val insertedId = attendanceDao.insertAttendance(attendance)
        return CheckInResult.Success(member, attendance.copy(id = insertedId))
    }

    suspend fun associateQrCodeToMember(memberId: Long, qrCode: String) {
        memberDao.updateMemberQrCode(memberId, qrCode.trim())
    }

    suspend fun insertMember(member: Member): Long {
        return memberDao.insertMember(member)
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
    }

    suspend fun deleteAttendance(attendanceId: Long) {
        attendanceDao.deleteAttendanceById(attendanceId)
    }

    suspend fun recordAttendanceForDate(
        memberId: Long,
        dateString: String,
        timeString: String = "18:00"
    ): Attendance {
        val existing = attendanceDao.findTodayAttendance(memberId, dateString)
        if (existing != null) return existing

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            sdf.parse(dateString)
        } catch (_: Exception) {
            null
        }
        val timestamp = date?.time ?: System.currentTimeMillis()

        val att = Attendance(
            memberId = memberId,
            dateString = dateString,
            timeString = timeString,
            timestamp = timestamp,
            checkInMethod = "CALENDAR"
        )
        val id = attendanceDao.insertAttendance(att)
        return att.copy(id = id)
    }

    suspend fun deleteAttendanceForMemberAndDate(memberId: Long, dateString: String) {
        val existing = attendanceDao.findTodayAttendance(memberId, dateString)
        if (existing != null) {
            attendanceDao.deleteAttendanceById(existing.id)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (memberDao.countMembers() > 0) return

        val initialMembers = listOf(
            Member(
                qrCode = "JS-2026-001",
                firstName = "Aimed",
                lastName = "Bensaci",
                birthDate = "2012-04-12",
                phone = "0555 12 34 56",
                groupCode = "U15",
                beltName = "VERTE",
                status = "ACTIF",
                registrationDate = "2024-09-01",
                notes = "Assidu, très bon morote seoi-nage"
            ),
            Member(
                qrCode = "JS-2026-002",
                firstName = "Mohamed",
                lastName = "Haddad",
                birthDate = "2011-08-25",
                phone = "0661 78 90 12",
                groupCode = "U15",
                beltName = "BLEUE",
                status = "ACTIF",
                registrationDate = "2023-09-10",
                notes = "Capitaine adjoint Minimes"
            ),
            Member(
                qrCode = "JS-2026-003",
                firstName = "Yanis",
                lastName = "Amroune",
                birthDate = "2013-01-19",
                phone = "0770 45 67 89",
                groupCode = "U13",
                beltName = "ORANGE",
                status = "ACTIF",
                registrationDate = "2025-01-15"
            ),
            Member(
                qrCode = "JS-2026-004",
                firstName = "Massinissa",
                lastName = "Said",
                birthDate = "2010-11-03",
                phone = "0550 99 88 77",
                groupCode = "U18",
                beltName = "MARRON",
                status = "ACTIF",
                registrationDate = "2022-09-01"
            ),
            Member(
                qrCode = "JS-2026-005",
                firstName = "Lina",
                lastName = "Kaci",
                birthDate = "2014-06-30",
                phone = "0666 33 22 11",
                groupCode = "U13",
                beltName = "JAUNE_ORANGE",
                status = "ACTIF",
                registrationDate = "2025-09-01"
            ),
            Member(
                qrCode = "JS-2026-006",
                firstName = "Sofiane",
                lastName = "Meziani",
                birthDate = "2015-09-14",
                phone = "0772 11 44 55",
                groupCode = "U11",
                beltName = "JAUNE",
                status = "ACTIF",
                registrationDate = "2025-10-01"
            ),
            Member(
                qrCode = "JS-2026-007",
                firstName = "Rayan",
                lastName = "Benali",
                birthDate = "2016-03-22",
                phone = "0558 77 66 55",
                groupCode = "U11",
                beltName = "BLANCHE_JAUNE",
                status = "ACTIF",
                registrationDate = "2026-01-10"
            ),
            Member(
                qrCode = "JS-2026-008",
                firstName = "Ines",
                lastName = "Brahimi",
                birthDate = "2012-07-08",
                phone = "0664 22 88 99",
                groupCode = "U15",
                beltName = "VERTE",
                status = "ACTIF",
                registrationDate = "2024-10-05"
            ),
            Member(
                qrCode = "JS-2026-009",
                firstName = "Amine",
                lastName = "Cherif",
                birthDate = "2008-02-17",
                phone = "0775 88 11 22",
                groupCode = "SENIOR",
                beltName = "NOIRE",
                status = "ACTIF",
                registrationDate = "2020-09-01",
                notes = "Entraîneur bénévole & compétiteur"
            ),
            Member(
                qrCode = "JS-2026-010",
                firstName = "Celia",
                lastName = "Hamadi",
                birthDate = "2017-12-05",
                phone = "0553 44 77 11",
                groupCode = "U9",
                beltName = "BLANCHE",
                status = "ACTIF",
                registrationDate = "2026-09-01"
            ),
            Member(
                qrCode = "JS-2026-011",
                firstName = "Bilal",
                lastName = "Tigrine",
                birthDate = "2011-05-18",
                phone = "0661 44 33 22",
                groupCode = "U15",
                beltName = "BLEUE",
                status = "ACTIF",
                registrationDate = "2023-09-01"
            ),
            Member(
                qrCode = "JS-2026-012",
                firstName = "Kenzi",
                lastName = "Ait Ouali",
                birthDate = "2018-04-10",
                phone = "0771 99 00 22",
                groupCode = "EVEIL",
                beltName = "BLANCHE",
                status = "ACTIF",
                registrationDate = "2026-09-15"
            )
        )
        memberDao.insertMembers(initialMembers)

        val insertedMembers = memberDao.countMembers()
        if (insertedMembers > 0 && attendanceDao.countTotalAttendance() < 10) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 3600 * 1000L
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date(now))

            val historicalList = mutableListOf<Attendance>()

            // Today's attendances (5 present today)
            historicalList.add(Attendance(memberId = 1, dateString = today, timeString = "18:01", timestamp = now - 3600_000L, checkInMethod = "QR_SCAN"))
            historicalList.add(Attendance(memberId = 2, dateString = today, timeString = "18:03", timestamp = now - 3400_000L, checkInMethod = "QR_SCAN"))
            historicalList.add(Attendance(memberId = 3, dateString = today, timeString = "18:04", timestamp = now - 3200_000L, checkInMethod = "QR_SCAN"))
            historicalList.add(Attendance(memberId = 4, dateString = today, timeString = "18:07", timestamp = now - 2800_000L, checkInMethod = "QR_SCAN"))
            historicalList.add(Attendance(memberId = 5, dateString = today, timeString = "18:09", timestamp = now - 2500_000L, checkInMethod = "QR_SCAN"))

            // Days offsets for training days across past 2 months
            val pastDaysOffsets = listOf(
                1L to listOf(1L, 2L, 4L, 6L, 7L, 8L), // Hier
                2L to listOf(2L, 3L, 5L, 6L, 9L),     // Il y a 2 jours
                4L to listOf(1L, 3L, 4L, 7L, 8L, 9L), // Il y a 4 jours (cette semaine)
                6L to listOf(1L, 2L, 5L, 6L, 10L),    // Il y a 6 jours (cette semaine)
                9L to listOf(2L, 3L, 4L, 7L, 8L, 11L),// Il y a 9 jours
                12L to listOf(1L, 2L, 4L, 5L, 6L, 9L),// Il y a 12 jours
                16L to listOf(1L, 3L, 4L, 7L, 8L),    // Il y a 16 jours
                20L to listOf(2L, 4L, 5L, 6L, 9L, 10L),// Il y a 20 jours (ce mois)
                24L to listOf(1L, 2L, 3L, 6L, 7L, 8L),// Il y a 24 jours (ce mois)
                28L to listOf(1L, 4L, 5L, 8L, 9L),    // Il y a 28 jours (ce mois)
                35L to listOf(2L, 3L, 4L, 6L, 7L, 11L),// Il y a 35 jours (mois dernier)
                42L to listOf(1L, 2L, 5L, 8L, 9L),    // Il y a 42 jours
                49L to listOf(1L, 3L, 4L, 6L, 7L, 10L),// Il y a 49 jours
                56L to listOf(2L, 4L, 5L, 8L, 9L, 11L) // Il y a 56 jours (2 mois)
            )

            for ((daysAgo, membersAttended) in pastDaysOffsets) {
                val pastTime = now - (daysAgo * dayMs)
                val dateStr = sdf.format(Date(pastTime))
                for (memberId in membersAttended) {
                    val hour = 17 + (memberId.toInt() % 3)
                    val minute = (memberId.toInt() * 7) % 60
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                    historicalList.add(
                        Attendance(
                            memberId = memberId,
                            dateString = dateStr,
                            timeString = timeStr,
                            timestamp = pastTime - (hour * 3600_000L),
                            checkInMethod = "QR_SCAN"
                        )
                    )
                }
            }

            attendanceDao.insertAll(historicalList)
        }
    }
}

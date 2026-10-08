package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceWithMember
import com.example.data.model.CoachProfile
import com.example.data.model.Member
import com.example.data.repository.CheckInResult
import com.example.data.repository.JudoRepository
import com.example.data.repository.MemberAttendanceStat
import com.example.ui.screens.StatPeriod
import com.example.util.CardDataParser
import com.example.util.ExtractedMemberInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemePreference {
    DARK, LIGHT, SYSTEM
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("judo_seddouk_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(application)
    private val repository = JudoRepository(database.memberDao(), database.attendanceDao())

    private val _themePreference = MutableStateFlow(
        try {
            ThemePreference.valueOf(prefs.getString("theme_pref", ThemePreference.DARK.name) ?: ThemePreference.DARK.name)
        } catch (_: Exception) {
            ThemePreference.DARK
        }
    )
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _coachProfile = MutableStateFlow(loadCoachProfile())
    val coachProfile: StateFlow<CoachProfile> = _coachProfile.asStateFlow()

    private fun loadCoachProfile(): CoachProfile {
        return CoachProfile(
            name = prefs.getString("coach_name", "Sensei Ahmed BOURENANE") ?: "Sensei Ahmed BOURENANE",
            title = prefs.getString("coach_title", "Entraîneur Principal") ?: "Entraîneur Principal",
            beltName = prefs.getString("coach_belt", "NOIRE") ?: "NOIRE",
            danGrade = prefs.getString("coach_dan", "Ceinture Noire 4ème Dan") ?: "Ceinture Noire 4ème Dan",
            phone = prefs.getString("coach_phone", "0782487120") ?: "0782487120",
            photoUrl = prefs.getString("coach_photo_url", "") ?: "",
            clubName = prefs.getString("coach_club", "JUDO CLUB SEDDOUK") ?: "JUDO CLUB SEDDOUK",
            bio = prefs.getString("coach_bio", "Professeur diplômé d'État • Dojo Municipal de Seddouk") ?: "Professeur diplômé d'État • Dojo Municipal de Seddouk"
        )
    }

    fun updateCoachProfile(profile: CoachProfile) {
        _coachProfile.value = profile
        prefs.edit()
            .putString("coach_name", profile.name)
            .putString("coach_title", profile.title)
            .putString("coach_belt", profile.beltName)
            .putString("coach_dan", profile.danGrade)
            .putString("coach_phone", profile.phone)
            .putString("coach_photo_url", profile.photoUrl)
            .putString("coach_club", profile.clubName)
            .putString("coach_bio", profile.bio)
            .apply()
    }

    fun updateCoachPhoto(photoUrl: String) {
        val updated = _coachProfile.value.copy(photoUrl = photoUrl)
        updateCoachProfile(updated)
    }

    val allMembers: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<AttendanceWithMember>> = repository.getTodayAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAttendance: StateFlow<List<AttendanceWithMember>> = repository.recentAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceWithMember>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastScanned = MutableStateFlow<AttendanceWithMember?>(null)
    val lastScanned: StateFlow<AttendanceWithMember?> = _lastScanned.asStateFlow()

    private val _activeCheckInResult = MutableStateFlow<CheckInResult?>(null)
    val activeCheckInResult: StateFlow<CheckInResult?> = _activeCheckInResult.asStateFlow()

    private val _unassignedQrCode = MutableStateFlow<String?>(null)
    val unassignedQrCode: StateFlow<String?> = _unassignedQrCode.asStateFlow()

    private val _scannedMemberForCreation = MutableStateFlow<ExtractedMemberInfo?>(null)
    val scannedMemberForCreation: StateFlow<ExtractedMemberInfo?> = _scannedMemberForCreation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroupFilter = MutableStateFlow("ALL")
    val selectedGroupFilter: StateFlow<String> = _selectedGroupFilter.asStateFlow()

    private var lastScannedCode: String = ""
    private var lastScannedTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
        prefs.edit().putString("theme_pref", pref.name).apply()
    }

    fun toggleTheme() {
        val next = if (_themePreference.value == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
        setThemePreference(next)
    }

    fun onQrScanned(code: String) {
        val now = System.currentTimeMillis()
        val clean = code.trim()
        if (clean.isBlank()) return

        // Debounce exact duplicate scan within 3 seconds
        if (clean == lastScannedCode && (now - lastScannedTimestamp) < 3000) {
            return
        }
        lastScannedCode = clean
        lastScannedTimestamp = now

        viewModelScope.launch {
            val result = repository.processCheckIn(clean)
            triggerHapticFeedback(result)
            when (result) {
                is CheckInResult.Success -> {
                    _activeCheckInResult.value = result
                    _lastScanned.value = AttendanceWithMember(
                        attendanceId = result.attendance.id,
                        memberId = result.member.id,
                        firstName = result.member.firstName,
                        lastName = result.member.lastName,
                        groupCode = result.member.groupCode,
                        beltName = result.member.beltName,
                        dateString = result.attendance.dateString,
                        timeString = result.attendance.timeString,
                        timestamp = result.attendance.timestamp,
                        qrCode = result.member.qrCode,
                        phone = result.member.phone
                    )
                }
                is CheckInResult.AlreadyCheckedIn -> {
                    _activeCheckInResult.value = result
                }
                is CheckInResult.UnknownQr -> {
                    // Try parsing as sports card with member info!
                    val extracted = CardDataParser.parseCardData(clean)
                    if (extracted.firstName.isNotBlank() || extracted.lastName.isNotBlank()) {
                        _scannedMemberForCreation.value = extracted
                    } else {
                        _unassignedQrCode.value = clean
                    }
                    _activeCheckInResult.value = result
                }
                is CheckInResult.Error -> {
                    _activeCheckInResult.value = result
                }
            }
        }
    }

    fun extractCardDataForMemberCreation(rawCardData: String) {
        val extracted = CardDataParser.parseCardData(rawCardData)
        _scannedMemberForCreation.value = extracted
    }

    fun clearScannedMemberForCreation() {
        _scannedMemberForCreation.value = null
    }

    fun manualCheckIn(memberId: Long) {
        viewModelScope.launch {
            val result = repository.manualCheckIn(memberId)
            triggerHapticFeedback(result)
            _activeCheckInResult.value = result
            if (result is CheckInResult.Success) {
                _lastScanned.value = AttendanceWithMember(
                    attendanceId = result.attendance.id,
                    memberId = result.member.id,
                    firstName = result.member.firstName,
                    lastName = result.member.lastName,
                    groupCode = result.member.groupCode,
                    beltName = result.member.beltName,
                    dateString = result.attendance.dateString,
                    timeString = result.attendance.timeString,
                    timestamp = result.attendance.timestamp,
                    qrCode = result.member.qrCode,
                    phone = result.member.phone
                )
            }
        }
    }

    fun recordAttendanceForDate(memberId: Long, dateString: String) {
        viewModelScope.launch {
            repository.recordAttendanceForDate(memberId, dateString)
        }
    }

    fun deleteAttendanceForDate(memberId: Long, dateString: String) {
        viewModelScope.launch {
            repository.deleteAttendanceForMemberAndDate(memberId, dateString)
        }
    }

    fun associateQrToMember(memberId: Long, qrCode: String) {
        viewModelScope.launch {
            repository.associateQrCodeToMember(memberId, qrCode)
            _unassignedQrCode.value = null
            // Automatically check-in after associating!
            val result = repository.processCheckIn(qrCode)
            _activeCheckInResult.value = result
        }
    }

    fun cancelUnassignedQr() {
        _unassignedQrCode.value = null
        if (_activeCheckInResult.value is CheckInResult.UnknownQr) {
            _activeCheckInResult.value = null
        }
    }

    fun dismissCheckInDialog() {
        _activeCheckInResult.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupFilter(groupCode: String) {
        _selectedGroupFilter.value = groupCode
    }

    fun saveMember(member: Member) {
        viewModelScope.launch {
            if (member.id == 0L) {
                val newId = repository.insertMember(member)
                // If member was just created from card, auto check-in them for their session!
                repository.processCheckIn(member.qrCode)
            } else {
                repository.updateMember(member)
            }
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun deleteAttendance(attendanceId: Long) {
        viewModelScope.launch {
            repository.deleteAttendance(attendanceId)
        }
    }

    fun filterAttendancesByPeriod(
        attendances: List<AttendanceWithMember>,
        period: StatPeriod
    ): List<AttendanceWithMember> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 3600 * 1000L
        val todayStr = repository.getTodayDateString()

        return when (period) {
            StatPeriod.TODAY -> attendances.filter { it.dateString == todayStr }
            StatPeriod.WEEK -> {
                val cutoff = now - (7L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.MONTH -> {
                val cutoff = now - (30L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.TWO_MONTHS -> {
                val cutoff = now - (60L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.SEASON -> attendances
        }
    }

    fun getTopAttendanceList(
        allMembersList: List<Member>,
        allAttendances: List<AttendanceWithMember>,
        period: StatPeriod = StatPeriod.SEASON
    ): List<MemberAttendanceStat> {
        if (allMembersList.isEmpty()) return emptyList()
        val periodAttendances = filterAttendancesByPeriod(allAttendances, period)
        val attendanceCountByMember = periodAttendances.groupBy { it.memberId }
            .mapValues { it.value.size }

        val maxAttendance = attendanceCountByMember.values.maxOrNull() ?: 1
        val baselineTotal = when (period) {
            StatPeriod.TODAY -> 1
            StatPeriod.WEEK -> 3.coerceAtLeast(maxAttendance)
            StatPeriod.MONTH -> 10.coerceAtLeast(maxAttendance)
            StatPeriod.TWO_MONTHS -> 18.coerceAtLeast(maxAttendance)
            StatPeriod.SEASON -> 30.coerceAtLeast(maxAttendance)
        }

        return allMembersList.map { member ->
            val count = attendanceCountByMember[member.id] ?: 0
            val pct = if (count == 0) 0 else ((count.toDouble() / baselineTotal.toDouble()) * 100).toInt().coerceIn(1, 100)
            MemberAttendanceStat(
                member = member,
                totalAttendances = count,
                attendancePercentage = pct
            )
        }.sortedWith(
            compareByDescending<MemberAttendanceStat> { it.totalAttendances }
                .thenByDescending { it.attendancePercentage }
        )
    }

    private fun triggerHapticFeedback(result: CheckInResult) {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = when (result) {
                    is CheckInResult.Success -> longArrayOf(0, 70, 50, 90)
                    is CheckInResult.AlreadyCheckedIn -> longArrayOf(0, 150)
                    else -> longArrayOf(0, 100, 100, 100)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}

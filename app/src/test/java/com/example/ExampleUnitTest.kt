package com.example

import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import com.example.ui.screens.StatFilterMode
import com.example.ui.screens.StatPeriod
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun statPeriod_enum_valuesAreCorrect() {
    val periods = StatPeriod.entries
    assertEquals(5, periods.size)
    assertTrue(periods.contains(StatPeriod.TODAY))
    assertTrue(periods.contains(StatPeriod.WEEK))
    assertTrue(periods.contains(StatPeriod.MONTH))
    assertTrue(periods.contains(StatPeriod.TWO_MONTHS))
    assertTrue(periods.contains(StatPeriod.SEASON))
  }

  @Test
  fun statFilterMode_enum_valuesAreCorrect() {
    val modes = StatFilterMode.entries
    assertEquals(4, modes.size)
    assertTrue(modes.contains(StatFilterMode.ABSENTS))
    assertTrue(modes.contains(StatFilterMode.PRESENTS))
    assertTrue(modes.contains(StatFilterMode.ALL_MEMBERS))
    assertTrue(modes.contains(StatFilterMode.ATTENDANCE_RATE))
  }

  @Test
  fun periodFiltering_calculatesPresentsAndAbsentsAccurately() {
    val members = listOf(
      Member(id = 1, qrCode = "JS-01", firstName = "Aimed", lastName = "Bensaci", birthDate = "2012-04-12", phone = "0555", groupCode = "U15", beltName = "VERTE"),
      Member(id = 2, qrCode = "JS-02", firstName = "Mohamed", lastName = "Haddad", birthDate = "2011-08-25", phone = "0661", groupCode = "U15", beltName = "BLEUE"),
      Member(id = 3, qrCode = "JS-03", firstName = "Yanis", lastName = "Amroune", birthDate = "2013-01-19", phone = "0770", groupCode = "U13", beltName = "ORANGE")
    )

    val now = System.currentTimeMillis()
    val dayMs = 24L * 3600 * 1000L

    val attendances = listOf(
      // Member 1 present today
      AttendanceWithMember(
        attendanceId = 1, memberId = 1, firstName = "Aimed", lastName = "Bensaci",
        groupCode = "U15", beltName = "VERTE", dateString = "2026-10-08", timeString = "18:00",
        timestamp = now - 1000L, qrCode = "JS-01", phone = "0555"
      ),
      // Member 2 present 3 days ago (this week)
      AttendanceWithMember(
        attendanceId = 2, memberId = 2, firstName = "Mohamed", lastName = "Haddad",
        groupCode = "U15", beltName = "BLEUE", dateString = "2026-10-05", timeString = "18:00",
        timestamp = now - (3 * dayMs), qrCode = "JS-02", phone = "0661"
      )
    )

    // For Week: Members 1 and 2 are present, Member 3 is absent
    val weekCutoff = now - (7 * dayMs)
    val weekAttendances = attendances.filter { it.timestamp >= weekCutoff }
    val weekPresentIds = weekAttendances.map { it.memberId }.toSet()

    val weekPresents = members.filter { it.id in weekPresentIds }
    val weekAbsents = members.filter { it.id !in weekPresentIds }

    assertEquals(2, weekPresents.size)
    assertEquals(1, weekAbsents.size)
    assertEquals("Yanis", weekAbsents.first().firstName)
  }

  @Test
  fun cardDataParser_parsesJudoClubSeddoukCardAccurately() {
    val sampleQr = """
        JUDO CLUB SEDDOUK
        "
        N: 1

        Adherent: babi BOURENANE

        Ne(e) le: 2013-01-06

        Tel tuteur: 0782487120

        Saison: 2026/2027
        "
    """.trimIndent()

    val extracted = com.example.util.CardDataParser.parseCardData(sampleQr)

    assertEquals("Babi", extracted.firstName)
    assertEquals("BOURENANE", extracted.lastName)
    assertEquals("2013-01-06", extracted.birthDate)
    assertEquals("0782487120", extracted.phone)
    assertEquals("2026/2027", extracted.season)
    assertEquals("1", extracted.cardNumber)
    assertEquals("JCS-1", extracted.qrCode)
    assertEquals(com.example.data.model.JudoGroup.MINIMES, extracted.group)
  }

  @Test
  fun coachProfile_defaultValuesAndModelIntegrity() {
    val coach = com.example.data.model.CoachProfile(
      name = "Sensei Ahmed BOURENANE",
      danGrade = "Ceinture Noire 4ème Dan",
      photoUrl = "content://media/external/images/media/42"
    )

    assertEquals("Sensei Ahmed BOURENANE", coach.name)
    assertEquals("Ceinture Noire 4ème Dan", coach.danGrade)
    assertEquals("content://media/external/images/media/42", coach.photoUrl)
  }
}

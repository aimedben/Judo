package com.example

import com.example.data.model.Belt
import com.example.data.model.JudoGroup
import com.example.data.model.Member
import com.example.util.CardDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JudoLogicTest {

    @Test
    fun testBeltParsing() {
        val beltVerte = Belt.fromString("VERTE")
        assertEquals(Belt.VERTE, beltVerte)
        assertEquals("Verte", beltVerte.displayName)

        val beltBlancheJaune = Belt.fromString("BLANCHE_JAUNE")
        assertEquals(Belt.BLANCHE_JAUNE, beltBlancheJaune)
        assertNotNull(beltBlancheJaune.secondaryColor)
    }

    @Test
    fun testGroupParsing() {
        val groupU15 = JudoGroup.fromString("U15")
        assertEquals(JudoGroup.MINIMES, groupU15)
        assertEquals("12-13 ans", groupU15.ageRange)
    }

    @Test
    fun testMemberFullName() {
        val member = Member(
            id = 1,
            qrCode = "JS-2026-001",
            firstName = "Aimed",
            lastName = "Bensaci",
            birthDate = "2012-04-12",
            phone = "0555 12 34 56",
            groupCode = "U15",
            beltName = "VERTE"
        )
        assertEquals("Aimed Bensaci", member.fullName)
        assertEquals(Belt.VERTE, member.belt)
        assertEquals(JudoGroup.MINIMES, member.group)
    }

    @Test
    fun testCardDataParserJson() {
        val json = """
            {"nom":"Belaid","prenom":"Karim","naissance":"2012-09-14","groupe":"U15","ceinture":"VERTE","tel":"0552 44 88 12","id":"JS-CARD-2026-777"}
        """.trimIndent()
        val parsed = CardDataParser.parseCardData(json)
        assertEquals("Karim", parsed.firstName)
        assertEquals("Belaid", parsed.lastName)
        assertEquals("2012-09-14", parsed.birthDate)
        assertEquals(JudoGroup.MINIMES, parsed.group)
        assertEquals(Belt.VERTE, parsed.belt)
        assertEquals("JS-CARD-2026-777", parsed.qrCode)
        assertEquals("0552 44 88 12", parsed.phone)
    }

    @Test
    fun testCardDataParserDelimited() {
        val delimited = "JS-CARD-99|Mansouri|Sarah|2014-02-10|U13|ORANGE|0667123456"
        val parsed = CardDataParser.parseCardData(delimited)
        assertEquals("Sarah", parsed.firstName)
        assertEquals("Mansouri", parsed.lastName)
        assertEquals("2014-02-10", parsed.birthDate)
        assertEquals(JudoGroup.BENJAMINS, parsed.group)
        assertEquals(Belt.ORANGE, parsed.belt)
        assertEquals("JS-CARD-99", parsed.qrCode)
    }

    @Test
    fun testCardDataParserVCard() {
        val vcard = """
            BEGIN:VCARD
            FN:Amine Dahmani
            TEL:0771 22 33 44
            NOTE:U18 Marron
            END:VCARD
        """.trimIndent()
        val parsed = CardDataParser.parseCardData(vcard)
        assertEquals("Amine", parsed.firstName)
        assertEquals("Dahmani", parsed.lastName)
        assertEquals("0771 22 33 44", parsed.phone)
    }
}

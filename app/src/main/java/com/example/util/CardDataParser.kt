package com.example.util

import com.example.data.model.Belt
import com.example.data.model.JudoGroup
import java.util.Locale

data class ExtractedMemberInfo(
    val cardNumber: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "2013-01-06",
    val phone: String = "",
    val season: String = "2026/2027",
    val group: JudoGroup = JudoGroup.MINIMES,
    val belt: Belt = Belt.BLANCHE,
    val qrCode: String = "",
    val photoUrl: String = "",
    val clubName: String = "JUDO CLUB SEDDOUK",
    val notes: String = "",
    val rawScannedData: String = ""
)

object CardDataParser {

    /**
     * Tries multiple standard formats for sports cards / QR codes:
     * 1. Judo Club Seddouk official card format:
     *    JUDO CLUB SEDDOUK
     *    "
     *    N: 1
     *    Adherent: babi BOURENANE
     *    Ne(e) le: 2013-01-06
     *    Tel tuteur: 0782487120
     *    Saison: 2026/2027
     *    "
     * 2. JSON format: {"nom":"Haddad","prenom":"Mohamed","naissance":"2011-08-25", ...}
     * 3. vCard format: "BEGIN:VCARD\nFN:Mohamed Haddad\nTEL:...\nEND:VCARD"
     * 4. Generic Key-Value text
     * 5. Pipe or semicolon-separated
     * 6. Fallback card ID / License code
     */
    fun parseCardData(raw: String): ExtractedMemberInfo {
        val trimmed = raw.trim()

        // 1. Try Judo Club Seddouk / Sports Card Key-Value format first!
        tryParseJudoClubCard(trimmed)?.let { return it }

        // 2. Try JSON
        tryParseJson(trimmed)?.let { return it }

        // 3. Try vCard
        tryParseVCard(trimmed)?.let { return it }

        // 4. Try Generic Key-Value
        tryParseGenericKeyValue(trimmed)?.let { return it }

        // 5. Try Pipe or Semicolon Delimited
        tryParseDelimited(trimmed)?.let { return it }

        // 6. Fallback: Card ID / License Code
        val generatedCode = if (trimmed.length > 30) trimmed.take(24) else trimmed
        return ExtractedMemberInfo(
            qrCode = generatedCode,
            notes = "Carte scannée: $generatedCode",
            rawScannedData = trimmed
        )
    }

    /**
     * Specifically handles sports card QR codes like:
     * JUDO CLUB SEDDOUK
     * "
     * N: 1
     * Adherent: babi BOURENANE
     * Ne(e) le: 2013-01-06
     * Tel tuteur: 0782487120
     * Saison: 2026/2027
     * "
     */
    private fun tryParseJudoClubCard(text: String): ExtractedMemberInfo? {
        val cleaned = text.replace("\"", "")
            .replace("“", "")
            .replace("”", "")
            .trim()

        // Check if there is an adherent or member indicator
        val hasAdherent = cleaned.contains("adherent", ignoreCase = true) ||
                cleaned.contains("adhérent", ignoreCase = true) ||
                cleaned.contains("ne(e) le", ignoreCase = true) ||
                cleaned.contains("né(e) le", ignoreCase = true) ||
                cleaned.contains("tel tuteur", ignoreCase = true) ||
                cleaned.contains("saison", ignoreCase = true) ||
                cleaned.contains("judo club seddouk", ignoreCase = true)

        if (!hasAdherent) return null

        var cardNumber = ""
        var adherentRaw = ""
        var birthDate = ""
        var phone = ""
        var season = ""
        var clubName = "JUDO CLUB SEDDOUK"
        var beltStr = ""
        var groupStr = ""
        var photoUrl = ""

        // Process line by line
        cleaned.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach

            if (line.contains("judo club seddouk", ignoreCase = true)) {
                clubName = "JUDO CLUB SEDDOUK"
                return@forEach
            }

            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase(Locale.ROOT)
                val value = line.substring(colonIndex + 1).trim()

                when {
                    key == "n" || key == "n°" || key == "numero" || key == "numéro" || key == "id" || key == "carte" -> {
                        cardNumber = value
                    }
                    key == "adherent" || key == "adhérent" || key == "membre" || key == "judoka" ||
                            key.contains("nom & prénom") || key.contains("nom et prénom") -> {
                        adherentRaw = value
                    }
                    key.contains("ne(e) le") || key.contains("né(e) le") || key.contains("ne le") ||
                            key.contains("née le") || key.contains("naissance") || key.contains("date") -> {
                        birthDate = value
                    }
                    key.contains("tuteur") || key.contains("tel") || key.contains("tél") ||
                            key.contains("telephone") || key.contains("téléphone") || key.contains("phone") -> {
                        phone = value
                    }
                    key.contains("saison") || key.contains("season") -> {
                        season = value
                    }
                    key.contains("ceinture") || key.contains("grade") || key.contains("belt") -> {
                        beltStr = value
                    }
                    key.contains("groupe") || key.contains("catégorie") || key.contains("categorie") || key.contains("group") -> {
                        groupStr = value
                    }
                    key.contains("photo") || key.contains("image") || key.contains("avatar") -> {
                        photoUrl = value
                    }
                }
            }
        }

        // Regex fallbacks if line-by-line missed anything
        if (adherentRaw.isBlank()) {
            val nameMatch = Regex("""(?i)(?:adherent|adhérent|membre|judoka)\s*[:=]?\s*([^\n\r]+)""").find(cleaned)
            nameMatch?.let { adherentRaw = it.groupValues[1].trim() }
        }
        if (cardNumber.isBlank()) {
            val nMatch = Regex("""(?i)(?:^|\n|\s)(?:n|n°|numéro|numero)\s*[:=.]?\s*([0-9a-zA-Z_-]+)""").find(cleaned)
            nMatch?.let { cardNumber = it.groupValues[1].trim() }
        }
        if (birthDate.isBlank()) {
            val dobMatch = Regex("""([0-9]{4}[-/][0-9]{2}[-/][0-9]{2}|[0-9]{2}[-/][0-9]{2}[-/][0-9]{4})""").find(cleaned)
            dobMatch?.let { birthDate = it.groupValues[1].trim() }
        }
        if (phone.isBlank()) {
            val phoneMatch = Regex("""(?i)(?:tel|tél|tuteur|contact)\s*[:=.]?\s*([0-9\s+.-]{8,15})""").find(cleaned)
            phoneMatch?.let { phone = it.groupValues[1].trim() }
        }
        if (season.isBlank()) {
            val seasonMatch = Regex("""(?i)(?:saison|season)\s*[:=.]?\s*([0-9]{4}\s*[/_-]\s*[0-9]{4})""").find(cleaned)
            seasonMatch?.let { season = it.groupValues[1].trim() }
        }

        if (adherentRaw.isBlank() && cardNumber.isBlank()) {
            return null
        }

        // Normalize birth date to YYYY-MM-DD
        val normalizedBirthDate = normalizeDate(birthDate).ifBlank { "2013-01-06" }

        // Parse first and last names smartly
        val (firstName, lastName) = parseFirstAndLastName(adherentRaw)

        // Compute Judo category based on birth date
        val group = if (groupStr.isNotBlank()) JudoGroup.fromString(groupStr) else JudoGroup.fromBirthDate(normalizedBirthDate)
        val belt = if (beltStr.isNotBlank()) Belt.fromString(beltStr) else Belt.BLANCHE

        // Construct a clean, unique QR identifier from card number
        val qrCode = when {
            cardNumber.isNotBlank() && cardNumber.all { it.isDigit() } -> "JCS-$cardNumber"
            cardNumber.isNotBlank() -> cardNumber
            else -> "JCS-${System.currentTimeMillis().toString().takeLast(5)}"
        }

        val noteText = buildString {
            append("Inscrit via carte $clubName")
            if (cardNumber.isNotBlank()) append(" (N° $cardNumber)")
            if (season.isNotBlank()) append(" - Saison $season")
        }

        return ExtractedMemberInfo(
            cardNumber = cardNumber,
            firstName = firstName,
            lastName = lastName,
            birthDate = normalizedBirthDate,
            phone = phone.replace(" ", "").trim(),
            season = season.ifBlank { "2026/2027" },
            group = group,
            belt = belt,
            qrCode = qrCode,
            photoUrl = photoUrl,
            clubName = clubName,
            notes = noteText,
            rawScannedData = text
        )
    }

    /**
     * Intelligently parses French/International sport card name conventions.
     * e.g. "babi BOURENANE" -> firstName: "Babi", lastName: "BOURENANE"
     * e.g. "BOURENANE babi" -> firstName: "Babi", lastName: "BOURENANE"
     * e.g. "Mohamed Haddad" -> firstName: "Mohamed", lastName: "Haddad"
     */
    private fun parseFirstAndLastName(fullNameRaw: String): Pair<String, String> {
        val trimmed = fullNameRaw.trim()
        if (trimmed.isBlank()) return Pair("", "")

        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return Pair("", "")
        if (parts.size == 1) return Pair(capitalizeWord(parts[0]), "")

        val upperParts = parts.filter { it.length > 1 && it.all { ch -> ch.isUpperCase() || !ch.isLetter() } }
        val nonUpperParts = parts.filterNot { it.length > 1 && it.all { ch -> ch.isUpperCase() || !ch.isLetter() } }

        return if (upperParts.isNotEmpty() && nonUpperParts.isNotEmpty()) {
            val lastName = upperParts.joinToString(" ")
            val firstName = nonUpperParts.joinToString(" ") { capitalizeWord(it) }
            Pair(firstName, lastName)
        } else {
            // First token is first name, remaining are last name
            val firstName = capitalizeWord(parts.first())
            val lastName = parts.drop(1).joinToString(" ")
            Pair(firstName, lastName)
        }
    }

    private fun capitalizeWord(word: String): String {
        return word.lowercase(Locale.ROOT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    private fun normalizeDate(rawDate: String): String {
        val clean = rawDate.trim()
        if (clean.isBlank()) return ""
        // Check YYYY-MM-DD
        val ymd = Regex("""(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})""").find(clean)
        if (ymd != null) {
            val y = ymd.groupValues[1]
            val m = ymd.groupValues[2].padStart(2, '0')
            val d = ymd.groupValues[3].padStart(2, '0')
            return "$y-$m-$d"
        }
        // Check DD/MM/YYYY
        val dmy = Regex("""(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})""").find(clean)
        if (dmy != null) {
            val d = dmy.groupValues[1].padStart(2, '0')
            val m = dmy.groupValues[2].padStart(2, '0')
            val y = dmy.groupValues[3]
            return "$y-$m-$d"
        }
        return clean
    }

    private fun tryParseJson(text: String): ExtractedMemberInfo? {
        if (!text.startsWith("{") || !text.endsWith("}")) return null
        return try {
            fun extractField(vararg keys: String): String {
                for (key in keys) {
                    val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
                    val match = pattern.find(text)
                    if (match != null) return match.groupValues[1]
                }
                return ""
            }

            val lastName = extractField("nom", "lastName", "last_name")
            val firstName = extractField("prenom", "prénom", "firstName", "first_name")
            val birthDate = extractField("naissance", "birthDate", "birth").ifBlank { "2013-01-06" }
            val phone = extractField("tel", "phone", "telephone", "tuteur")
            val groupStr = extractField("groupe", "group").ifBlank { "U15" }
            val beltStr = extractField("ceinture", "belt").ifBlank { "BLANCHE" }
            val cardNum = extractField("n", "numero", "id", "code")
            val qrCode = extractField("qr", "id", "code").ifBlank { "JCS-${System.currentTimeMillis().toString().takeLast(5)}" }
            val season = extractField("saison", "season").ifBlank { "2026/2027" }
            val photoUrl = extractField("photo", "photoUrl", "avatar")
            val notes = extractField("notes").ifBlank { "Importé par scan de carte (JSON)" }

            if (lastName.isNotBlank() || firstName.isNotBlank() || qrCode.isNotBlank()) {
                ExtractedMemberInfo(
                    cardNumber = cardNum,
                    firstName = capitalizeWord(firstName.trim()),
                    lastName = lastName.trim(),
                    birthDate = normalizeDate(birthDate).ifBlank { "2013-01-06" },
                    phone = phone.trim(),
                    season = season,
                    group = JudoGroup.fromString(groupStr),
                    belt = Belt.fromString(beltStr),
                    qrCode = qrCode.trim(),
                    photoUrl = photoUrl.trim(),
                    notes = notes,
                    rawScannedData = text
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseVCard(text: String): ExtractedMemberInfo? {
        if (!text.contains("BEGIN:VCARD", ignoreCase = true)) return null
        var fn = ""
        var tel = ""
        var note = ""
        var photo = ""

        text.lines().forEach { line ->
            val l = line.trim()
            when {
                l.startsWith("FN:", ignoreCase = true) -> fn = l.substring(3).trim()
                l.startsWith("TEL:", ignoreCase = true) -> tel = l.substring(4).trim()
                l.startsWith("NOTE:", ignoreCase = true) -> note = l.substring(5).trim()
                l.startsWith("PHOTO:", ignoreCase = true) -> photo = l.substring(6).trim()
            }
        }

        val (firstName, lastName) = parseFirstAndLastName(fn)

        return ExtractedMemberInfo(
            firstName = firstName,
            lastName = lastName,
            phone = tel,
            photoUrl = photo,
            notes = note.ifBlank { "Extrait depuis vCard de sport" },
            qrCode = "JCS-VCARD-${System.currentTimeMillis().toString().takeLast(5)}",
            rawScannedData = text
        )
    }

    private fun tryParseGenericKeyValue(text: String): ExtractedMemberInfo? {
        if (!text.contains(":") || text.lines().size < 2) return null
        val map = mutableMapOf<String, String>()

        text.lines().forEach { line ->
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase(Locale.ROOT)
                val value = line.substring(colonIndex + 1).trim()
                map[key] = value
            }
        }

        if (map.containsKey("nom") || map.containsKey("prénom") || map.containsKey("prenom") ||
            map.containsKey("adherent") || map.containsKey("adhérent") || map.containsKey("carte")
        ) {
            val lastName = map["nom"] ?: map["lastname"] ?: ""
            val firstName = map["prénom"] ?: map["prenom"] ?: map["firstname"] ?: ""
            val birthDate = map["naissance"] ?: map["date"] ?: map["né"] ?: map["ne"] ?: "2013-01-06"
            val phone = map["tél"] ?: map["tel"] ?: map["telephone"] ?: map["phone"] ?: map["tuteur"] ?: ""
            val groupStr = map["groupe"] ?: map["catégorie"] ?: map["categorie"] ?: "U15"
            val beltStr = map["ceinture"] ?: map["grade"] ?: "BLANCHE"
            val cardNum = map["n"] ?: map["n°"] ?: map["carte"] ?: map["id"] ?: ""
            val photoUrl = map["photo"] ?: map["avatar"] ?: ""
            val season = map["saison"] ?: "2026/2027"

            val finalFirstName = if (firstName.isNotBlank()) capitalizeWord(firstName) else ""
            val finalLastName = if (lastName.isNotBlank()) lastName.trim() else ""

            return ExtractedMemberInfo(
                cardNumber = cardNum,
                firstName = finalFirstName,
                lastName = finalLastName,
                birthDate = normalizeDate(birthDate).ifBlank { "2013-01-06" },
                phone = phone,
                season = season,
                group = JudoGroup.fromString(groupStr),
                belt = Belt.fromString(beltStr),
                qrCode = if (cardNum.isNotBlank()) "JCS-$cardNum" else "JCS-${System.currentTimeMillis().toString().takeLast(5)}",
                photoUrl = photoUrl,
                notes = "Extrait de carte clé-valeur",
                rawScannedData = text
            )
        }
        return null
    }

    private fun tryParseDelimited(text: String): ExtractedMemberInfo? {
        val delimiter = when {
            text.contains("|") -> "|"
            text.contains(";") -> ";"
            else -> null
        } ?: return null

        val parts = text.split(delimiter).map { it.trim() }
        if (parts.size >= 4) {
            var offset = 0
            var id = ""
            if (parts[0].startsWith("JS") || parts[0].startsWith("JCS") || parts[0].startsWith("LIC") || (parts[0].length < 15 && !parts[0].contains(" "))) {
                id = parts[0]
                offset = 1
            }

            val lastName = parts.getOrNull(offset) ?: ""
            val firstName = parts.getOrNull(offset + 1) ?: ""
            val birthDate = parts.getOrNull(offset + 2) ?: "2013-01-06"
            val groupStr = parts.getOrNull(offset + 3) ?: "U15"
            val beltStr = parts.getOrNull(offset + 4) ?: "BLANCHE"
            val phone = parts.getOrNull(offset + 5) ?: ""

            return ExtractedMemberInfo(
                firstName = capitalizeWord(firstName),
                lastName = lastName.trim(),
                birthDate = normalizeDate(birthDate).ifBlank { "2013-01-06" },
                phone = phone,
                group = JudoGroup.fromString(groupStr),
                belt = Belt.fromString(beltStr),
                qrCode = id.ifBlank { "JCS-${System.currentTimeMillis().toString().takeLast(5)}" },
                notes = "Extrait automatiquement par carte délimitée",
                rawScannedData = text
            )
        }
        return null
    }
}

package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.JudoRed

enum class StatPeriod(
    val title: String,
    val shortLabel: String,
    val description: String,
    val daysCount: Int
) {
    TODAY(
        title = "Aujourd'hui",
        shortLabel = "Même jour",
        description = "Séance du jour",
        daysCount = 1
    ),
    WEEK(
        title = "Cette semaine",
        shortLabel = "Semaine",
        description = "7 derniers jours",
        daysCount = 7
    ),
    MONTH(
        title = "Ce mois",
        shortLabel = "Mois",
        description = "30 derniers jours",
        daysCount = 30
    ),
    TWO_MONTHS(
        title = "Les 2 mois",
        shortLabel = "2 Mois",
        description = "60 derniers jours",
        daysCount = 60
    ),
    SEASON(
        title = "Saison 2025-2026",
        shortLabel = "Saison",
        description = "Année sportive complète",
        daysCount = 365
    )
}

enum class StatFilterMode(
    val title: String,
    val subtitle: String,
    val accentColor: Color
) {
    ABSENTS(
        title = "Judokas Absents",
        subtitle = "Membres non pointés sur la période",
        accentColor = Color(0xFFE53935)
    ),
    PRESENTS(
        title = "Judokas Présents",
        subtitle = "Membres ayant participé sur la période",
        accentColor = Color(0xFF2E7D32)
    ),
    ALL_MEMBERS(
        title = "Tous les Inscrits",
        subtitle = "Effectif total du club Judo Seddouk",
        accentColor = JudoRed
    ),
    ATTENDANCE_RATE(
        title = "Taux de Présence",
        subtitle = "Participation et assiduité aux entraînements",
        accentColor = JudoRed
    )
}

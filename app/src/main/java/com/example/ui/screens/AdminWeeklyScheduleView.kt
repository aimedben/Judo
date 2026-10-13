package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoachUser
import com.example.data.model.JudoCategory
import com.example.data.model.RecurrenceScope
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionConflict
import com.example.data.model.SessionStatus
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

enum class ScheduleViewMode {
    WEEK_AGENDA, // Jour par jour / Agenda responsive
    LIST_VIEW,    // Liste chronologique
    TABLE_GRID    // Grille 7 colonnes pour tablette
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWeeklyScheduleView(
    sessions: List<ScheduledTrainingSession>,
    categories: List<JudoCategory>,
    coaches: List<CoachUser>,
    selectedCategoryIdFilter: String = "ALL",
    onCategoryFilterChange: (String) -> Unit = {},
    onAddNewSession: (dayName: String?, timeSlot: String?) -> Unit,
    onEditSession: (ScheduledTrainingSession) -> Unit,
    onDuplicateSession: (ScheduledTrainingSession) -> Unit,
    onDeleteSession: (String, RecurrenceScope) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("Samedi", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi")

    var selectedCoachFilter by remember { mutableStateOf("ALL") }
    var selectedDayIndex by remember { mutableStateOf(2) } // default Mercredi (index 4) or Lundi (index 2)
    var weekOffset by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.WEEK_AGENDA) }

    var sessionForDetailsDialog by remember { mutableStateOf<ScheduledTrainingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ScheduledTrainingSession?>(null) }

    // Filter sessions
    val filteredSessions = remember(sessions, selectedCategoryIdFilter, selectedCoachFilter) {
        sessions.filter { s ->
            val matchCategory = selectedCategoryIdFilter == "ALL" || s.categoryId.equals(selectedCategoryIdFilter, ignoreCase = true)
            val matchCoach = selectedCoachFilter == "ALL" || s.coachNames.any { it.equals(selectedCoachFilter, ignoreCase = true) }
            matchCategory && matchCoach && s.status != SessionStatus.CANCELLED
        }.sortedWith(compareBy({ it.dayOfWeekName }, { it.startTime }))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val activeViewMode = if (isTablet && viewMode == ScheduleViewMode.WEEK_AGENDA) ScheduleViewMode.TABLE_GRID else viewMode

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("admin_weekly_schedule_view"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Navigation & Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Planning Hebdomadaire",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = when (weekOffset) {
                            0 -> "Cette semaine • Saison en cours"
                            1 -> "Semaine prochaine (+1)"
                            -1 -> "Semaine précédente (-1)"
                            else -> if (weekOffset > 0) "+$weekOffset semaines" else "$weekOffset semaines"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Switch view mode icon button
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == ScheduleViewMode.LIST_VIEW) ScheduleViewMode.WEEK_AGENDA else ScheduleViewMode.LIST_VIEW
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == ScheduleViewMode.LIST_VIEW) Icons.Default.ViewWeek else Icons.Default.ViewAgenda,
                            contentDescription = "Changer de vue",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Add session primary button
                    Button(
                        onClick = { onAddNewSession(daysOfWeek.getOrNull(selectedDayIndex), "17:30") },
                        colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_schedule_session_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Planifier")
                    }
                }
            }

            // Week Switcher (Précédente / Aujourd'hui / Suivante)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { weekOffset -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Semaine précédente", modifier = Modifier.size(16.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = JudoRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (weekOffset == 0) "Semaine Actuelle" else "Semaine ${if (weekOffset > 0) "+$weekOffset" else "$weekOffset"}",
                            fontWeight = FontWeight.Bold
                        )
                        if (weekOffset != 0) {
                            OutlinedButton(
                                onClick = { weekOffset = 0 },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Aujourd'hui", fontSize = 11.sp)
                            }
                        }
                    }

                    IconButton(onClick = { weekOffset += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Semaine suivante", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Filters Row: Categories & Coach Filter
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category All
                item {
                    FilterChip(
                        selected = selectedCategoryIdFilter == "ALL",
                        onClick = { onCategoryFilterChange("ALL") },
                        label = { Text("Toutes catégories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JudoRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Individual categories
                items(categories.filter { it.isActive }) { cat ->
                    val isSelected = selectedCategoryIdFilter == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryFilterChange(if (isSelected) "ALL" else cat.id) },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JudoRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Secondary Coach filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCoachFilter == "ALL",
                        onClick = { selectedCoachFilter = "ALL" },
                        label = { Text("Tous les coachs", fontSize = 12.sp) }
                    )
                }
                items(coaches.filter { it.isActive }) { coach ->
                    val isSelected = selectedCoachFilter == coach.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCoachFilter = if (isSelected) "ALL" else coach.name },
                        label = { Text(coach.name.split(" ").takeLast(2).joinToString(" "), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            // Content based on view mode
            when (activeViewMode) {
                ScheduleViewMode.WEEK_AGENDA -> {
                    // Mobile Day Selector Tabs
                    TabRow(
                        selectedTabIndex = selectedDayIndex,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val dayCount = filteredSessions.count { it.dayOfWeekName.equals(day, ignoreCase = true) }
                            Tab(
                                selected = selectedDayIndex == index,
                                onClick = { selectedDayIndex = index },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day.take(3), fontWeight = FontWeight.Bold)
                                        if (dayCount > 0) {
                                            Text(
                                                "$dayCount",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (selectedDayIndex == index) JudoRed else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Sessions for selected day
                    val currentDay = daysOfWeek[selectedDayIndex]
                    val daySessions = filteredSessions.filter { it.dayOfWeekName.equals(currentDay, ignoreCase = true) }

                    if (daySessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                Text(
                                    text = "Aucune séance programmée le $currentDay",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                OutlinedButton(
                                    onClick = { onAddNewSession(currentDay, "17:30") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Planifier sur ce créneau")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(daySessions) { session ->
                                SessionCardBlock(
                                    session = session,
                                    onClick = { sessionForDetailsDialog = session }
                                )
                            }
                        }
                    }
                }

                ScheduleViewMode.LIST_VIEW -> {
                    if (filteredSessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune séance trouvée avec les filtres sélectionnés.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredSessions) { session ->
                                SessionCardBlock(
                                    session = session,
                                    onClick = { sessionForDetailsDialog = session }
                                )
                            }
                        }
                    }
                }

                ScheduleViewMode.TABLE_GRID -> {
                    // Tablet 7 columns Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            val daySessions = filteredSessions.filter { it.dayOfWeekName.equals(day, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = JudoRed.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = JudoRed),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        daySessions.forEach { session ->
                                            SessionMiniGridCard(
                                                session = session,
                                                onClick = { sessionForDetailsDialog = session }
                                            )
                                        }

                                        // Empty slot click to add
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onAddNewSession(day, "18:00") }
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SESSION DETAILS & ACTIONS DIALOG
    val activeDetail = sessionForDetailsDialog
    if (activeDetail != null) {
        AlertDialog(
            onDismissRequest = { sessionForDetailsDialog = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(activeDetail.categoryId))
                    )
                    Text(
                        text = "${activeDetail.categoryName} • ${activeDetail.dayOfWeekName}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Créneau : ${activeDetail.startTime} - ${activeDetail.endTime} (${activeDetail.durationMinutes} min)",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Coach(s) affecté(s) : ${activeDetail.coachNames.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (activeDetail.isRecurringWeekly) "Répétition : Chaque semaine" else "Séance ponctuelle",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            if (activeDetail.notes.isNotBlank()) {
                                Text(
                                    text = "Notes : ${activeDetail.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = JudoGold, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val target = activeDetail
                            sessionForDetailsDialog = null
                            onEditSession(target)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modifier")
                    }

                    OutlinedButton(
                        onClick = {
                            val target = activeDetail
                            sessionForDetailsDialog = null
                            onDuplicateSession(target)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dupliquer")
                    }

                    Button(
                        onClick = {
                            sessionToDelete = activeDetail
                            sessionForDetailsDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Annuler")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionForDetailsDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // CONFIRM DELETE / CANCEL DIALOG
    val deleting = sessionToDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Annuler cette séance ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Êtes-vous sûr de vouloir annuler la séance de ${deleting.categoryName} (${deleting.dayOfWeekName} ${deleting.startTime} - ${deleting.endTime}) ?")
                    if (deleting.isRecurringWeekly && deleting.seriesId != null) {
                        Text(
                            "Cette séance fait partie d'une série hebdomadaire récurrente.",
                            style = MaterialTheme.typography.bodySmall.copy(color = JudoGold, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (deleting.isRecurringWeekly && deleting.seriesId != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteSession(deleting.id, RecurrenceScope.ALL_SERIES)
                                sessionToDelete = null
                            }
                        ) {
                            Text("Toute la série")
                        }
                    }

                    Button(
                        onClick = {
                            onDeleteSession(deleting.id, RecurrenceScope.THIS_ONLY)
                            sessionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmer l'annulation")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Retour")
                }
            }
        )
    }
}

@Composable
private fun SessionCardBlock(
    session: ScheduledTrainingSession,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(session.categoryId)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = session.categoryName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = session.groupCode,
                                style = MaterialTheme.typography.labelSmall.copy(color = categoryColor, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${session.startTime} - ${session.endTime} (${session.durationMinutes} min)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Text(
                        text = "Coach(s) : ${session.coachNames.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (session.isRecurringWeekly) JudoSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (session.isRecurringWeekly) "Hebdo" else "Unique",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (session.isRecurringWeekly) JudoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionMiniGridCard(
    session: ScheduledTrainingSession,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(session.categoryId)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = categoryColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${session.startTime} - ${session.endTime}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = categoryColor)
            )
            Text(
                text = session.categoryName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = session.coachNames.firstOrNull()?.split(" ")?.takeLast(1)?.firstOrNull() ?: "",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1
            )
        }
    }
}

fun getCategoryColor(categoryId: String): Color {
    return when (categoryId.lowercase()) {
        "baby" -> Color(0xFFE91E63) // Rose
        "poussins" -> Color(0xFFFF9800) // Orange
        "benjamins" -> Color(0xFF2196F3) // Bleu
        "minimes" -> Color(0xFF4CAF50) // Vert
        "cadets" -> Color(0xFF9C27B0) // Violet
        "adultes" -> Color(0xFF3F51B5) // Indigo
        else -> JudoRed
    }
}

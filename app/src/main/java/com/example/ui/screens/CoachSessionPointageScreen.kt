package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.AttendanceWithMember
import com.example.data.model.CoachScanFeedback
import com.example.data.model.Member
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 3 — Pointage (Check-in during training)
 * Essential stats: presents, absents, attendance rate.
 * Big button: "Scanner un judoka" if camera mode.
 * Indicator: "Douchette prête" in external barcode mode.
 * Auto-handled continuous barcode input in douchette mode.
 * Real-time instant visual feedback banner:
 *   - Green: Present & cotisation up-to-date
 *   - Orange: Present but cotisation late
 *   - Red: Invalid QR or unknown judoka
 * Manual check-in button to mark a judoka present manually.
 * Recent check-ins list.
 * Finish session button.
 */
@Composable
fun CoachSessionPointageScreen(
    session: ActiveTrainingSession,
    categoryMembers: List<Member>,
    sessionAttendances: List<AttendanceWithMember>,
    scanFeedback: CoachScanFeedback?,
    onOpenScanner: () -> Unit,
    onQrScanned: (String) -> Unit,
    onManualCheckInMember: (Member) -> Unit,
    onFinishSession: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categoryMembers.size
    val presentCount = sessionAttendances.size
    val absentCount = (totalCount - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalCount > 0) ((presentCount.toDouble() / totalCount.toDouble()) * 100).toInt() else 0

    var isManualCheckInDialogOpen by remember { mutableStateOf(false) }
    var douchetteInput by remember { mutableStateOf("") }
    val douchetteFocusRequester = remember { FocusRequester() }

    // Auto-focus douchette text input in external scanner mode
    LaunchedEffect(session.isExternalScannerMode) {
        if (session.isExternalScannerMode) {
            delay(300)
            douchetteFocusRequester.requestFocus()
        }
    }

    // Auto-dismiss scan feedback banner after 4.5 seconds
    LaunchedEffect(scanFeedback) {
        if (scanFeedback != null) {
            delay(4500)
            onDismissFeedback()
        }
    }

    val presentMemberIds = remember(sessionAttendances) {
        sessionAttendances.map { it.memberId }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("coach_pointage_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header: Category & Start Time
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.categoryName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "Début : ${session.startTime} • Tatami Seddouk",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (session.isExternalScannerMode) Color(0xFF1565C0).copy(alpha = 0.15f) else JudoRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (session.isExternalScannerMode) "📡 Douchette prête" else "📷 Mode Caméra",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (session.isExternalScannerMode) Color(0xFF1E88E5) else JudoRed
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Essential Stats: Présents | Absents | Taux
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatPill(
                title = "Présents",
                value = "$presentCount",
                color = JudoSuccess,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                title = "Absents",
                value = "$absentCount",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                title = "Taux",
                value = "$attendanceRate%",
                color = JudoRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Real-Time Visual Feedback Banner (Vert / Orange / Rouge)
        AnimatedVisibility(
            visible = scanFeedback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            scanFeedback?.let { feedback ->
                val (bgColor, borderColor, icon, title, subtitle) = when (feedback) {
                    is CoachScanFeedback.Success -> {
                        if (feedback.isCotisationUpToDate) {
                            Quadruple(
                                JudoSuccess.copy(alpha = 0.15f),
                                JudoSuccess,
                                Icons.Default.CheckCircle,
                                "✓ ${feedback.member.fullName} — Présent",
                                "Cotisation à jour • Pointé à ${feedback.timeString}"
                            )
                        } else {
                            Quadruple(
                                Color(0xFFFF9800).copy(alpha = 0.18f),
                                Color(0xFFF57C00),
                                Icons.Default.Warning,
                                "⚠️ ${feedback.member.fullName} — Présent",
                                "Cotisation en retard • Pointé à ${feedback.timeString}"
                            )
                        }
                    }
                    is CoachScanFeedback.AlreadyChecked -> {
                        Quadruple(
                            Color(0xFF1976D2).copy(alpha = 0.15f),
                            Color(0xFF1976D2),
                            Icons.Default.Check,
                            "ℹ️ Déjà pointé : ${feedback.member.fullName}",
                            "Enregistré à ${feedback.timeString}"
                        )
                    }
                    is CoachScanFeedback.Invalid -> {
                        Quadruple(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.error,
                            Icons.Default.Error,
                            "❌ Non reconnu",
                            feedback.message
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coach_scan_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = borderColor
                                    )
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        IconButton(onClick = onDismissFeedback) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = borderColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Douchette Continuous Input (invisible / active receiver for barcode scanner keystrokes)
        if (session.isExternalScannerMode) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { douchetteFocusRequester.requestFocus() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(JudoSuccess)
                        )
                        Text(
                            text = "Scannez avec la douchette sans toucher l'écran",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1565C0)
                            )
                        )
                    }

                    // Active BasicTextField capturing barcode input
                    BasicTextField(
                        value = douchetteInput,
                        onValueChange = { douchetteInput = it },
                        modifier = Modifier
                            .width(60.dp)
                            .focusRequester(douchetteFocusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (douchetteInput.isNotBlank()) {
                                    onQrScanned(douchetteInput.trim())
                                    douchetteInput = ""
                                }
                            }
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Actions Row: "Scanner un judoka" & "Pointage manuel"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JudoRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp)
                    .testTag("coach_scan_judoka_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Scanner judoka",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = { isManualCheckInDialogOpen = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("coach_manual_checkin_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = JudoRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Manuel",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // Recent Scans / Check-ins
        Text(
            text = "Derniers pointages (${sessionAttendances.size})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sessionAttendances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "En attente des premiers judokas sur le tatami...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(sessionAttendances, key = { it.attendanceId }) { attendance ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
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
                                JudokaAvatar(
                                    name = attendance.fullName,
                                    belt = attendance.belt,
                                    photoUrl = attendance.photoUrl,
                                    sizeDp = 38
                                )
                                Column {
                                    Text(
                                        text = attendance.fullName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Pointé à ${attendance.timeString}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = JudoSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Présent ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = JudoSuccess,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Finish Session Button
        OutlinedButton(
            onClick = onFinishSession,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("coach_finish_session_btn")
        ) {
            Icon(
                imageVector = Icons.Default.StopCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terminer la séance",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    // Modal Dialog: Pointage Manuel (Judoka absent du scan ou carte oubliée)
    if (isManualCheckInDialogOpen) {
        var manualSearchQuery by remember { mutableStateOf("") }
        val absentMembers = remember(categoryMembers, presentMemberIds, manualSearchQuery) {
            categoryMembers
                .filter { it.id !in presentMemberIds }
                .filter {
                    manualSearchQuery.isBlank() || it.fullName.contains(manualSearchQuery, ignoreCase = true)
                }
        }

        AlertDialog(
            onDismissRequest = { isManualCheckInDialogOpen = false },
            title = {
                Text(
                    text = "Pointage manuel",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = manualSearchQuery,
                        onValueChange = { manualSearchQuery = it },
                        placeholder = { Text("Rechercher judoka...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Judokas non encore pointés (${absentMembers.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (absentMembers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tous les judokas sont déjà pointés !",
                                        style = MaterialTheme.typography.bodySmall.copy(color = JudoSuccess)
                                    )
                                }
                            }
                        } else {
                            items(absentMembers, key = { it.id }) { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onManualCheckInMember(member)
                                            isManualCheckInDialogOpen = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        JudokaAvatar(name = member.fullName, belt = member.belt, sizeDp = 30)
                                        Column {
                                            Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Ceinture ${member.beltName.lowercase()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text("Pointer +", fontWeight = FontWeight.Bold, color = JudoRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isManualCheckInDialogOpen = false }) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/**
 * Screen 4 — Fin de séance (Récapitulatif officiel)
 * Displays:
 * - Total presents and total absents
 * - Start time and end time
 * - Present coaches
 * - Fast list of presents and absents (with tabs)
 * - "Valider et enregistrer" button
 */
@Composable
fun CoachSessionSummaryScreen(
    session: ActiveTrainingSession,
    categoryMembers: List<Member>,
    sessionAttendances: List<AttendanceWithMember>,
    onValidateAndSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categoryMembers.size
    val presentCount = sessionAttendances.size
    val absentCount = (totalCount - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalCount > 0) ((presentCount.toDouble() / totalCount.toDouble()) * 100).toInt() else 0

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var endTimeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.FRENCH)
        endTimeString = sdf.format(Date())
    }

    val presentMemberIds = remember(sessionAttendances) {
        sessionAttendances.map { it.memberId }.toSet()
    }

    val presentMembers = remember(sessionAttendances) {
        sessionAttendances.map { it.fullName }
    }

    val absentMembers = remember(categoryMembers, presentMemberIds) {
        categoryMembers.filter { it.id !in presentMemberIds }.map { it.fullName }
    }

    val presentCoaches = remember(session) {
        val list = mutableListOf<String>()
        if (session.coach1Present) list.add(session.coach1Name)
        if (session.coach2Present) list.add(session.coach2Name)
        if (list.isEmpty()) list.add("Aucun coach consigné")
        list.joinToString(" • ")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("coach_session_summary_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(JudoSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JudoSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Séance terminée",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )

                Text(
                    text = "Récapitulatif officiel • ${session.categoryName}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Stats Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Coachs présents",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = presentCoaches,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Horaire",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "${session.startTime} - $endTimeString",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )

                        // 3 Stats Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill(
                                title = "Présents",
                                value = "$presentCount",
                                color = JudoSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                title = "Absents",
                                value = "$absentCount",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                title = "Taux",
                                value = "$attendanceRate%",
                                color = JudoRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs for Presents and Absents list
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = JudoRed,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Présents ($presentCount)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Absents ($absentCount)", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable List of presents / absents
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentList = if (selectedTabIndex == 0) presentMembers else absentMembers
                    if (currentList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTabIndex == 0) "Aucun judoka présent" else "Aucun judoka absent !",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    } else {
                        items(currentList) { name ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (selectedTabIndex == 0) "Présent ✓" else "Absent",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedTabIndex == 0) JudoSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom validate and save button
            Button(
                onClick = { onValidateAndSave(endTimeString) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JudoRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("coach_validate_summary_btn")
            ) {
                Text(
                    text = "Valider et enregistrer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
private fun StatPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

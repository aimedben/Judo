package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CoachProfile
import com.example.data.model.Member
import com.example.data.repository.CheckInResult
import com.example.ui.MainViewModel
import com.example.ui.ThemePreference
import com.example.ui.components.AddEditMemberDialog
import com.example.ui.components.AlreadyCheckedInDialog
import com.example.ui.components.AssociateCardDialog
import com.example.ui.components.CheckInConfirmationDialog
import com.example.ui.components.CoachProfileDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ScannerTargetMode
import com.example.ui.screens.StatFilterMode
import com.example.ui.screens.StatFilteredMembersScreen
import com.example.ui.screens.StatPeriod
import com.example.ui.theme.JudoRed
import com.example.ui.theme.MyApplicationTheme

enum class ScreenTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Home),
    SCANNER("Scanner", Icons.Default.QrCodeScanner),
    MEMBERS("Membres", Icons.Default.People),
    DASHBOARD("Stats", Icons.Default.BarChart),
    HISTORY("Historique", Icons.Default.History)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by viewModel.themePreference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePref) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                JudoApp(viewModel = viewModel, isDarkTheme = isDark)
            }
        }
    }
}

@Composable
fun JudoApp(viewModel: MainViewModel, isDarkTheme: Boolean) {
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var activeStatFilter by remember { mutableStateOf<StatFilterMode?>(null) }
    var activeStatPeriod by remember { mutableStateOf(StatPeriod.TODAY) }
    var dashboardPeriod by remember { mutableStateOf(StatPeriod.TODAY) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var scannerMode by remember { mutableStateOf(ScannerTargetMode.POINTAGE) }
    var isCoachProfileDialogOpen by remember { mutableStateOf(false) }

    val coachProfile by viewModel.coachProfile.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val recentAttendance by viewModel.recentAttendance.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val lastScanned by viewModel.lastScanned.collectAsState()
    val activeCheckInResult by viewModel.activeCheckInResult.collectAsState()
    val unassignedQrCode by viewModel.unassignedQrCode.collectAsState()
    val scannedMemberForCreation by viewModel.scannedMemberForCreation.collectAsState()

    val topStats = remember(allMembers, allAttendance, dashboardPeriod) {
        viewModel.getTopAttendanceList(allMembers, allAttendance, dashboardPeriod)
    }

    // Back handling priority: Member Detail -> Stat Filter list -> Home screen
    if (selectedMemberId != null) {
        BackHandler {
            selectedMemberId = null
        }
    } else if (activeStatFilter != null) {
        BackHandler {
            activeStatFilter = null
        }
    } else if (currentTab != ScreenTab.HOME) {
        BackHandler {
            currentTab = ScreenTab.HOME
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Tablet layout: NavigationRail on left
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Text(
                            text = "🥋 JUDO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = JudoRed
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                ) {
                    ScreenTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab && selectedMemberId == null && activeStatFilter == null,
                            onClick = {
                                selectedMemberId = null
                                activeStatFilter = null
                                currentTab = tab
                                if (tab == ScreenTab.SCANNER) {
                                    scannerMode = ScannerTargetMode.POINTAGE
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = JudoRed,
                                indicatorColor = JudoRed
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Theme toggle on tablet rail
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Changer thème",
                            tint = if (isDarkTheme) Color(0xFFFFD54F) else JudoRed
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AppScreenContent(
                        currentTab = currentTab,
                        selectedMemberId = selectedMemberId,
                        activeStatFilter = activeStatFilter,
                        activeStatPeriod = activeStatPeriod,
                        dashboardPeriod = dashboardPeriod,
                        onDashboardPeriodChange = { dashboardPeriod = it },
                        onActiveStatPeriodChange = { activeStatPeriod = it },
                        onFilterModeChange = { activeStatFilter = it },
                        allMembers = allMembers,
                        todayAttendance = todayAttendance,
                        recentAttendance = recentAttendance,
                        allAttendance = allAttendance,
                        lastScanned = lastScanned,
                        topStats = topStats,
                        isDarkTheme = isDarkTheme,
                        scannerMode = scannerMode,
                        coachProfile = coachProfile,
                        onEditCoachProfile = { isCoachProfileDialogOpen = true },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onNavigateToTab = { tab ->
                            activeStatFilter = null
                            selectedMemberId = null
                            currentTab = tab
                            if (tab == ScreenTab.SCANNER) {
                                scannerMode = ScannerTargetMode.POINTAGE
                            }
                        },
                        onSelectMember = { selectedMemberId = it },
                        onBackFromMemberDetail = { selectedMemberId = null },
                        onStatClick = { filterMode, period ->
                            selectedMemberId = null
                            activeStatFilter = filterMode
                            activeStatPeriod = period
                        },
                        onBackFromStatFilter = { activeStatFilter = null },
                        onEditMember = { memberToEdit = it },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onSaveMember = { viewModel.saveMember(it) },
                        onDeleteAttendance = { viewModel.deleteAttendance(it) },
                        onQrScanned = { viewModel.onQrScanned(it) },
                        onCardExtractedForCreation = { viewModel.extractCardDataForMemberCreation(it) },
                        onScanCardToExtract = {
                            activeStatFilter = null
                            selectedMemberId = null
                            scannerMode = ScannerTargetMode.EXTRACTION_MEMBRE
                            currentTab = ScreenTab.SCANNER
                        },
                        onManualCheckIn = { viewModel.manualCheckIn(it) },
                        onRecordAttendanceForDate = { id, date -> viewModel.recordAttendanceForDate(id, date) },
                        onDeleteAttendanceForDate = { id, date -> viewModel.deleteAttendanceForDate(id, date) }
                    )
                }
            }
        } else {
            // Phone layout: Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        ScreenTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab && selectedMemberId == null && activeStatFilter == null,
                                onClick = {
                                    selectedMemberId = null
                                    activeStatFilter = null
                                    currentTab = tab
                                    if (tab == ScreenTab.SCANNER) {
                                        scannerMode = ScannerTargetMode.POINTAGE
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (currentTab == tab && activeStatFilter == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = JudoRed,
                                    indicatorColor = JudoRed,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppScreenContent(
                        currentTab = currentTab,
                        selectedMemberId = selectedMemberId,
                        activeStatFilter = activeStatFilter,
                        activeStatPeriod = activeStatPeriod,
                        dashboardPeriod = dashboardPeriod,
                        onDashboardPeriodChange = { dashboardPeriod = it },
                        onActiveStatPeriodChange = { activeStatPeriod = it },
                        onFilterModeChange = { activeStatFilter = it },
                        allMembers = allMembers,
                        todayAttendance = todayAttendance,
                        recentAttendance = recentAttendance,
                        allAttendance = allAttendance,
                        lastScanned = lastScanned,
                        topStats = topStats,
                        isDarkTheme = isDarkTheme,
                        scannerMode = scannerMode,
                        coachProfile = coachProfile,
                        onEditCoachProfile = { isCoachProfileDialogOpen = true },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onNavigateToTab = { tab ->
                            activeStatFilter = null
                            selectedMemberId = null
                            currentTab = tab
                            if (tab == ScreenTab.SCANNER) {
                                scannerMode = ScannerTargetMode.POINTAGE
                            }
                        },
                        onSelectMember = { selectedMemberId = it },
                        onBackFromMemberDetail = { selectedMemberId = null },
                        onStatClick = { filterMode, period ->
                            selectedMemberId = null
                            activeStatFilter = filterMode
                            activeStatPeriod = period
                        },
                        onBackFromStatFilter = { activeStatFilter = null },
                        onEditMember = { memberToEdit = it },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onSaveMember = { viewModel.saveMember(it) },
                        onDeleteAttendance = { viewModel.deleteAttendance(it) },
                        onQrScanned = { viewModel.onQrScanned(it) },
                        onCardExtractedForCreation = { viewModel.extractCardDataForMemberCreation(it) },
                        onScanCardToExtract = {
                            activeStatFilter = null
                            selectedMemberId = null
                            scannerMode = ScannerTargetMode.EXTRACTION_MEMBRE
                            currentTab = ScreenTab.SCANNER
                        },
                        onManualCheckIn = { viewModel.manualCheckIn(it) },
                        onRecordAttendanceForDate = { id, date -> viewModel.recordAttendanceForDate(id, date) },
                        onDeleteAttendanceForDate = { id, date -> viewModel.deleteAttendanceForDate(id, date) }
                    )
                }
            }
        }
    }

    // Modal dialogs triggered by scan or actions
    when (val result = activeCheckInResult) {
        is CheckInResult.Success -> {
            CheckInConfirmationDialog(
                member = result.member,
                timeString = result.attendance.timeString,
                onDismiss = { viewModel.dismissCheckInDialog() },
                onViewProfile = { memberId ->
                    viewModel.dismissCheckInDialog()
                    selectedMemberId = memberId
                }
            )
        }
        is CheckInResult.AlreadyCheckedIn -> {
            AlreadyCheckedInDialog(
                member = result.member,
                existingTime = result.existingAttendance.timeString,
                onDismiss = { viewModel.dismissCheckInDialog() }
            )
        }
        else -> Unit
    }

    // Associate unassigned QR Card Dialog
    if (unassignedQrCode != null && scannedMemberForCreation == null) {
        AssociateCardDialog(
            qrCode = unassignedQrCode!!,
            members = allMembers,
            onAssociate = { memberId, code ->
                viewModel.associateQrToMember(memberId, code)
            },
            onDismiss = { viewModel.cancelUnassignedQr() }
        )
    }

    // Scanned Card Auto-Extraction Dialog for Member Registration!
    if (scannedMemberForCreation != null) {
        AddEditMemberDialog(
            initialMember = null,
            initialExtractedInfo = scannedMemberForCreation,
            coachProfile = coachProfile,
            onEditCoachProfile = { isCoachProfileDialogOpen = true },
            onRequestScanCard = {
                activeStatFilter = null
                selectedMemberId = null
                scannerMode = ScannerTargetMode.EXTRACTION_MEMBRE
                currentTab = ScreenTab.SCANNER
            },
            onSave = { newMember ->
                viewModel.saveMember(newMember)
                viewModel.clearScannedMemberForCreation()
            },
            onDismiss = { viewModel.clearScannedMemberForCreation() }
        )
    }

    // Edit Member Dialog
    if (memberToEdit != null) {
        AddEditMemberDialog(
            initialMember = memberToEdit,
            coachProfile = coachProfile,
            onEditCoachProfile = { isCoachProfileDialogOpen = true },
            onSave = { updated ->
                viewModel.saveMember(updated)
                memberToEdit = null
            },
            onDismiss = { memberToEdit = null }
        )
    }

    // Coach / Sensei Profile Dialog
    if (isCoachProfileDialogOpen) {
        CoachProfileDialog(
            coachProfile = coachProfile,
            onSave = { updated ->
                viewModel.updateCoachProfile(updated)
                isCoachProfileDialogOpen = false
            },
            onDismiss = { isCoachProfileDialogOpen = false }
        )
    }
}

@Composable
private fun AppScreenContent(
    currentTab: ScreenTab,
    selectedMemberId: Long?,
    activeStatFilter: StatFilterMode?,
    activeStatPeriod: StatPeriod,
    dashboardPeriod: StatPeriod,
    onDashboardPeriodChange: (StatPeriod) -> Unit,
    onActiveStatPeriodChange: (StatPeriod) -> Unit,
    onFilterModeChange: (StatFilterMode) -> Unit,
    allMembers: List<Member>,
    todayAttendance: List<com.example.data.model.AttendanceWithMember>,
    recentAttendance: List<com.example.data.model.AttendanceWithMember>,
    allAttendance: List<com.example.data.model.AttendanceWithMember>,
    lastScanned: com.example.data.model.AttendanceWithMember?,
    topStats: List<com.example.data.repository.MemberAttendanceStat>,
    isDarkTheme: Boolean,
    scannerMode: ScannerTargetMode,
    coachProfile: CoachProfile,
    onEditCoachProfile: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit,
    onSelectMember: (Long) -> Unit,
    onBackFromMemberDetail: () -> Unit,
    onStatClick: (StatFilterMode, StatPeriod) -> Unit,
    onBackFromStatFilter: () -> Unit,
    onEditMember: (Member) -> Unit,
    onDeleteMember: (Member) -> Unit,
    onSaveMember: (Member) -> Unit,
    onDeleteAttendance: (Long) -> Unit,
    onQrScanned: (String) -> Unit,
    onCardExtractedForCreation: (String) -> Unit,
    onScanCardToExtract: () -> Unit,
    onManualCheckIn: (Long) -> Unit,
    onRecordAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onDeleteAttendanceForDate: (Long, String) -> Unit = { _, _ -> }
) {
    // 1. Member Profile Detail Screen
    if (selectedMemberId != null) {
        val member = allMembers.find { it.id == selectedMemberId }
        if (member != null) {
            val memberAttendances = allAttendance
                .filter { it.memberId == selectedMemberId }
                .map {
                    com.example.data.model.Attendance(
                        id = it.attendanceId,
                        memberId = it.memberId,
                        dateString = it.dateString,
                        timeString = it.timeString,
                        timestamp = it.timestamp
                    )
                }

            MemberDetailScreen(
                member = member,
                attendanceList = memberAttendances,
                allClubAttendances = allAttendance,
                onBack = onBackFromMemberDetail,
                onManualCheckIn = onManualCheckIn,
                onRecordAttendanceForDate = onRecordAttendanceForDate,
                onDeleteAttendanceForDate = onDeleteAttendanceForDate,
                onEditMember = onEditMember,
                onDeleteMember = onDeleteMember
            )
            return
        }
    }

    // 2. Stat Filtered Members Screen (Absents, Présents, Inscrits, Taux)
    if (activeStatFilter != null) {
        StatFilteredMembersScreen(
            filterMode = activeStatFilter,
            statPeriod = activeStatPeriod,
            onPeriodChange = onActiveStatPeriodChange,
            onFilterModeChange = onFilterModeChange,
            allMembers = allMembers,
            todayAttendance = todayAttendance,
            allAttendance = allAttendance,
            onSelectMember = onSelectMember,
            onManualCheckIn = onManualCheckIn,
            onBack = onBackFromStatFilter
        )
        return
    }

    // 3. Tab Screens
    when (currentTab) {
        ScreenTab.HOME -> {
            HomeScreen(
                allMembers = allMembers,
                todayAttendance = todayAttendance,
                lastScanned = lastScanned,
                coachProfile = coachProfile,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onEditCoachProfile = onEditCoachProfile,
                onNavigateToScanner = { onNavigateToTab(ScreenTab.SCANNER) },
                onNavigateToMemberDetail = onSelectMember,
                onQuickCheckIn = onQrScanned,
                onStatClick = { filterMode -> onStatClick(filterMode, StatPeriod.TODAY) }
            )
        }
        ScreenTab.SCANNER -> {
            ScannerScreen(
                members = allMembers,
                initialMode = scannerMode,
                onQrScanned = onQrScanned,
                onCardExtractedForCreation = onCardExtractedForCreation,
                onBack = { onNavigateToTab(ScreenTab.HOME) }
            )
        }
        ScreenTab.MEMBERS -> {
            MembersScreen(
                members = allMembers,
                todayAttendance = todayAttendance,
                coachProfile = coachProfile,
                onEditCoachProfile = onEditCoachProfile,
                onSelectMember = onSelectMember,
                onSaveMember = onSaveMember,
                onNavigateToScanner = { onNavigateToTab(ScreenTab.SCANNER) },
                onScanCardToExtract = onScanCardToExtract
            )
        }
        ScreenTab.DASHBOARD -> {
            DashboardScreen(
                members = allMembers,
                todayAttendance = todayAttendance,
                allAttendance = allAttendance,
                selectedPeriod = dashboardPeriod,
                onPeriodChange = onDashboardPeriodChange,
                onSelectMember = onSelectMember,
                onStatClick = onStatClick
            )
        }
        ScreenTab.HISTORY -> {
            HistoryScreen(
                attendanceList = allAttendance,
                onDeleteAttendance = onDeleteAttendance,
                onNavigateToMemberDetail = onSelectMember
            )
        }
    }
}

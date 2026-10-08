package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceWithMember
import com.example.data.model.CoachProfile
import com.example.data.model.JudoGroup
import com.example.data.model.Member
import com.example.ui.components.AddEditMemberDialog
import com.example.ui.components.BeltBadge
import com.example.ui.components.GroupBadge
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

@Composable
fun MembersScreen(
    members: List<Member>,
    todayAttendance: List<AttendanceWithMember>,
    coachProfile: CoachProfile = CoachProfile(),
    onEditCoachProfile: (() -> Unit)? = null,
    onSelectMember: (Long) -> Unit,
    onSaveMember: (Member) -> Unit,
    onNavigateToScanner: () -> Unit,
    onScanCardToExtract: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("ALL") }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    val filteredMembers = remember(members, searchQuery, selectedGroupFilter) {
        members.filter { member ->
            val matchesSearch = searchQuery.isBlank() ||
                member.fullName.contains(searchQuery, ignoreCase = true) ||
                member.qrCode.contains(searchQuery, ignoreCase = true) ||
                member.beltName.contains(searchQuery, ignoreCase = true)

            val matchesGroup = selectedGroupFilter == "ALL" || member.groupCode == selectedGroupFilter

            matchesSearch && matchesGroup
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("members_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Membres du Dojo",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${filteredMembers.size} judokas inscrits",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onScanCardToExtract,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("scan_card_extract_button")
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Scan Carte")
                }

                Button(
                    onClick = { showAddMemberDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_member_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Ajouter")
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher nom, ceinture, carte QR...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("members_search_input")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedGroupFilter == "ALL",
                    onClick = { selectedGroupFilter = "ALL" },
                    label = { Text("Tous") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(JudoGroup.entries) { group ->
                FilterChip(
                    selected = selectedGroupFilter == group.code,
                    onClick = { selectedGroupFilter = group.code },
                    label = { Text(group.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Secondary Action: Associer carte QR
        OutlinedButton(
            onClick = onNavigateToScanner,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Associer une carte QR physique à un judoka")
        }

        // Members List
        if (filteredMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun judoka correspondant trouvé.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredMembers) { member ->
                    val isCheckedInToday = todayAttendance.any { it.memberId == member.id }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMember(member.id) }
                            .testTag("member_card_${member.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            JudokaAvatar(
                                name = member.fullName,
                                belt = member.belt,
                                photoUrl = member.photoUrl,
                                sizeDp = 48
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = member.fullName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (isCheckedInToday) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = JudoSuccess.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = JudoSuccess,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Présent",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = JudoSuccess,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GroupBadge(group = member.group)
                                    BeltBadge(belt = member.belt)
                                    Text(
                                        text = member.qrCode,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Détail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMemberDialog) {
        AddEditMemberDialog(
            initialMember = null,
            coachProfile = coachProfile,
            onEditCoachProfile = onEditCoachProfile,
            onRequestScanCard = {
                showAddMemberDialog = false
                onScanCardToExtract()
            },
            onSave = { newMember ->
                onSaveMember(newMember)
                showAddMemberDialog = false
            },
            onDismiss = { showAddMemberDialog = false }
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminNotification
import com.example.data.model.AdminProfile
import com.example.data.model.ClubSettings
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed

@Composable
fun AdminHeaderBar(
    clubSettings: ClubSettings,
    adminProfile: AdminProfile,
    notifications: List<AdminNotification>,
    isDarkTheme: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfileDialog: () -> Unit,
    onSwitchToCoach: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_header_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Club Logo & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(JudoRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "柔",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = clubSettings.clubName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "Espace Administrateur",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = JudoGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // Right: Actions (Notifications, Settings, Admin Profile Menu)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Switch to Coach Mode Quick Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = JudoRed.copy(alpha = 0.12f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .clickable { onSwitchToCoach() }
                        .testTag("admin_switch_coach_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsMartialArts,
                            contentDescription = "Espace Tatami",
                            tint = JudoRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tatami",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Notifications Icon with Badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("admin_notifications_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = JudoRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Settings Icon
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("admin_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Paramètres",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Admin Profile Icon & Menu
                Box {
                    IconButton(
                        onClick = { profileMenuExpanded = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("admin_profile_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(JudoGold.copy(alpha = 0.2f))
                                .border(1.5.dp, JudoGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Profil Administrateur",
                                tint = JudoGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = adminProfile.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = adminProfile.role,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenProfileDialog()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Modifier mes informations") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenProfileDialog()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Paramètres du club") },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenSettings()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Espace Coach (Tatami)") },
                            leadingIcon = {
                                Icon(Icons.Default.SportsMartialArts, contentDescription = null, tint = JudoRed, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onSwitchToCoach()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Déconnexion",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}

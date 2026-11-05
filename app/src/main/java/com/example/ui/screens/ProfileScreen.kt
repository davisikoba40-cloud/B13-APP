package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BluetoothMeshService
import com.example.model.AppThemeMode
import com.example.ui.components.TacticalAvatar
import com.example.ui.components.TacticalHeader
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.MeshChatViewModel

@Composable
fun ProfileScreen(
    viewModel: MeshChatViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val isBtEnabled by viewModel.meshService.isBluetoothEnabled.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()

    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bioStatus by remember(profile.bioStatus) { mutableStateOf(profile.bioStatus) }
    var isRelayEnabled by remember(profile.isRelayEnabled) { mutableStateOf(profile.isRelayEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("profile_screen_root")
    ) {
        TacticalHeader(
            title = "NODE PROFILE & SETTINGS",
            subtitle = "ID: ${profile.nodeId} • POWERED BY MOBILABS"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tactical Node Identity Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TacticalAvatar(name = displayName, isOnline = isBtEnabled, size = 56)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = displayName.uppercase(),
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = profile.nodeId,
                            color = GrayLight,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isBtEnabled) OnlineGreen else GrayMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBtEnabled) "RADIO TRANSCEIVER ACTIVE" else "RADIO OFF",
                                color = if (isBtEnabled) OnlineGreen else GrayMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Visual Theme Switcher
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COLOR THEME & VISUAL DESIGN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeOptionCard(
                        name = "BLACK & WHITE",
                        subtitle = "Tactical Monochrome High-Contrast",
                        primaryColor = PureWhite,
                        secondaryColor = PureBlack,
                        isSelected = currentTheme == AppThemeMode.BLACK_WHITE,
                        onClick = { viewModel.setThemeMode(AppThemeMode.BLACK_WHITE) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeOptionCard(
                        name = "PINK & WHITE",
                        subtitle = "Cyber Rose & Sakura Pearl",
                        primaryColor = PinkAccent,
                        secondaryColor = PureWhite,
                        isSelected = currentTheme == AppThemeMode.PINK_WHITE,
                        onClick = { viewModel.setThemeMode(AppThemeMode.PINK_WHITE) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeOptionCard(
                        name = "BLUE & WHITE",
                        subtitle = "Cobalt Cyber & Frost White",
                        primaryColor = BlueAccent,
                        secondaryColor = PureWhite,
                        isSelected = currentTheme == AppThemeMode.BLUE_WHITE,
                        onClick = { viewModel.setThemeMode(AppThemeMode.BLUE_WHITE) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeOptionCard(
                        name = "DARK STEALTH",
                        subtitle = "OLED Pitch Black Minimalist",
                        primaryColor = Color(0xFFD4D4D8),
                        secondaryColor = PureBlack,
                        isSelected = currentTheme == AppThemeMode.DARK_STEALTH,
                        onClick = { viewModel.setThemeMode(AppThemeMode.DARK_STEALTH) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Fields
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALL SIGN / DISPLAY NAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GrayLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input"),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = PureBlack,
                            unfocusedContainerColor = PureBlack
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TACTICAL STATUS / BIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GrayLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bioStatus,
                        onValueChange = { bioStatus = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bio_status_input"),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = PureBlack,
                            unfocusedContainerColor = PureBlack
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mesh Relay Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATIC MESH RELAY",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Forward encrypted packets from other nearby peers to extend mesh range.",
                                color = GrayLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = isRelayEnabled,
                            onCheckedChange = { isRelayEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = PureWhite,
                                uncheckedThumbColor = GrayLight,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(displayName, bioStatus, isRelayEnabled)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVE PROFILE CHANGES",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hardware & Protocol Info Card
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MESH TELEMETRY & HARDWARE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GrayLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TelemetryRow("PROTOCOL", "RFCOMM P2P (SPP)")
                    TelemetryRow("SERVICE UUID", BluetoothMeshService.B13_UUID.toString().take(18) + "...")
                    TelemetryRow("NETWORK LAYER", "FLOOD-RELAY (TTL=3)")
                    TelemetryRow("PACKET TYPES", "TEXT, AUDIO (M4A), VIDEO (MP4), FILE")
                    TelemetryRow("ENCRYPTION", "BLUETOOTH LINK-LEVEL AES-128")

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                }
                                context.startActivity(discoverableIntent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MAKE DISCOVERABLE (300 SEC)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    name: String,
    subtitle: String,
    primaryColor: Color,
    secondaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) DarkCard else PureBlack,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PureWhite else DarkBorder
        ),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color Swatch Pill
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(primaryColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(secondaryColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = subtitle,
                        color = GrayLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active Theme",
                    tint = PureWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = GrayMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = PureWhite,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

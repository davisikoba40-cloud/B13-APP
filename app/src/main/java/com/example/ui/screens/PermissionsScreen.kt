package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningYellow

@Composable
fun PermissionsScreen(
    isBluetoothEnabled: Boolean,
    onAllGrantedOrContinue: () -> Unit
) {
    val context = LocalContext.current
    var permissionsRequestedOnce by remember { mutableStateOf(false) }

    // Required permissions based on Android API level
    val permissionsToRequest = remember {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsRequestedOnce = true
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Result handled via system broadcast
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .testTag("permissions_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Tactical Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isBluetoothEnabled) OnlineGreen else WarningYellow)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RADIO & SENSOR INITIALIZATION",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "B13 BYMOBILABS requires hardware radios and sensors for decentralized peer-to-peer audio transmission, file distribution, and active mesh scanning.",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GrayLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subsystem 1: Bluetooth Radio Requirement
            PermissionCard(
                title = "BLUETOOTH TRANSCEIVER",
                description = "Mandatory for discovery, RFCOMM mesh networking, and direct device-to-device transport.",
                icon = Icons.Default.Bluetooth,
                statusText = if (isBluetoothEnabled) "ADAPTER ACTIVE" else "ACTIVATION REQUIRED",
                isActive = isBluetoothEnabled,
                actionButton = if (!isBluetoothEnabled) {
                    {
                        Button(
                            onClick = {
                                try {
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    enableBtLauncher.launch(enableBtIntent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("enable_bluetooth_button")
                        ) {
                            Text("ACTIVATE BLUETOOTH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem 2: Microphone / Audio Recording
            PermissionCard(
                title = "AUDIO & MICROPHONE",
                description = "Used to record high-fidelity voice notes transmitted through peer mesh packets.",
                icon = Icons.Default.Mic,
                statusText = "VOICE TRANSMISSION",
                isActive = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem 3: Storage & File Sharing
            PermissionCard(
                title = "STORAGE & FILE SHARING",
                description = "Grants access to document and photo sharing across the decentralized mesh.",
                icon = Icons.Default.Folder,
                statusText = "PAYLOAD TRANSFER",
                isActive = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem 4: Push Notifications Outside App
            PermissionCard(
                title = "PUSH NOTIFICATIONS",
                description = "Receives instant heads-up alerts outside the app so you never miss incoming peer or squad transmissions.",
                icon = Icons.Default.Warning,
                statusText = "OUTSIDE APP ALERTS",
                isActive = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Area
            Button(
                onClick = {
                    permissionLauncher.launch(permissionsToRequest)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("request_permissions_button")
            ) {
                Text(
                    text = "GRANT HARDWARE PERMISSIONS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAllGrantedOrContinue,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PureWhite
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("continue_to_mesh_button")
            ) {
                Text(
                    text = "PROCEED TO B13 CONSOLE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    statusText: String,
    isActive: Boolean,
    actionButton: (@Composable () -> Unit)? = null
) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PureBlack)
                            .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Text(
                            text = statusText,
                            color = if (isActive) OnlineGreen else WarningYellow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isActive) OnlineGreen else WarningYellow,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = GrayLight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(10.dp))
                actionButton()
            }
        }
    }
}

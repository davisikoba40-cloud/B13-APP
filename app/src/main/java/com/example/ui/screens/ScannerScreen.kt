package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MeshPeer
import com.example.ui.components.SignalStrengthBar
import com.example.ui.components.TacticalAvatar
import com.example.ui.components.TacticalHeader
import com.example.ui.components.formatTime
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.MeshChatViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScannerScreen(
    viewModel: MeshChatViewModel,
    onStartDirectChat: (MeshPeer) -> Unit
) {
    val isScanning by viewModel.meshService.isScanning.collectAsState()
    val isBtEnabled by viewModel.meshService.isBluetoothEnabled.collectAsState()
    val allPeers by viewModel.allPeers.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("scanner_screen_root")
    ) {
        TacticalHeader(
            title = "RF RADAR & NODE SCANNER",
            subtitle = if (isScanning) "ACTIVE SPECTRUM SWEEP IN PROGRESS" else "SPECTRUM MONITORING STANDBY",
            actions = {
                IconButton(
                    onClick = {
                        if (isScanning) viewModel.meshService.stopScan()
                        else viewModel.meshService.startScan()
                    },
                    modifier = Modifier.testTag("toggle_scan_button")
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Sensors else Icons.Default.Refresh,
                        contentDescription = "Toggle Scan",
                        tint = if (isScanning) OnlineGreen else PureWhite
                    )
                }
            }
        )

        // Radar Visualizer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(PureBlack)
                .border(1.dp, DarkBorder),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Concentric circles
                drawCircle(color = DarkBorder, radius = radius, style = Stroke(width = 1f))
                drawCircle(color = DarkBorder, radius = radius * 0.66f, style = Stroke(width = 1f))
                drawCircle(color = DarkBorder, radius = radius * 0.33f, style = Stroke(width = 1f))

                // Crosshairs
                drawLine(
                    color = DarkBorder,
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = DarkBorder,
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1f
                )

                // Sweep Beam Line
                if (isScanning) {
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + (radius * cos(rad)).toFloat()
                    val endY = center.y + (radius * sin(rad)).toFloat()
                    drawLine(
                        color = PureWhite,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // Center Node
                drawCircle(color = PureWhite, radius = 4f, center = center)
            }

            // Radar Status Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (isScanning) "STATUS: SCANNING RFCOMM" else "STATUS: IDLE",
                    color = if (isScanning) OnlineGreen else GrayLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TARGETS DETECTED: ${allPeers.size}",
                    color = PureWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Active Status
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "HARDWARE BT ACTIVE",
                    color = OnlineGreen,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // Active Devices List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVERED NODES (${allPeers.size})",
                        color = GrayLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RFCOMM 2.4GHz",
                        color = GrayMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (allPeers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO BLUETOOTH PEERS IN RANGE\nTap top scan icon to discover active devices nearby.",
                            color = GrayLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(allPeers, key = { it.macAddress }) { peer ->
                PeerCard(
                    peer = peer,
                    onConnect = { viewModel.connectPeer(peer) },
                    onToggleSave = { viewModel.toggleSavePeer(peer) },
                    onChat = { onStartDirectChat(peer) }
                )
            }
        }
    }
}

@Composable
fun PeerCard(
    peer: MeshPeer,
    onConnect: () -> Unit,
    onToggleSave: () -> Unit,
    onChat: () -> Unit
) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (peer.isConnected) PureWhite else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TacticalAvatar(name = peer.name, isOnline = peer.isConnected, size = 38)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = peer.name,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "MAC: ${peer.macAddress}",
                            color = GrayMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalStrengthBar(rssi = peer.rssi)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${peer.rssi}dB",
                        color = GrayLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LAST SEEN: ${formatTime(peer.lastSeenTimestamp)}",
                    color = GrayLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = peer.status,
                    color = if (peer.isConnected) OnlineGreen else GrayMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save / Bookmark Icon
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (peer.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Contact",
                        tint = if (peer.isSaved) PureWhite else GrayMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (!peer.isConnected) {
                    OutlinedButton(
                        onClick = onConnect,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "LINK",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Button(
                    onClick = onChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CHAT",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

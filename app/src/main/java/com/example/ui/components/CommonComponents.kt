package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TacticalHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Surface(
        color = PureBlack,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.uppercase(),
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = GrayLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }
            }
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun SignalStrengthBar(rssi: Int, modifier: Modifier = Modifier) {
    // RSSI ranges from -100 (poor) to -40 (strong)
    val bars = when {
        rssi >= -55 -> 4
        rssi >= -70 -> 3
        rssi >= -85 -> 2
        else -> 1
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            val h = (i * 3 + 4).dp
            val active = i <= bars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .background(if (active) PureWhite else DarkBorder)
            )
        }
    }
}

@Composable
fun TacticalAvatar(
    name: String,
    isOnline: Boolean = true,
    size: Int = 42
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkCard)
            .border(1.dp, if (isOnline) PureWhite else DarkBorder, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .ifEmpty { "B13" }

        Text(
            text = initials.uppercase(),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = (size / 3).sp
        )

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun VoiceNotePlayer(
    audioPath: String,
    durationSec: Int,
    isPlaying: Boolean,
    progress: Float,
    isFromMe: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isFromMe) PureWhite else DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFromMe) DarkBorder else PureWhite.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isFromMe) PureBlack else PureWhite)
                    .testTag("play_voice_note_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isFromMe) PureWhite else PureBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VOICE TRANSMISSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isFromMe) PureBlack else PureWhite
                    )
                    Text(
                        text = "${durationSec}s",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isFromMe) GrayMuted else GrayLight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isFromMe) PureBlack else PureWhite,
                    trackColor = if (isFromMe) Color(0xFFE5E5E5) else DarkBorder
                )
            }
        }
    }
}

@Composable
fun FileAttachmentView(
    fileName: String,
    fileSize: Long,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    val sizeString = when {
        fileSize >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize / (1024f * 1024f))
        fileSize >= 1024 -> "${fileSize / 1024} KB"
        else -> "$fileSize B"
    }
    Surface(
        color = if (isFromMe) PureWhite else DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFromMe) DarkBorder else PureWhite.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isFromMe) PureBlack else PureWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = "File attachment",
                    tint = if (isFromMe) PureWhite else PureBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFromMe) PureBlack else PureWhite
                )
                Text(
                    text = "$sizeString • BLUETOOTH TRANSFER",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFromMe) GrayMuted else GrayLight
                )
            }
        }
    }
}

@Composable
fun VideoMessageView(
    fileName: String,
    fileSize: Long,
    isFromMe: Boolean,
    onPlayVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeString = when {
        fileSize >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize / (1024f * 1024f))
        fileSize >= 1024 -> "${fileSize / 1024} KB"
        else -> "$fileSize B"
    }
    Surface(
        color = if (isFromMe) PureWhite else DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFromMe) DarkBorder else PureWhite.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayVideo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isFromMe) PureBlack else PureWhite)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    tint = if (isFromMe) PureWhite else PureBlack,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFromMe) PureBlack else PureWhite
                )
                Text(
                    text = "VIDEO • $sizeString • BLUETOOTH MESH",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFromMe) GrayMuted else GrayLight
                )
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

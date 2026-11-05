package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.ChatMessage
import com.example.model.MeshGroup
import com.example.model.MeshPeer
import com.example.model.MessageType
import com.example.ui.components.FileAttachmentView
import com.example.ui.components.TacticalAvatar
import com.example.ui.components.TacticalHeader
import com.example.ui.components.VideoMessageView
import com.example.ui.components.VoiceNotePlayer
import com.example.ui.components.formatTime
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.MeshChatViewModel
import java.io.File

@Composable
fun ChatScreen(
    viewModel: MeshChatViewModel,
    peer: MeshPeer? = null,
    group: MeshGroup? = null,
    onBack: (() -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val savedPeers by viewModel.savedPeers.collectAsState()
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val recordSeconds by viewModel.recordingSeconds.collectAsState()
    val playingPath by viewModel.audioPlayer.currentPlayingPath.collectAsState()
    val isPlayingAudio by viewModel.audioPlayer.isPlaying.collectAsState()
    val audioProgress by viewModel.audioPlayer.playProgress.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showCreateSquadDialog by remember { mutableStateOf(false) }
    var channelViewMode by remember { mutableStateOf(0) } // 0 = Global Mesh Chat, 1 = Squads / Groups Directory

    val listState = rememberLazyListState()

    // Determine target ID
    val targetId = when {
        group != null -> group.id
        peer != null -> peer.id
        else -> "BROADCAST"
    }

    // Filter messages for this target
    val filteredMessages = remember(messages, targetId) {
        when {
            group != null -> {
                messages.filter { it.targetId == group.id }
            }
            peer != null -> {
                messages.filter {
                    (it.senderId == peer.id && it.targetId != "BROADCAST") ||
                    (it.isFromMe && it.targetId == peer.id)
                }
            }
            else -> {
                messages.filter { it.targetId == "BROADCAST" }
            }
        }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty() && channelViewMode == 0) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.shareFileFromUri(uri, targetId)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.shareVideoFromUri(uri, targetId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("chat_screen_root")
    ) {
        // Tactical Header
        val title = when {
            group != null -> "[SQUAD] ${group.name}"
            peer != null -> peer.name
            else -> "B13 • BYMOBILABS"
        }
        val subtitle = when {
            group != null -> "${group.memberIds.size} NODES • ${group.description}"
            peer != null -> "DIRECT RFCOMM • ${peer.macAddress}"
            else -> "GLOBAL MESH BROADCAST • POWERED BY MOBILABS"
        }

        TacticalHeader(
            title = title,
            subtitle = subtitle,
            actions = {
                // Live status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE BT MESH",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                if (onBack != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                }
            }
        )

        // Sub-channel selector for Root Channels tab
        if (peer == null && group == null) {
            Surface(
                color = DarkCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        ChannelTabButton(
                            label = "#GLOBAL MESH",
                            icon = Icons.Default.Public,
                            selected = channelViewMode == 0,
                            onClick = { channelViewMode = 0 }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ChannelTabButton(
                            label = "SQUADS (${groups.size})",
                            icon = Icons.Default.Group,
                            selected = channelViewMode == 1,
                            onClick = { channelViewMode = 1 }
                        )
                    }

                    if (channelViewMode == 1) {
                        OutlinedButton(
                            onClick = { showCreateSquadDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("create_squad_header_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ SQUAD", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // View Mode 1: Squads & Groups Directory
        if (peer == null && group == null && channelViewMode == 1) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "TACTICAL MESH SQUADS & CHANNELS",
                        color = GrayLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (groups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO SQUADS CREATED\nTap '+ SQUAD' to initialize a channel",
                                color = GrayLight,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(groups, key = { it.id }) { grp ->
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openGroupChat(grp) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TacticalAvatar(name = grp.name, size = 36)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = grp.name,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "ID: ${grp.id} • ${grp.memberIds.size} NODES",
                                            color = GrayMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.openGroupChat(grp) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PureWhite,
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("OPEN", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = grp.description,
                                color = GrayLight,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = DarkCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "READY FOR TRANSMISSIONS",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Send live text, record voice audio notes, or share video and files over Bluetooth mesh.",
                                        color = GrayLight,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredMessages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        isPlaying = playingPath == msg.audioFilePath && isPlayingAudio,
                        playProgress = if (playingPath == msg.audioFilePath) audioProgress else 0f,
                        onTogglePlayAudio = {
                            msg.audioFilePath?.let { path ->
                                viewModel.audioPlayer.togglePlay(path)
                            }
                        }
                    )
                }
            }
        }

        // Voice Recording Banner
        AnimatedVisibility(visible = isRecording) {
            RecordingControlBar(
                seconds = recordSeconds,
                onCancel = { viewModel.cancelVoiceRecording() },
                onSend = { viewModel.stopVoiceRecording(targetId) }
            )
        }

        // Bottom Compose Input Bar
        if (!(peer == null && group == null && channelViewMode == 1)) {
            Surface(
                color = PureBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File Attachment Button
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .testTag("attach_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Share File",
                            tint = PureWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Video Attachment Button
                    IconButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .testTag("attach_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Share Video",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Message ${if (group != null) group.name else if (peer != null) peer.name else "MESH"}...",
                                color = GrayMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = DarkBorder,
                            cursorColor = PureWhite
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (inputText.isNotBlank()) {
                        // Send Text Button
                        IconButton(
                            onClick = {
                                viewModel.sendText(inputText, targetId)
                                inputText = ""
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureWhite)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = PureBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Mic Voice Note Button
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopVoiceRecording(targetId)
                                } else {
                                    viewModel.startVoiceRecording()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isRecording) Color.Red else DarkCard)
                                .border(1.dp, if (isRecording) Color.Red else DarkBorder, RoundedCornerShape(4.dp))
                                .testTag("voice_record_button")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                                contentDescription = "Record Audio",
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Squad Dialog
    if (showCreateSquadDialog) {
        CreateSquadDialog(
            savedPeers = savedPeers,
            onDismiss = { showCreateSquadDialog = false },
            onCreate = { squadName, squadDesc, selectedMembers ->
                viewModel.createGroup(squadName, squadDesc, selectedMembers)
                showCreateSquadDialog = false
            }
        )
    }
}

@Composable
private fun ChannelTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) PureWhite else PureBlack,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PureWhite else DarkBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PureBlack else GrayLight,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) PureBlack else GrayLight,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CreateSquadDialog(
    savedPeers: List<MeshPeer>,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "INITIALIZE MESH SQUAD",
                color = PureWhite,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SQUAD CALLSIGN",
                    color = GrayLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. SQUAD-13", color = GrayMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("squad_name_input"),
                    shape = RoundedCornerShape(2.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MISSION FREQUENCY / DESCRIPTION",
                    color = GrayLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("e.g. Tactical Recon Channel", color = GrayMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("squad_desc_input"),
                    shape = RoundedCornerShape(2.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "INCLUDE SAVED NODES (${savedPeers.size})",
                    color = GrayLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (savedPeers.isEmpty()) {
                    Text(
                        text = "No saved contacts yet. Scan nodes in RADAR tab to add members.",
                        color = GrayMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    savedPeers.forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedMembers.contains(peer.id)) selectedMembers.remove(peer.id)
                                    else selectedMembers.add(peer.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedMembers.contains(peer.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedMembers.add(peer.id)
                                    else selectedMembers.remove(peer.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PureWhite,
                                    checkmarkColor = PureBlack,
                                    uncheckedColor = GrayLight
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = peer.name,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(name, description, selectedMembers.toList())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("CREATE SQUAD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("CANCEL", fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isPlaying: Boolean,
    playProgress: Float,
    onTogglePlayAudio: () -> Unit
) {
    val isFromMe = message.isFromMe
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Sender info & Hop telemetry
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = if (isFromMe) "ME" else message.senderName.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isFromMe) PureWhite else GrayLight
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "HOP:${message.hops}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = GrayMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatTime(message.timestamp),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = GrayMuted
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isFromMe) PureWhite else DarkCard)
                .border(
                    width = 1.dp,
                    color = if (isFromMe) DarkBorder else PureWhite.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(10.dp)
        ) {
            when (message.type) {
                MessageType.TEXT -> {
                    Text(
                        text = message.text,
                        color = if (isFromMe) PureBlack else PureWhite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
                MessageType.AUDIO -> {
                    VoiceNotePlayer(
                        audioPath = message.audioFilePath ?: "",
                        durationSec = message.audioDurationSeconds,
                        isPlaying = isPlaying,
                        progress = playProgress,
                        isFromMe = isFromMe,
                        onTogglePlay = onTogglePlayAudio
                    )
                }
                MessageType.VIDEO -> {
                    VideoMessageView(
                        fileName = message.fileName ?: "video.mp4",
                        fileSize = message.fileSize,
                        isFromMe = isFromMe,
                        onPlayVideo = {
                            message.videoFilePath?.let { vPath ->
                                try {
                                    val file = File(vPath)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
                MessageType.FILE -> {
                    FileAttachmentView(
                        fileName = message.fileName ?: "attachment",
                        fileSize = message.fileSize,
                        isFromMe = isFromMe
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingControlBar(
    seconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val recScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recScale"
    )

    Surface(
        color = DarkCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Red.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(recScale)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = String.format("REC VOICE [ %02d:%02d ]", seconds / 60, seconds % 60),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Recording",
                        tint = GrayLight
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Voice Transmission",
                        tint = PureBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

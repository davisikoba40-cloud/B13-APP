package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerHelper
import com.example.audio.AudioRecorderHelper
import com.example.bluetooth.BluetoothMeshService
import com.example.data.PreferencesManager
import com.example.model.AppThemeMode
import com.example.model.ChatMessage
import com.example.model.MeshGroup
import com.example.model.MeshPeer
import com.example.model.MessageType
import com.example.model.UserProfile
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class AppScreen {
    SPLASH,
    PERMISSIONS_GUARD,
    MAIN_TABS,
    DIRECT_CHAT,
    GROUP_CHAT
}

enum class MainTab {
    CHANNELS,
    RADAR,
    CONTACTS,
    PROFILE
}

class MeshChatViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val prefsManager = PreferencesManager(context)
    val meshService = BluetoothMeshService(context)
    val audioRecorder = AudioRecorderHelper(context)
    val audioPlayer = AudioPlayerHelper(context)
    val notificationHelper = NotificationHelper(context)

    private val _themeMode = MutableStateFlow(prefsManager.getThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.CHANNELS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeChatPeer = MutableStateFlow<MeshPeer?>(null)
    val activeChatPeer: StateFlow<MeshPeer?> = _activeChatPeer.asStateFlow()

    private val _activeChatGroup = MutableStateFlow<MeshGroup?>(null)
    val activeChatGroup: StateFlow<MeshGroup?> = _activeChatGroup.asStateFlow()

    private val _groups = MutableStateFlow(prefsManager.getStoredGroups())
    val groups: StateFlow<List<MeshGroup>> = _groups.asStateFlow()

    private val _userProfile = MutableStateFlow(prefsManager.getUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _messages = MutableStateFlow(prefsManager.getStoredMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _savedPeers = MutableStateFlow(prefsManager.getSavedPeers())
    val savedPeers: StateFlow<List<MeshPeer>> = _savedPeers.asStateFlow()

    // Voice recording state
    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio = _isRecordingAudio.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds = _recordingSeconds.asStateFlow()

    private var recordingTimerJob: Job? = null

    // Status banner / toast
    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice = _statusNotice.asStateFlow()

    // Combined all peers (real discovered + saved merged)
    val allPeers: StateFlow<List<MeshPeer>> = combine(
        meshService.discoveredPeers,
        meshService.connectedPeers,
        _savedPeers
    ) { discovered, connected, saved ->
        val map = mutableMapOf<String, MeshPeer>()
        saved.forEach { map[it.macAddress] = it }
        discovered.forEach { disc ->
            val existing = map[disc.macAddress]
            map[disc.macAddress] = disc.copy(
                isSaved = existing?.isSaved == true,
                isConnected = connected.any { it.macAddress == disc.macAddress }
            )
        }
        connected.forEach { conn ->
            val existing = map[conn.macAddress]
            map[conn.macAddress] = conn.copy(
                isSaved = existing?.isSaved == true,
                isConnected = true
            )
        }
        map.values.toList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Collect incoming real Bluetooth messages and dispatch real push notifications
        viewModelScope.launch {
            meshService.incomingMessages.collect { incomingMsg ->
                val current = _messages.value.toMutableList()
                current.add(incomingMsg)
                _messages.value = current
                prefsManager.saveMessages(current)

                // Dispatch real system notification
                val groupName = _groups.value.find { it.id == incomingMsg.targetId }?.name
                notificationHelper.showIncomingMessageNotification(incomingMsg, groupName)
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefsManager.saveThemeMode(mode)
        postNotice("Theme applied: ${mode.name.replace("_", " ")}")
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openDirectChat(peer: MeshPeer) {
        _activeChatPeer.value = peer
        _activeChatGroup.value = null
        _currentScreen.value = AppScreen.DIRECT_CHAT
    }

    fun closeDirectChat() {
        _activeChatPeer.value = null
        _currentScreen.value = AppScreen.MAIN_TABS
    }

    fun openGroupChat(group: MeshGroup) {
        _activeChatGroup.value = group
        _activeChatPeer.value = null
        _currentScreen.value = AppScreen.GROUP_CHAT
    }

    fun closeGroupChat() {
        _activeChatGroup.value = null
        _currentScreen.value = AppScreen.MAIN_TABS
    }

    fun createGroup(name: String, description: String, memberIds: List<String>) {
        val newGroup = MeshGroup(
            id = "GROUP-${UUID.randomUUID().toString().take(6).uppercase()}",
            name = name.ifBlank { "SQUAD-${(10..99).random()}" },
            description = description.ifBlank { "Encrypted Mobilabs Squad" },
            createdAt = System.currentTimeMillis(),
            memberIds = memberIds
        )
        val updated = _groups.value.toMutableList().apply { add(0, newGroup) }
        _groups.value = updated
        prefsManager.saveGroups(updated)
        postNotice("Squad created: ${newGroup.name}")
    }

    fun deleteGroup(groupId: String) {
        val updated = _groups.value.filterNot { it.id == groupId }
        _groups.value = updated
        prefsManager.saveGroups(updated)
        postNotice("Squad disbanded")
    }

    fun postNotice(msg: String) {
        viewModelScope.launch {
            _statusNotice.value = msg
            delay(3500)
            if (_statusNotice.value == msg) {
                _statusNotice.value = null
            }
        }
    }

    fun sendText(text: String, targetId: String = "BROADCAST") {
        if (text.isBlank()) return
        val sentMsg = meshService.sendTextMessage(text.trim(), targetId, _userProfile.value)
        val current = _messages.value.toMutableList()
        current.add(sentMsg)
        _messages.value = current
        prefsManager.saveMessages(current)
    }

    fun startVoiceRecording() {
        val file = audioRecorder.startRecording()
        if (file != null) {
            _isRecordingAudio.value = true
            _recordingSeconds.value = 0
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (isActive && _isRecordingAudio.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                }
            }
        } else {
            postNotice("Mic permission or audio recorder initialization failed.")
        }
    }

    fun stopVoiceRecording(targetId: String = "BROADCAST") {
        recordingTimerJob?.cancel()
        _isRecordingAudio.value = false
        val (file, duration) = audioRecorder.stopRecording()
        if (file != null && file.exists() && file.length() > 0) {
            val sentMsg = meshService.sendAudioNote(file, duration, targetId, _userProfile.value)
            val current = _messages.value.toMutableList()
            current.add(sentMsg)
            _messages.value = current
            prefsManager.saveMessages(current)
            postNotice("Voice note sent ($duration sec)")
        } else {
            postNotice("Voice recording empty or cancelled.")
        }
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        _isRecordingAudio.value = false
        _recordingSeconds.value = 0
        audioRecorder.cancelRecording()
    }

    fun shareFileFromUri(uri: Uri, targetId: String = "BROADCAST") {
        viewModelScope.launch {
            try {
                var fileName = "mesh_attachment_${System.currentTimeMillis()}"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
                val dir = File(context.cacheDir, "b13_files").apply { mkdirs() }
                val targetFile = File(dir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    val sentMsg = meshService.sendFileAttachment(targetFile, targetId, _userProfile.value)
                    val current = _messages.value.toMutableList()
                    current.add(sentMsg)
                    _messages.value = current
                    prefsManager.saveMessages(current)
                    postNotice("File shared: $fileName")
                }
            } catch (e: Exception) {
                postNotice("File sharing failed: ${e.localizedMessage}")
            }
        }
    }

    fun shareVideoFromUri(uri: Uri, targetId: String = "BROADCAST") {
        viewModelScope.launch {
            try {
                var fileName = "mesh_video_${System.currentTimeMillis()}.mp4"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    }
                }
                val dir = File(context.cacheDir, "b13_videos").apply { mkdirs() }
                val targetFile = File(dir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    val sentMsg = meshService.sendVideoMessage(targetFile, targetId, _userProfile.value)
                    val current = _messages.value.toMutableList()
                    current.add(sentMsg)
                    _messages.value = current
                    prefsManager.saveMessages(current)
                    postNotice("Video shared: $fileName")
                }
            } catch (e: Exception) {
                postNotice("Video sharing failed: ${e.localizedMessage}")
            }
        }
    }

    fun toggleSavePeer(peer: MeshPeer) {
        val current = _savedPeers.value.toMutableList()
        val index = current.indexOfFirst { it.id == peer.id || it.macAddress == peer.macAddress }
        if (index >= 0) {
            current.removeAt(index)
            prefsManager.removeSavedPeer(peer.id)
            postNotice("Removed ${peer.name} from saved contacts")
        } else {
            val saved = peer.copy(isSaved = true)
            current.add(saved)
            prefsManager.savePeer(saved)
            postNotice("Saved ${peer.name} to contacts")
        }
        _savedPeers.value = current
    }

    fun updateProfile(displayName: String, bioStatus: String, isRelayEnabled: Boolean) {
        val updated = _userProfile.value.copy(
            displayName = displayName.ifBlank { "OPERATOR-13" },
            bioStatus = bioStatus.ifBlank { "STANDBY • MESH READY" },
            isRelayEnabled = isRelayEnabled
        )
        _userProfile.value = updated
        prefsManager.saveUserProfile(updated)
        postNotice("Node profile updated")
    }

    fun connectPeer(peer: MeshPeer) {
        postNotice("Connecting to ${peer.name} via Bluetooth RFCOMM...")
        meshService.connectToPeer(peer, _userProfile.value) { success, msg ->
            postNotice(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        meshService.onDestroy()
    }
}

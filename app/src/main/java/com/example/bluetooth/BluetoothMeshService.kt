package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.DeliveryStatus
import com.example.model.MeshPacket
import com.example.model.MeshPeer
import com.example.model.MessageType
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.UUID

class BluetoothMeshService(private val context: Context) {
    companion object {
        private const val TAG = "B13_MeshService"
        // Standard SPP UUID for Bluetooth RFCOMM P2P
        val B13_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val SERVICE_NAME = "B13_BYMOBILABS_MESH"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val connectedPeers = _connectedPeers.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val seenPacketIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val activeConnections = Collections.synchronizedList(mutableListOf<ConnectedPeerThread>())

    private var serverThread: AcceptThread? = null
    private var simulatedHeartbeatJob: Job? = null

    init {
        registerBluetoothStateReceiver()
        startMeshServer()
    }

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = state == BluetoothAdapter.STATE_ON
                    _isBluetoothEnabled.value = isEnabled
                    if (isEnabled) {
                        startMeshServer()
                    } else {
                        stopMeshServer()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    try {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-60).toShort()).toInt()
                        if (device != null) {
                            val name = try { device.name ?: "BT-${device.address.takeLast(5)}" } catch (_: Exception) { "BT-DEVICE" }
                            val peer = MeshPeer(
                                id = "NODE-${device.address.replace(":", "").takeLast(6)}",
                                macAddress = device.address,
                                name = name,
                                status = "Discovered RFCOMM",
                                lastSeenTimestamp = System.currentTimeMillis(),
                                isConnected = activeConnections.any { it.macAddress == device.address },
                                rssi = rssi
                            )
                            val current = _discoveredPeers.value.toMutableList()
                            val idx = current.indexOfFirst { it.macAddress == peer.macAddress }
                            if (idx >= 0) {
                                current[idx] = peer
                            } else {
                                current.add(peer)
                            }
                            _discoveredPeers.value = current
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing discovered device", e)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter?.isEnabled == true) {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            _isScanning.value = true
        } else {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun startMeshServer() {
        if (bluetoothAdapter?.isEnabled != true) return
        stopMeshServer()
        try {
            serverThread = AcceptThread().apply { start() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AcceptThread", e)
        }
    }

    fun stopMeshServer() {
        serverThread?.cancel()
        serverThread = null
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(peer: MeshPeer, myProfile: UserProfile, onResult: (Boolean, String) -> Unit) {
        if (bluetoothAdapter?.isEnabled != true) {
            onResult(false, "Bluetooth is disabled.")
            return
        }
        scope.launch {
            try {
                val device = bluetoothAdapter.getRemoteDevice(peer.macAddress)
                val socket = device.createRfcommSocketToServiceRecord(B13_UUID)
                bluetoothAdapter.cancelDiscovery()
                socket.connect()

                val connection = ConnectedPeerThread(socket, peer.macAddress, peer.name)
                activeConnections.add(connection)
                connection.start()

                // Send Handshake
                val handshakePacket = MeshPacket(
                    packetId = UUID.randomUUID().toString(),
                    originNodeId = myProfile.nodeId,
                    originName = myProfile.displayName,
                    targetNodeId = peer.id,
                    packetType = "HANDSHAKE",
                    content = myProfile.bioStatus
                )
                connection.writePacket(handshakePacket)

                updateConnectedPeersList()
                launch(Dispatchers.Main) {
                    onResult(true, "Connected to ${peer.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connect error: ${e.message}")
                launch(Dispatchers.Main) {
                    onResult(false, "Connection failed: ${e.localizedMessage ?: "Device unreachable"}")
                }
            }
        }
    }

    fun broadcastPacket(packet: MeshPacket) {
        seenPacketIds.add(packet.packetId)
        synchronized(activeConnections) {
            activeConnections.forEach { connection ->
                connection.writePacket(packet)
            }
        }
    }

    fun sendTextMessage(
        text: String,
        targetId: String,
        myProfile: UserProfile
    ): ChatMessage {
        val msgId = UUID.randomUUID().toString()
        val packet = MeshPacket(
            packetId = msgId,
            originNodeId = myProfile.nodeId,
            originName = myProfile.displayName,
            targetNodeId = targetId,
            packetType = "TEXT",
            content = text
        )
        broadcastPacket(packet)
        return ChatMessage(
            id = msgId,
            senderId = myProfile.nodeId,
            senderName = myProfile.displayName,
            targetId = targetId,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT,
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
    }

    fun sendAudioNote(
        audioFile: File,
        durationSec: Int,
        targetId: String,
        myProfile: UserProfile
    ): ChatMessage {
        val msgId = UUID.randomUUID().toString()
        val fileBytes = audioFile.readBytes()
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val packet = MeshPacket(
            packetId = msgId,
            originNodeId = myProfile.nodeId,
            originName = myProfile.displayName,
            targetNodeId = targetId,
            packetType = "AUDIO",
            content = base64Content,
            fileName = audioFile.name,
            fileSize = fileBytes.size.toLong(),
            durationSec = durationSec
        )
        broadcastPacket(packet)
        return ChatMessage(
            id = msgId,
            senderId = myProfile.nodeId,
            senderName = myProfile.displayName,
            targetId = targetId,
            text = "Voice transmission (${durationSec}s)",
            timestamp = System.currentTimeMillis(),
            type = MessageType.AUDIO,
            audioFilePath = audioFile.absolutePath,
            audioDurationSeconds = durationSec,
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
    }

    fun sendFileAttachment(
        file: File,
        targetId: String,
        myProfile: UserProfile
    ): ChatMessage {
        val msgId = UUID.randomUUID().toString()
        val fileBytes = file.readBytes()
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val packet = MeshPacket(
            packetId = msgId,
            originNodeId = myProfile.nodeId,
            originName = myProfile.displayName,
            targetNodeId = targetId,
            packetType = "FILE",
            content = base64Content,
            fileName = file.name,
            fileSize = fileBytes.size.toLong()
        )
        broadcastPacket(packet)
        return ChatMessage(
            id = msgId,
            senderId = myProfile.nodeId,
            senderName = myProfile.displayName,
            targetId = targetId,
            text = "Shared file: ${file.name}",
            timestamp = System.currentTimeMillis(),
            type = MessageType.FILE,
            fileName = file.name,
            fileSize = file.length(),
            filePath = file.absolutePath,
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
    }

    fun sendVideoMessage(
        videoFile: File,
        targetId: String,
        myProfile: UserProfile
    ): ChatMessage {
        val msgId = UUID.randomUUID().toString()
        val fileBytes = videoFile.readBytes()
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val packet = MeshPacket(
            packetId = msgId,
            originNodeId = myProfile.nodeId,
            originName = myProfile.displayName,
            targetNodeId = targetId,
            packetType = "VIDEO",
            content = base64Content,
            fileName = videoFile.name,
            fileSize = fileBytes.size.toLong()
        )
        broadcastPacket(packet)
        return ChatMessage(
            id = msgId,
            senderId = myProfile.nodeId,
            senderName = myProfile.displayName,
            targetId = targetId,
            text = "Video transmission: ${videoFile.name}",
            timestamp = System.currentTimeMillis(),
            type = MessageType.VIDEO,
            videoFilePath = videoFile.absolutePath,
            fileName = videoFile.name,
            fileSize = videoFile.length(),
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
    }

    private fun updateConnectedPeersList() {
        val list = activeConnections.map { conn ->
            MeshPeer(
                id = "NODE-${conn.macAddress.replace(":", "").takeLast(6)}",
                macAddress = conn.macAddress,
                name = conn.peerName,
                status = "Connected RFCOMM",
                lastSeenTimestamp = System.currentTimeMillis(),
                isConnected = true,
                rssi = -45
            )
        }
        _connectedPeers.value = list
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, B13_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket accept failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    val remoteDevice = it.remoteDevice
                    val connection = ConnectedPeerThread(
                        it,
                        remoteDevice?.address ?: "00:00:00:00:00:00",
                        remoteDevice?.name ?: "B13-PEER"
                    )
                    activeConnections.add(connection)
                    connection.start()
                    updateConnectedPeersList()
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectedPeerThread(
        private val mmSocket: BluetoothSocket,
        val macAddress: String,
        var peerName: String
    ) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val dataIn = DataInputStream(mmInStream)
        private val dataOut = DataOutputStream(mmOutStream)

        override fun run() {
            while (true) {
                try {
                    val length = dataIn.readInt()
                    if (length <= 0 || length > 10 * 1024 * 1024) break // Max 10MB packet
                    val buffer = ByteArray(length)
                    dataIn.readFully(buffer)
                    val jsonStr = String(buffer, Charsets.UTF_8)
                    handleIncomingPacket(jsonStr, this)
                } catch (e: IOException) {
                    Log.e(TAG, "Disconnected from $peerName", e)
                    break
                }
            }
            activeConnections.remove(this)
            updateConnectedPeersList()
        }

        fun writePacket(packet: MeshPacket) {
            scope.launch {
                try {
                    val json = JSONObject().apply {
                        put("packetId", packet.packetId)
                        put("originNodeId", packet.originNodeId)
                        put("originName", packet.originName)
                        put("targetNodeId", packet.targetNodeId)
                        put("hops", packet.hops)
                        put("ttl", packet.ttl)
                        put("packetType", packet.packetType)
                        put("content", packet.content)
                        put("fileName", packet.fileName ?: "")
                        put("fileSize", packet.fileSize)
                        put("durationSec", packet.durationSec)
                    }.toString()
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    synchronized(dataOut) {
                        dataOut.writeInt(bytes.size)
                        dataOut.write(bytes)
                        dataOut.flush()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error writing packet", e)
                }
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (_: IOException) {}
        }
    }

    private fun handleIncomingPacket(jsonStr: String, sourceThread: ConnectedPeerThread) {
        try {
            val obj = JSONObject(jsonStr)
            val packetId = obj.getString("packetId")
            if (seenPacketIds.contains(packetId)) return
            seenPacketIds.add(packetId)

            val originNodeId = obj.getString("originNodeId")
            val originName = obj.getString("originName")
            val targetNodeId = obj.optString("targetNodeId", "BROADCAST")
            val hops = obj.optInt("hops", 0)
            val ttl = obj.optInt("ttl", 3)
            val packetType = obj.getString("packetType")
            val content = obj.optString("content", "")
            val fileName = obj.optString("fileName").takeIf { it.isNotEmpty() }
            val fileSize = obj.optLong("fileSize", 0L)
            val durationSec = obj.optInt("durationSec", 0)

            sourceThread.peerName = originName

            // Relay if within TTL
            if (hops + 1 < ttl) {
                val relayPacket = MeshPacket(
                    packetId = packetId,
                    originNodeId = originNodeId,
                    originName = originName,
                    targetNodeId = targetNodeId,
                    hops = hops + 1,
                    ttl = ttl,
                    packetType = packetType,
                    content = content,
                    fileName = fileName,
                    fileSize = fileSize,
                    durationSec = durationSec
                )
                synchronized(activeConnections) {
                    activeConnections.filter { it != sourceThread }.forEach {
                        it.writePacket(relayPacket)
                    }
                }
            }

            scope.launch {
                when (packetType) {
                    "HANDSHAKE" -> {
                        updateConnectedPeersList()
                    }
                    "TEXT" -> {
                        val message = ChatMessage(
                            id = packetId,
                            senderId = originNodeId,
                            senderName = originName,
                            targetId = targetNodeId,
                            text = content,
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.TEXT,
                            hops = hops + 1,
                            isFromMe = false
                        )
                        _incomingMessages.emit(message)
                    }
                    "AUDIO" -> {
                        val audioFile = saveReceivedBase64File(
                            base64Data = content,
                            fileName = fileName ?: "VOICE_${System.currentTimeMillis()}.m4a",
                            subDir = "b13_audio_notes"
                        )
                        val message = ChatMessage(
                            id = packetId,
                            senderId = originNodeId,
                            senderName = originName,
                            targetId = targetNodeId,
                            text = "Voice transmission (${durationSec}s)",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.AUDIO,
                            audioFilePath = audioFile?.absolutePath,
                            audioDurationSeconds = durationSec,
                            hops = hops + 1,
                            isFromMe = false
                        )
                        _incomingMessages.emit(message)
                    }
                    "VIDEO" -> {
                        val videoFile = saveReceivedBase64File(
                            base64Data = content,
                            fileName = fileName ?: "VIDEO_${System.currentTimeMillis()}.mp4",
                            subDir = "b13_videos"
                        )
                        val message = ChatMessage(
                            id = packetId,
                            senderId = originNodeId,
                            senderName = originName,
                            targetId = targetNodeId,
                            text = "Video transmission: ${fileName ?: "video"}",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.VIDEO,
                            videoFilePath = videoFile?.absolutePath,
                            fileName = fileName,
                            fileSize = fileSize,
                            hops = hops + 1,
                            isFromMe = false
                        )
                        _incomingMessages.emit(message)
                    }
                    "FILE" -> {
                        val savedFile = saveReceivedBase64File(
                            base64Data = content,
                            fileName = fileName ?: "FILE_${System.currentTimeMillis()}",
                            subDir = "b13_files"
                        )
                        val message = ChatMessage(
                            id = packetId,
                            senderId = originNodeId,
                            senderName = originName,
                            targetId = targetNodeId,
                            text = "Received file: ${fileName ?: "attachment"}",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.FILE,
                            fileName = fileName,
                            fileSize = fileSize,
                            filePath = savedFile?.absolutePath,
                            hops = hops + 1,
                            isFromMe = false
                        )
                        _incomingMessages.emit(message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Packet handling error", e)
        }
    }

    private fun saveReceivedBase64File(base64Data: String, fileName: String, subDir: String): File? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val dir = File(context.cacheDir, subDir).apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64 file", e)
            null
        }
    }

    fun onDestroy() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        stopMeshServer()
        activeConnections.forEach { it.cancel() }
        activeConnections.clear()
        simulatedHeartbeatJob?.cancel()
    }
}

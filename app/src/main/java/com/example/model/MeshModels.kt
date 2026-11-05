package com.example.model

enum class MessageType {
    TEXT,
    AUDIO,
    VIDEO,
    FILE
}

enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

enum class AppThemeMode {
    BLACK_WHITE,
    PINK_WHITE,
    BLUE_WHITE,
    DARK_STEALTH
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val targetId: String = "BROADCAST",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val audioFilePath: String? = null,
    val audioDurationSeconds: Int = 0,
    val videoFilePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val filePath: String? = null,
    val hops: Int = 0,
    val isFromMe: Boolean = false,
    val status: DeliveryStatus = DeliveryStatus.SENT
)

data class MeshPeer(
    val id: String,
    val macAddress: String,
    val name: String,
    val status: String = "Discovered RFCOMM",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false,
    val isSaved: Boolean = false,
    val rssi: Int = -60,
    val packetsRelayed: Int = 0
)

data class UserProfile(
    val nodeId: String,
    val displayName: String,
    val bioStatus: String,
    val isRelayEnabled: Boolean = true,
    val avatarSeed: Int = 1
)

data class MeshPacket(
    val packetId: String,
    val originNodeId: String,
    val originName: String,
    val targetNodeId: String = "BROADCAST",
    val hops: Int = 0,
    val ttl: Int = 3,
    val packetType: String, // "TEXT", "AUDIO", "VIDEO", "FILE", "HANDSHAKE"
    val content: String = "",
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val durationSec: Int = 0
)

data class MeshGroup(
    val id: String,
    val name: String,
    val description: String = "Tactical Mesh Squad",
    val createdAt: Long = System.currentTimeMillis(),
    val memberIds: List<String> = emptyList()
)

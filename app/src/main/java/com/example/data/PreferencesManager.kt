package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppThemeMode
import com.example.model.ChatMessage
import com.example.model.MeshGroup
import com.example.model.MeshPeer
import com.example.model.MessageType
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("b13_mesh_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NODE_ID = "pref_node_id"
        private const val KEY_DISPLAY_NAME = "pref_display_name"
        private const val KEY_BIO_STATUS = "pref_bio_status"
        private const val KEY_RELAY_ENABLED = "pref_relay_enabled"
        private const val KEY_AVATAR_SEED = "pref_avatar_seed"
        private const val KEY_SAVED_PEERS = "pref_saved_peers"
        private const val KEY_MESSAGES = "pref_messages"
        private const val KEY_GROUPS = "pref_groups"
        private const val KEY_THEME_MODE = "pref_theme_mode"
    }

    fun getThemeMode(): AppThemeMode {
        val str = prefs.getString(KEY_THEME_MODE, AppThemeMode.BLACK_WHITE.name)
        return try {
            AppThemeMode.valueOf(str ?: AppThemeMode.BLACK_WHITE.name)
        } catch (_: Exception) {
            AppThemeMode.BLACK_WHITE
        }
    }

    fun saveThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getUserProfile(): UserProfile {
        var nodeId = prefs.getString(KEY_NODE_ID, null)
        if (nodeId == null) {
            val randomSuffix = UUID.randomUUID().toString().take(4).uppercase()
            nodeId = "B13-NODE-$randomSuffix"
            prefs.edit().putString(KEY_NODE_ID, nodeId).apply()
        }
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "OPERATOR-13") ?: "OPERATOR-13"
        val bioStatus = prefs.getString(KEY_BIO_STATUS, "STANDBY • MESH READY") ?: "STANDBY • MESH READY"
        val isRelayEnabled = prefs.getBoolean(KEY_RELAY_ENABLED, true)
        val avatarSeed = prefs.getInt(KEY_AVATAR_SEED, 1)

        return UserProfile(
            nodeId = nodeId,
            displayName = displayName,
            bioStatus = bioStatus,
            isRelayEnabled = isRelayEnabled,
            avatarSeed = avatarSeed
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NODE_ID, profile.nodeId)
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putString(KEY_BIO_STATUS, profile.bioStatus)
            .putBoolean(KEY_RELAY_ENABLED, profile.isRelayEnabled)
            .putInt(KEY_AVATAR_SEED, profile.avatarSeed)
            .apply()
    }

    fun getSavedPeers(): List<MeshPeer> {
        val jsonString = prefs.getString(KEY_SAVED_PEERS, null) ?: return emptyList()
        val list = mutableListOf<MeshPeer>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MeshPeer(
                        id = obj.getString("id"),
                        macAddress = obj.optString("macAddress", "00:11:22:33:44:55"),
                        name = obj.getString("name"),
                        status = obj.optString("status", "Active"),
                        lastSeenTimestamp = obj.optLong("lastSeen", System.currentTimeMillis()),
                        isConnected = false,
                        isSaved = true,
                        rssi = obj.optInt("rssi", -65),
                        packetsRelayed = obj.optInt("relayed", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun savePeers(peers: List<MeshPeer>) {
        val jsonArray = JSONArray()
        peers.filter { it.isSaved }.forEach { peer ->
            val obj = JSONObject().apply {
                put("id", peer.id)
                put("macAddress", peer.macAddress)
                put("name", peer.name)
                put("status", peer.status)
                put("lastSeen", peer.lastSeenTimestamp)
                put("rssi", peer.rssi)
                put("relayed", peer.packetsRelayed)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_PEERS, jsonArray.toString()).apply()
    }

    fun savePeer(peer: MeshPeer) {
        val current = getSavedPeers().toMutableList()
        val index = current.indexOfFirst { it.id == peer.id || it.macAddress == peer.macAddress }
        if (index >= 0) {
            current[index] = peer.copy(isSaved = true)
        } else {
            current.add(peer.copy(isSaved = true))
        }
        savePeers(current)
    }

    fun removeSavedPeer(peerId: String) {
        val current = getSavedPeers().filterNot { it.id == peerId }
        savePeers(current)
    }

    fun getStoredMessages(): List<ChatMessage> {
        val jsonString = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        val list = mutableListOf<ChatMessage>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", "TEXT")
                val type = when (typeStr) {
                    "AUDIO" -> MessageType.AUDIO
                    "VIDEO" -> MessageType.VIDEO
                    "FILE" -> MessageType.FILE
                    else -> MessageType.TEXT
                }
                list.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        senderId = obj.getString("senderId"),
                        senderName = obj.getString("senderName"),
                        targetId = obj.optString("targetId", "BROADCAST"),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        type = type,
                        audioFilePath = obj.optString("audioFilePath").takeIf { it.isNotEmpty() },
                        audioDurationSeconds = obj.optInt("audioDurationSeconds", 0),
                        videoFilePath = obj.optString("videoFilePath").takeIf { it.isNotEmpty() },
                        fileName = obj.optString("fileName").takeIf { it.isNotEmpty() },
                        fileSize = obj.optLong("fileSize", 0L),
                        filePath = obj.optString("filePath").takeIf { it.isNotEmpty() },
                        hops = obj.optInt("hops", 0),
                        isFromMe = obj.optBoolean("isFromMe", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val jsonArray = JSONArray()
        // Save at most the latest 300 messages to keep storage snappy
        val sublist = if (messages.size > 300) messages.takeLast(300) else messages
        sublist.forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("senderId", msg.senderId)
                put("senderName", msg.senderName)
                put("targetId", msg.targetId)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
                put("type", msg.type.name)
                put("audioFilePath", msg.audioFilePath ?: "")
                put("audioDurationSeconds", msg.audioDurationSeconds)
                put("videoFilePath", msg.videoFilePath ?: "")
                put("fileName", msg.fileName ?: "")
                put("fileSize", msg.fileSize)
                put("filePath", msg.filePath ?: "")
                put("hops", msg.hops)
                put("isFromMe", msg.isFromMe)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MESSAGES, jsonArray.toString()).apply()
    }

    fun getStoredGroups(): List<MeshGroup> {
        val jsonString = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        val list = mutableListOf<MeshGroup>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val members = mutableListOf<String>()
                val membersArray = obj.optJSONArray("members")
                if (membersArray != null) {
                    for (j in 0 until membersArray.length()) {
                        members.add(membersArray.getString(j))
                    }
                }
                list.add(
                    MeshGroup(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        memberIds = members
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveGroups(groups: List<MeshGroup>) {
        val jsonArray = JSONArray()
        groups.forEach { group ->
            val obj = JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("description", group.description)
                put("createdAt", group.createdAt)
                val membersArray = JSONArray()
                group.memberIds.forEach { membersArray.put(it) }
                put("members", membersArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_GROUPS, jsonArray.toString()).apply()
    }

    fun addGroup(group: MeshGroup) {
        val current = getStoredGroups().toMutableList()
        current.removeAll { it.id == group.id }
        current.add(0, group)
        saveGroups(current)
    }

    fun deleteGroup(groupId: String) {
        val current = getStoredGroups().filterNot { it.id == groupId }
        saveGroups(current)
    }
}

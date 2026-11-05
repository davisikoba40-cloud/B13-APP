package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ChatMessage
import com.example.model.MessageType

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "b13_mesh_transmissions"
        const val CHANNEL_NAME = "B13 Mesh Transmissions"
        const val CHANNEL_DESC = "Push notifications for incoming peer and squad mesh messages"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 200)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showIncomingMessageNotification(message: ChatMessage, groupName: String? = null) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_ID", message.targetId)
            putExtra("EXTRA_SENDER_ID", message.senderId)
            putExtra("EXTRA_SENDER_NAME", message.senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (groupName != null) {
            "[$groupName] ${message.senderName}"
        } else if (message.targetId == "BROADCAST") {
            "[GLOBAL MESH] ${message.senderName}"
        } else {
            "B13: ${message.senderName}"
        }

        val contentText = when (message.type) {
            MessageType.TEXT -> message.text
            MessageType.AUDIO -> "Voice transmission (${message.audioDurationSeconds}s)"
            MessageType.VIDEO -> "Video transmission: ${message.fileName ?: "video"}"
            MessageType.FILE -> "Shared file: ${message.fileName ?: "document"}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_b13_logo)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 150, 100, 200))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            notificationManager.notify(message.targetId.hashCode(), notification)
        } catch (_: SecurityException) {}
    }
}

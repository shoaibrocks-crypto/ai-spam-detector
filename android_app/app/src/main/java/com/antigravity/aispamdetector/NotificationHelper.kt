package com.antigravity.aispamdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_SPAM_ID = "ai_spam_detector_alerts"
    private const val CHANNEL_SAFE_ID = "ai_spam_detector_safe"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High Priority Spam Alert Channel (Heads-Up Alert + Vibration)
            val spamChannel = NotificationChannel(
                CHANNEL_SPAM_ID,
                "AI Spam & Phishing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when incoming SMS or notifications are detected as scams"
                enableVibration(true)
                enableLights(true)
            }

            // Normal Notification Channel
            val safeChannel = NotificationChannel(
                CHANNEL_SAFE_ID,
                "Safe Message Verifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Passive notifications for legitimate SMS"
            }

            manager.createNotificationChannel(spamChannel)
            manager.createNotificationChannel(safeChannel)
        }
    }

    fun showSpamAlert(context: Context, sender: String, result: SpamAnalysisResult) {
        initNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TEXT", result.rawText)
            putExtra("EXTRA_VERDICT", result.verdict)
            putExtra("EXTRA_CONFIDENCE", result.confidence)
            putExtra("EXTRA_HEADLINE", result.headline)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SPAM_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 [SPAM DETECTED] From: $sender")
            .setContentText("${result.headline} (${result.confidence})")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🚨 Risk Level: ${result.riskLevel} (${result.confidence})\n\n${result.headline}\n\nSecurity Advice: ${result.recommendation}\n\nMessage:\n\"${result.rawText}\""))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    fun showSafeNotification(context: Context, sender: String, result: SpamAnalysisResult) {
        initNotificationChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_SAFE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("🛡️ [VERIFIED SAFE] From: $sender")
            .setContentText("Clean message • ${result.confidence} Ham confidence")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }
}

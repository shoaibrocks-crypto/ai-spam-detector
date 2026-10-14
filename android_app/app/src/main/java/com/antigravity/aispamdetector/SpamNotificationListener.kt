package com.antigravity.aispamdetector

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SpamNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SpamNotifListener"
        val MONITORED_PACKAGES = mapOf(
            "com.whatsapp" to "WhatsApp",
            "org.telegram.messenger" to "Telegram",
            "com.instagram.android" to "Instagram",
            "com.google.android.gm" to "Gmail",
            "com.google.android.apps.messaging" to "SMS",
            "com.samsung.android.messaging" to "SMS"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return

        // Skip our own app notifications to avoid loops
        if (packageName == this.packageName) return

        val sourceName = MONITORED_PACKAGES[packageName] ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""

        if (text.length < 5) return

        Log.d(TAG, "[$sourceName] Notification received from $title: $text")

        // 1. Whitelist Check
        val whitelistManager = WhitelistManager(this)
        if (whitelistManager.isWhitelisted(title)) {
            Log.d(TAG, "Sender $title is in whitelist. Skipping spam scan.")
            return
        }

        // 2. Run AI detection in background thread
        Thread {
            try {
                val result = SpamApiClient.analyzeMessage(this, text)

                // 3. Save to History Log
                val historyManager = HistoryManager(this)
                val record = HistoryRecord(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    sender = "$sourceName: $title",
                    source = sourceName,
                    text = text,
                    verdict = result.verdict,
                    confidence = result.confidence,
                    headline = result.headline,
                    riskLevel = result.riskLevel
                )
                historyManager.addRecord(record)

                // 4. Alert user if Spam
                if (result.verdict == "SPAM") {
                    NotificationHelper.showSpamAlert(this, "$sourceName ($title)", result)
                }

                // 5. Broadcast update to UI if active
                val updateIntent = Intent(SmsReceiver.ACTION_NEW_DETECTION).apply {
                    putExtra("sender", "$sourceName: $title")
                    putExtra("source", sourceName)
                    putExtra("text", text)
                    putExtra("verdict", result.verdict)
                    putExtra("confidence", result.confidence)
                    putExtra("headline", result.headline)
                    putExtra("riskLevel", result.riskLevel)
                    setPackage(this@SpamNotificationListener.packageName)
                }
                sendBroadcast(updateIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing notification: ${e.message}")
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }
}

package com.antigravity.aispamdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_NEW_DETECTION = "com.antigravity.aispamdetector.ACTION_NEW_DETECTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0].originatingAddress ?: "Unknown"
            val bodyBuilder = StringBuilder()
            for (sms in messages) {
                bodyBuilder.append(sms.messageBody)
            }
            val fullMessage = bodyBuilder.toString()

            Log.d("SmsReceiver", "Intercepted incoming SMS from $sender: $fullMessage")

            // Whitelist Check
            val whitelistManager = WhitelistManager(context)
            if (whitelistManager.isWhitelisted(sender)) {
                Log.d("SmsReceiver", "Sender $sender is whitelisted. Skipping.")
                return
            }

            val pendingResult = goAsync()
            // Pure standard Java Thread (No kotlinx.coroutines needed!)
            Thread {
                try {
                    val result = SpamApiClient.analyzeMessage(context, fullMessage)

                    // Save to persistent history
                    val historyManager = HistoryManager(context)
                    historyManager.addRecord(
                        HistoryRecord(
                            id = System.currentTimeMillis().toString(),
                            timestamp = System.currentTimeMillis(),
                            sender = sender,
                            source = "SMS",
                            text = fullMessage,
                            verdict = result.verdict,
                            confidence = result.confidence,
                            headline = result.headline,
                            riskLevel = result.riskLevel
                        )
                    )

                    if (result.verdict == "SPAM") {
                        NotificationHelper.showSpamAlert(context, sender, result)
                    } else {
                        NotificationHelper.showSafeNotification(context, sender, result)
                    }

                    val updateIntent = Intent(ACTION_NEW_DETECTION).apply {
                        putExtra("sender", sender)
                        putExtra("source", "SMS")
                        putExtra("text", fullMessage)
                        putExtra("verdict", result.verdict)
                        putExtra("confidence", result.confidence)
                        putExtra("headline", result.headline)
                        putExtra("riskLevel", result.riskLevel)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(updateIntent)

                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error analyzing SMS: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
}

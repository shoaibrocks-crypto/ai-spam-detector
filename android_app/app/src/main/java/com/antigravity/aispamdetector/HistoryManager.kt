package com.antigravity.aispamdetector

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryRecord(
    val id: String,
    val timestamp: Long,
    val sender: String,
    val source: String, // SMS, WhatsApp, Telegram, Manual
    val text: String,
    val verdict: String,
    val confidence: String,
    val headline: String,
    val riskLevel: String
)

class HistoryManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "spam_detector_history"
        private const val KEY_HISTORY = "history_records"
        private const val MAX_RECORDS = 50
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun addRecord(record: HistoryRecord) {
        val list = getRecords().toMutableList()
        list.add(0, record)
        if (list.size > MAX_RECORDS) {
            list.removeAt(list.size - 1)
        }
        saveRecords(list)
    }

    fun getRecords(): List<HistoryRecord> {
        val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = ArrayList<HistoryRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryRecord(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        sender = obj.optString("sender", "Unknown"),
                        source = obj.optString("source", "SMS"),
                        text = obj.optString("text", ""),
                        verdict = obj.optString("verdict", "HAM"),
                        confidence = obj.optString("confidence", "90%"),
                        headline = obj.optString("headline", "Analyzed"),
                        riskLevel = obj.optString("riskLevel", "SAFE")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveRecords(records: List<HistoryRecord>) {
        val arr = JSONArray()
        for (r in records) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("timestamp", r.timestamp)
                put("sender", r.sender)
                put("source", r.source)
                put("text", r.text)
                put("verdict", r.verdict)
                put("confidence", r.confidence)
                put("headline", r.headline)
                put("riskLevel", r.riskLevel)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}

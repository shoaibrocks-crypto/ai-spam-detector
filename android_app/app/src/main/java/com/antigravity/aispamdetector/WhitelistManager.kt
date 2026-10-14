package com.antigravity.aispamdetector

import android.content.Context

class WhitelistManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "spam_detector_whitelist"
        private const val KEY_WHITELIST = "trusted_senders"
        val DEFAULT_TRUSTED = setOf("HDFCBK", "SBIINB", "ICICIB", "AXISBK", "SWIGGY", "ZOMATO", "UBER")
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWhitelisted(sender: String): Boolean {
        if (sender.isBlank()) return false
        val clean = sender.trim().uppercase()
        val set = getWhitelistedSenders()
        return set.any { clean.contains(it.uppercase()) || it.uppercase().contains(clean) }
    }

    fun getWhitelistedSenders(): Set<String> {
        val stored = prefs.getStringSet(KEY_WHITELIST, null)
        return stored ?: DEFAULT_TRUSTED
    }

    fun addSender(sender: String) {
        val clean = sender.trim().uppercase()
        if (clean.isBlank()) return
        val current = getWhitelistedSenders().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
    }

    fun removeSender(sender: String) {
        val clean = sender.trim().uppercase()
        val current = getWhitelistedSenders().toMutableSet()
        current.remove(clean)
        prefs.edit().putStringSet(KEY_WHITELIST, current).apply()
    }
}

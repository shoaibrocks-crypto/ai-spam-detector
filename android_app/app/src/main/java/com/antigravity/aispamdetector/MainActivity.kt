package com.antigravity.aispamdetector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val COLOR_BG_APP = Color.parseColor("#080C14")
    private val COLOR_BG_CARD = Color.parseColor("#0F172A")
    private val COLOR_BG_INPUT = Color.parseColor("#0B1120")
    private val COLOR_BORDER = Color.parseColor("#2038BDF8")
    private val COLOR_CYAN = Color.parseColor("#38BDF8")
    private val COLOR_BLUE = Color.parseColor("#2563EB")
    private val COLOR_SPAM = Color.parseColor("#EF4444")
    private val COLOR_HAM = Color.parseColor("#10B981")
    private val COLOR_TEXT_MAIN = Color.parseColor("#F8FAFC")
    private val COLOR_TEXT_MUTED = Color.parseColor("#94A3B8")
    private val COLOR_TEXT_SUB = Color.parseColor("#64748B")

    // Navigation Tabs
    private lateinit var tabScannerLayout: LinearLayout
    private lateinit var tabHistoryLayout: LinearLayout
    private lateinit var tabSettingsLayout: LinearLayout
    private lateinit var btnTabScanner: Button
    private lateinit var btnTabHistory: Button
    private lateinit var btnTabSettings: Button

    // Scanner UI
    private lateinit var etManualScan: EditText
    private lateinit var btnRunScan: Button
    private lateinit var pbScanning: ProgressBar
    private lateinit var layoutScanResult: LinearLayout
    private lateinit var tvManualVerdict: TextView
    private lateinit var tvManualConfidence: TextView
    private lateinit var tvManualHeadline: TextView
    private lateinit var tvManualRec: TextView
    private var currentSensitivity = "balanced"

    // History UI
    private lateinit var llHistoryList: LinearLayout
    private lateinit var tvEmptyHistory: TextView
    private var currentHistoryFilter = "ALL"

    // Settings UI
    private lateinit var tvNotifAccessStatus: TextView
    private lateinit var llWhitelistChips: LinearLayout

    private lateinit var historyManager: HistoryManager
    private lateinit var whitelistManager: WhitelistManager

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_NEW_DETECTION) {
                runOnUiThread {
                    refreshHistoryList()
                    showIncomingDetectionBanner(
                        sender = intent.getStringExtra("sender") ?: "Unknown",
                        source = intent.getStringExtra("source") ?: "SMS",
                        text = intent.getStringExtra("text") ?: "",
                        verdict = intent.getStringExtra("verdict") ?: "HAM",
                        confidence = intent.getStringExtra("confidence") ?: "90%",
                        headline = intent.getStringExtra("headline") ?: "Scanned"
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyManager = HistoryManager(this)
        whitelistManager = WhitelistManager(this)

        buildProgrammaticUI()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SmsReceiver.ACTION_NEW_DETECTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
        updateNotifAccessStatus()
        refreshHistoryList()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun createRoundedDrawable(bgColor: Int, strokeColor: Int = COLOR_BORDER, radiusDp: Float = 12f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = radiusDp * resources.displayMetrics.density
            setStroke(dp(1), strokeColor)
        }
    }

    private fun buildProgrammaticUI() {
        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG_APP)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        // --- TOP HEADER ---
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
        }

        val tvShield = TextView(this).apply {
            text = "🛡️"
            textSize = 24f
        }

        val headerTextLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            }
        }

        val tvTitle = TextView(this).apply {
            text = "AI Spam Detector"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvSub = TextView(this).apply {
            text = "SMS • WhatsApp • Telegram • Emails"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 11f
        }

        headerTextLayout.addView(tvTitle)
        headerTextLayout.addView(tvSub)
        header.addView(tvShield)
        header.addView(headerTextLayout)
        rootContainer.addView(header)

        // --- TAB BAR NAVIGATION ---
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(8))
        }

        btnTabScanner = createTabButton("🔍 Scanner", true) { switchTab(0) }
        btnTabHistory = createTabButton("📋 History", false) { switchTab(1) }
        btnTabSettings = createTabButton("⚙️ Settings", false) { switchTab(2) }

        tabBar.addView(btnTabScanner)
        tabBar.addView(btnTabHistory)
        tabBar.addView(btnTabSettings)
        rootContainer.addView(tabBar)

        // --- CONTENT SCROLLER ---
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(24))
        }

        // TAB 1: SCANNER
        tabScannerLayout = buildScannerTab()
        contentLayout.addView(tabScannerLayout)

        // TAB 2: HISTORY
        tabHistoryLayout = buildHistoryTab()
        tabHistoryLayout.visibility = View.GONE
        contentLayout.addView(tabHistoryLayout)

        // TAB 3: SETTINGS
        tabSettingsLayout = buildSettingsTab()
        tabSettingsLayout.visibility = View.GONE
        contentLayout.addView(tabSettingsLayout)

        scrollView.addView(contentLayout)
        rootContainer.addView(scrollView)

        setContentView(rootContainer)
    }

    private fun createTabButton(title: String, isSelected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            textSize = 12f
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(if (isSelected) Color.WHITE else COLOR_TEXT_MUTED)
            background = createRoundedDrawable(
                if (isSelected) COLOR_BLUE else Color.parseColor("#1E293B"),
                if (isSelected) COLOR_CYAN else Color.TRANSPARENT,
                radiusDp = 8f
            )
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tabIndex: Int) {
        tabScannerLayout.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        tabHistoryLayout.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        tabSettingsLayout.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE

        updateTabStyle(btnTabScanner, tabIndex == 0)
        updateTabStyle(btnTabHistory, tabIndex == 1)
        updateTabStyle(btnTabSettings, tabIndex == 2)

        if (tabIndex == 1) refreshHistoryList()
        if (tabIndex == 2) updateNotifAccessStatus()
    }

    private fun updateTabStyle(btn: Button, selected: Boolean) {
        btn.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        btn.setTextColor(if (selected) Color.WHITE else COLOR_TEXT_MUTED)
        btn.background = createRoundedDrawable(
            if (selected) COLOR_BLUE else Color.parseColor("#1E293B"),
            if (selected) COLOR_CYAN else Color.TRANSPARENT,
            radiusDp = 8f
        )
    }

    // ==========================================
    // TAB 1: SCANNER VIEW
    // ==========================================
    private fun buildScannerTab(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Status Card
        val cardStatus = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_HAM)
            }
        }

        val statusInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            }
        }

        val tvStatus = TextView(this).apply {
            text = "AI Interceptor: ACTIVE"
            setTextColor(COLOR_HAM)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvStatusSub = TextView(this).apply {
            text = "SMS, WhatsApp & Telegram auto-monitored"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
        }

        statusInfo.addView(tvStatus)
        statusInfo.addView(tvStatusSub)
        cardStatus.addView(dot)
        cardStatus.addView(statusInfo)
        layout.addView(cardStatus)

        // Sensitivity Switcher
        val tvSensLabel = TextView(this).apply {
            text = "THREAT SENSITIVITY"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), dp(14), 0, dp(6))
        }
        layout.addView(tvSensLabel)

        val sensRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnSensBal = Button(this)
        val btnSensAgg = Button(this)
        val btnSensRel = Button(this)

        fun updateSensUi(chosen: String) {
            currentSensitivity = chosen
            btnSensBal.background = createRoundedDrawable(if (chosen == "balanced") COLOR_CYAN else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnSensBal.setTextColor(if (chosen == "balanced") Color.BLACK else COLOR_TEXT_MUTED)

            btnSensAgg.background = createRoundedDrawable(if (chosen == "aggressive") COLOR_SPAM else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnSensAgg.setTextColor(if (chosen == "aggressive") Color.WHITE else COLOR_TEXT_MUTED)

            btnSensRel.background = createRoundedDrawable(if (chosen == "relaxed") COLOR_HAM else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnSensRel.setTextColor(if (chosen == "relaxed") Color.WHITE else COLOR_TEXT_MUTED)
        }

        btnSensBal.apply {
            text = "Balanced"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { updateSensUi("balanced") }
        }
        btnSensAgg.apply {
            text = "Aggressive"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { updateSensUi("aggressive") }
        }
        btnSensRel.apply {
            text = "Relaxed"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f)
            setOnClickListener { updateSensUi("relaxed") }
        }

        sensRow.addView(btnSensBal)
        sensRow.addView(btnSensAgg)
        sensRow.addView(btnSensRel)
        layout.addView(sensRow)
        updateSensUi("balanced")

        // Input Box
        val tvInputLabel = TextView(this).apply {
            text = "MANUAL MESSAGE SCANNER"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), dp(14), 0, dp(6))
        }
        layout.addView(tvInputLabel)

        etManualScan = EditText(this).apply {
            hint = "Paste any SMS, WhatsApp text, email, or link..."
            setHintTextColor(COLOR_TEXT_SUB)
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 13f
            background = createRoundedDrawable(COLOR_BG_INPUT, radiusDp = 10f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        layout.addView(etManualScan)

        // Preset Chips
        val hScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val presets = listOf(
            Pair("🇮🇳 KBC Lottery", "Badhai ho! Aapka number KBC Lucky Draw mein 25 Lakh jeeta hai. WhatsApp +91-9988776655 par claim karein http://kbc-lottery-claim.xyz"),
            Pair("⚡ Bijli Cutoff", "Dear Customer, Aapka electricity connection kat diya jayega aaj raat 9:30 baje. Turant call karein: 9876543210"),
            Pair("🏦 Bank Phish", "URGENT: Your Chase account is locked. Verify at http://192.168.1.50/sbi-auth to prevent deactivation."),
            Pair("🔐 Safe OTP", "Your OTP for SBI Net Banking is 847291. Valid for 3 minutes. Do not share with anyone."),
            Pair("📦 Delivery", "USPS Alert: Package tracking #US89218 held. Pay $2.49 redelivery fee at http://usps-redelivery.xyz")
        )

        for (p in presets) {
            val chip = TextView(this).apply {
                text = p.first
                setTextColor(COLOR_CYAN)
                textSize = 11f
                background = createRoundedDrawable(Color.parseColor("#172554"), COLOR_BORDER, radiusDp = 14f)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener { etManualScan.setText(p.second) }
            }
            chipRow.addView(chip)
        }
        hScroll.addView(chipRow)
        layout.addView(hScroll)

        // Action Buttons Row
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }

        btnRunScan = Button(this).apply {
            text = "⚡ Scan Message with AI"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = createRoundedDrawable(COLOR_BLUE, COLOR_CYAN, radiusDp = 10f)
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            setOnClickListener { executeManualScan() }
        }

        val btnClear = Button(this).apply {
            text = "Clear"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 12f
            background = createRoundedDrawable(Color.parseColor("#1E293B"), radiusDp = 10f)
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(46)).apply { marginStart = dp(6) }
            setOnClickListener {
                etManualScan.text.clear()
                layoutScanResult.visibility = View.GONE
            }
        }

        btnRow.addView(btnRunScan)
        btnRow.addView(btnClear)
        layout.addView(btnRow)

        pbScanning = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12)
            }
        }
        layout.addView(pbScanning)

        // Result Card
        layoutScanResult = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        }

        val topResultRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tvManualVerdict = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        tvManualConfidence = TextView(this).apply {
            textSize = 12f
            setTextColor(COLOR_TEXT_MUTED)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        topResultRow.addView(tvManualVerdict)
        topResultRow.addView(tvManualConfidence)
        layoutScanResult.addView(topResultRow)

        tvManualHeadline = TextView(this).apply {
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(4))
        }
        layoutScanResult.addView(tvManualHeadline)

        tvManualRec = TextView(this).apply {
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
        }
        layoutScanResult.addView(tvManualRec)

        layout.addView(layoutScanResult)
        return layout
    }

    // ==========================================
    // TAB 2: QUARANTINE HISTORY VIEW
    // ==========================================
    private fun buildHistoryTab(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val tvTitle = TextView(this).apply {
            text = "Quarantine & Detection Log"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnClear = Button(this).apply {
            text = "Clear All"
            textSize = 11f
            setTextColor(COLOR_SPAM)
            background = createRoundedDrawable(Color.parseColor("#1E293B"), radiusDp = 6f)
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(32))
            setOnClickListener {
                historyManager.clearHistory()
                refreshHistoryList()
            }
        }

        headerRow.addView(tvTitle)
        headerRow.addView(btnClear)
        layout.addView(headerRow)

        // Filter Bar (All / Spam / Safe)
        val filterBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }

        val btnFiltAll = Button(this)
        val btnFiltSpam = Button(this)
        val btnFiltSafe = Button(this)

        fun updateFiltUi(f: String) {
            currentHistoryFilter = f
            btnFiltAll.background = createRoundedDrawable(if (f == "ALL") COLOR_BLUE else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnFiltAll.setTextColor(if (f == "ALL") Color.WHITE else COLOR_TEXT_MUTED)

            btnFiltSpam.background = createRoundedDrawable(if (f == "SPAM") COLOR_SPAM else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnFiltSpam.setTextColor(if (f == "SPAM") Color.WHITE else COLOR_TEXT_MUTED)

            btnFiltSafe.background = createRoundedDrawable(if (f == "SAFE") COLOR_HAM else Color.parseColor("#1E293B"), radiusDp = 6f)
            btnFiltSafe.setTextColor(if (f == "SAFE") Color.WHITE else COLOR_TEXT_MUTED)

            refreshHistoryList()
        }

        btnFiltAll.apply {
            text = "All"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { updateFiltUi("ALL") }
        }
        btnFiltSpam.apply {
            text = "Spam Only"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { updateFiltUi("SPAM") }
        }
        btnFiltSafe.apply {
            text = "Safe Only"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
            setOnClickListener { updateFiltUi("SAFE") }
        }

        filterBar.addView(btnFiltAll)
        filterBar.addView(btnFiltSpam)
        filterBar.addView(btnFiltSafe)
        layout.addView(filterBar)
        updateFiltUi("ALL")

        tvEmptyHistory = TextView(this).apply {
            text = "No messages intercepted yet.\nIncoming SMS, WhatsApp, and Telegram messages will automatically appear here."
            setTextColor(COLOR_TEXT_SUB)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }
        layout.addView(tvEmptyHistory)

        llHistoryList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(llHistoryList)

        return layout
    }

    private fun refreshHistoryList() {
        llHistoryList.removeAllViews()
        val records = historyManager.getRecords()

        val filtered = records.filter {
            when (currentHistoryFilter) {
                "SPAM" -> it.verdict == "SPAM"
                "SAFE" -> it.verdict != "SPAM"
                else -> true
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
        } else {
            tvEmptyHistory.visibility = View.GONE
            for (rec in filtered) {
                llHistoryList.addView(buildHistoryCard(rec))
            }
        }
    }

    private fun buildHistoryCard(rec: HistoryRecord): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 10f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val isSpam = rec.verdict == "SPAM"
        val badge = TextView(this).apply {
            text = if (isSpam) "[SPAM]" else "[SAFE]"
            setTextColor(if (isSpam) COLOR_SPAM else COLOR_HAM)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }

        val sourceIcon = when (rec.source.uppercase()) {
            "WHATSAPP" -> "💬 WhatsApp"
            "TELEGRAM" -> "✈️ Telegram"
            "GMAIL" -> "✉️ Gmail"
            else -> "📱 SMS"
        }

        val tvSender = TextView(this).apply {
            text = "  $sourceIcon: ${rec.sender}"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvConf = TextView(this).apply {
            text = rec.confidence
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 10f
        }

        topRow.addView(badge)
        topRow.addView(tvSender)
        topRow.addView(tvConf)
        card.addView(topRow)

        val tvSnippet = TextView(this).apply {
            text = rec.text
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 12f
            maxLines = 2
            setPadding(0, dp(4), 0, dp(4))
        }
        card.addView(tvSnippet)

        val tvHead = TextView(this).apply {
            text = "• ${rec.headline}"
            setTextColor(if (isSpam) Color.parseColor("#FCA5A5") else Color.parseColor("#6EE7B7"))
            textSize = 10f
        }
        card.addView(tvHead)

        card.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("${rec.verdict} Alert Details")
                .setMessage("Source: ${rec.source}\nSender: ${rec.sender}\nConfidence: ${rec.confidence}\nRisk: ${rec.riskLevel}\n\nHeadline: ${rec.headline}\n\nMessage:\n${rec.text}")
                .setPositiveButton("Close", null)
                .show()
        }

        return card
    }

    // ==========================================
    // TAB 3: SETTINGS & WHITELIST VIEW
    // ==========================================
    private fun buildSettingsTab(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Section 1: Notification Access
        val secNotif = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14)
            }
        }

        val tvNotifHeader = TextView(this).apply {
            text = "💬 WhatsApp & Telegram Interception"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        val tvNotifSub = TextView(this).apply {
            text = "Allows AI to scan incoming chat notifications from WhatsApp, Telegram, and Gmail without opening them."
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            setPadding(0, dp(4), 0, dp(8))
        }

        tvNotifAccessStatus = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }

        val btnOpenNotifSettings = Button(this).apply {
            text = "Open Notification Access Settings"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = createRoundedDrawable(COLOR_BLUE, radiusDp = 8f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
                topMargin = dp(8)
            }
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Could not open notification settings", Toast.LENGTH_SHORT).show()
                }
            }
        }

        secNotif.addView(tvNotifHeader)
        secNotif.addView(tvNotifSub)
        secNotif.addView(tvNotifAccessStatus)
        secNotif.addView(btnOpenNotifSettings)
        layout.addView(secNotif)

        // Section 2: Whitelist Senders
        val secWhite = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14)
            }
        }

        val tvWhiteHeader = TextView(this).apply {
            text = "🛡️ Trusted Whitelist Senders"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        val tvWhiteSub = TextView(this).apply {
            text = "Messages from these bank codes, numbers, or contacts will never trigger spam alarms."
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            setPadding(0, dp(4), 0, dp(8))
        }

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val etNewSender = EditText(this).apply {
            hint = "e.g. HDFCBK, SBI, +9198..."
            setHintTextColor(COLOR_TEXT_SUB)
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 12f
            background = createRoundedDrawable(COLOR_BG_INPUT, radiusDp = 8f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(6) }
        }

        val btnAddSender = Button(this).apply {
            text = "+ Add"
            textSize = 12f
            setTextColor(Color.BLACK)
            background = createRoundedDrawable(COLOR_CYAN, radiusDp = 8f)
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(38))
            setOnClickListener {
                val s = etNewSender.text.toString().trim()
                if (s.isNotEmpty()) {
                    whitelistManager.addSender(s)
                    etNewSender.text.clear()
                    refreshWhitelistChips()
                }
            }
        }

        addRow.addView(etNewSender)
        addRow.addView(btnAddSender)

        llWhitelistChips = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        secWhite.addView(tvWhiteHeader)
        secWhite.addView(tvWhiteSub)
        secWhite.addView(addRow)
        secWhite.addView(llWhitelistChips)
        layout.addView(secWhite)
        refreshWhitelistChips()

        // Section 3: Server Endpoint URL
        val secServer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val tvServerHead = TextView(this).apply {
            text = "🌐 AI Backend Server URL"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvServerSub = TextView(this).apply {
            text = "Current: " + SpamApiClient.getBackendUrl(this@MainActivity)
            setTextColor(COLOR_CYAN)
            textSize = 11f
            setPadding(0, dp(4), 0, dp(8))
        }

        val btnChangeServer = Button(this).apply {
            text = "Configure Server Endpoint"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = createRoundedDrawable(Color.parseColor("#1E293B"), radiusDp = 8f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38))
            setOnClickListener { showBackendUrlDialog(tvServerSub) }
        }

        secServer.addView(tvServerHead)
        secServer.addView(tvServerSub)
        secServer.addView(btnChangeServer)
        layout.addView(secServer)

        return layout
    }

    private fun updateNotifAccessStatus() {
        val enabled = isNotificationServiceEnabled()
        if (enabled) {
            tvNotifAccessStatus.text = "Status: GRANTED (WhatsApp & Telegram are active)"
            tvNotifAccessStatus.setTextColor(COLOR_HAM)
        } else {
            tvNotifAccessStatus.text = "Status: NOT GRANTED (Tap below to activate)"
            tvNotifAccessStatus.setTextColor(COLOR_SPAM)
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun refreshWhitelistChips() {
        llWhitelistChips.removeAllViews()
        val senders = whitelistManager.getWhitelistedSenders()
        for (s in senders) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(3), dp(4), dp(3))
            }
            val tv = TextView(this).apply {
                text = "✓ $s"
                setTextColor(COLOR_TEXT_MAIN)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnDel = TextView(this).apply {
                text = "✕"
                setTextColor(COLOR_SPAM)
                textSize = 14f
                setPadding(dp(8), 0, dp(8), 0)
                setOnClickListener {
                    whitelistManager.removeSender(s)
                    refreshWhitelistChips()
                }
            }
            item.addView(tv)
            item.addView(btnDel)
            llWhitelistChips.addView(item)
        }
    }

    private fun showBackendUrlDialog(tvServerDisplay: TextView) {
        val input = EditText(this).apply {
            setText(SpamApiClient.getBackendUrl(this@MainActivity))
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("AI Backend URL")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    SpamApiClient.setBackendUrl(this, newUrl)
                    tvServerDisplay.text = "Current: $newUrl"
                    Toast.makeText(this, "Updated backend URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Reset to Cloud") { _, _ ->
                SpamApiClient.setBackendUrl(this, SpamApiClient.DEFAULT_URL)
                tvServerDisplay.text = "Current: " + SpamApiClient.DEFAULT_URL
                Toast.makeText(this, "Reset to default cloud API", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // MANUAL SCAN EXECUTION
    // ==========================================
    private fun executeManualScan() {
        val text = etManualScan.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter or paste a message to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        btnRunScan.isEnabled = false
        pbScanning.visibility = View.VISIBLE
        layoutScanResult.visibility = View.GONE

        Thread {
            val result = SpamApiClient.analyzeMessage(this, text)
            runOnUiThread {
                btnRunScan.isEnabled = true
                pbScanning.visibility = View.GONE
                displayScanResult(result)

                // Save to history
                historyManager.addRecord(
                    HistoryRecord(
                        id = System.currentTimeMillis().toString(),
                        timestamp = System.currentTimeMillis(),
                        sender = "Manual Scanner",
                        source = "Manual",
                        text = text,
                        verdict = result.verdict,
                        confidence = result.confidence,
                        headline = result.headline,
                        riskLevel = result.riskLevel
                    )
                )

                // Vibrate on spam
                if (result.verdict == "SPAM") {
                    triggerHapticFeedback()
                }
            }
        }.start()
    }

    private fun displayScanResult(result: SpamAnalysisResult) {
        layoutScanResult.visibility = View.VISIBLE
        val isSpam = result.verdict == "SPAM"

        tvManualVerdict.text = if (isSpam) "⚠️ SPAM DETECTED" else "✅ SAFE / HAM"
        tvManualVerdict.setTextColor(if (isSpam) COLOR_SPAM else COLOR_HAM)
        tvManualConfidence.text = "Confidence: ${result.confidence}"
        tvManualHeadline.text = result.headline
        tvManualRec.text = "Recommendation: ${result.recommendation}"

        layoutScanResult.background = createRoundedDrawable(
            bgColor = COLOR_BG_CARD,
            strokeColor = if (isSpam) COLOR_SPAM else COLOR_HAM,
            radiusDp = 12f
        )
    }

    private fun triggerHapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(250)
            }
        }
    }

    private fun showIncomingDetectionBanner(sender: String, source: String, text: String, verdict: String, confidence: String, headline: String) {
        val isSpam = verdict == "SPAM"
        Toast.makeText(
            this,
            "[$source Alert] $sender: $verdict ($confidence)",
            if (isSpam) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 101)
        }
    }
}

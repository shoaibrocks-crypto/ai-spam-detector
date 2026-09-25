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

    private lateinit var etManualScan: EditText
    private lateinit var btnRunScan: Button
    private lateinit var pbScanning: ProgressBar
    private lateinit var layoutScanResult: LinearLayout
    private lateinit var tvManualVerdict: TextView
    private lateinit var tvManualConfidence: TextView
    private lateinit var tvManualHeadline: TextView
    private lateinit var tvManualRec: TextView
    private lateinit var llHistoryContainer: LinearLayout
    private lateinit var tvEmptyHistory: TextView

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_NEW_DETECTION) {
                val sender = intent.getStringExtra("sender") ?: "Unknown"
                val text = intent.getStringExtra("text") ?: ""
                val verdict = intent.getStringExtra("verdict") ?: "HAM"
                val confidence = intent.getStringExtra("confidence") ?: "90.0%"
                val headline = intent.getStringExtra("headline") ?: "Analyzed"

                addDetectionCard(sender, text, verdict, confidence, headline)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildProgrammaticUI()
        checkPermissions()
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
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG_APP)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        // --- TOP HEADER ---
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvShield = TextView(this).apply {
            text = "🛡️"
            textSize = 26f
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
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvSub = TextView(this).apply {
            text = "Real-Time Message Interceptor"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 11f
        }

        headerTextLayout.addView(tvTitle)
        headerTextLayout.addView(tvSub)

        val btnSettings = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setImageResource(android.R.drawable.ic_menu_preferences)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showSettingsDialog() }
        }

        header.addView(tvShield)
        header.addView(headerTextLayout)
        header.addView(btnSettings)
        root.addView(header)

        // --- PROTECTION STATUS CARD ---
        val cardStatus = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 14f)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
            }
        }

        val pulseDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_HAM)
            }
        }

        val statusTextLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }

        val tvStatus = TextView(this).apply {
            text = "SMS Interception: ACTIVE"
            setTextColor(COLOR_HAM)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvStatusSub = TextView(this).apply {
            text = "Every incoming SMS is automatically analyzed by AI"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
        }

        statusTextLayout.addView(tvStatus)
        statusTextLayout.addView(tvStatusSub)

        val btnDefault = Button(this).apply {
            text = "DEFAULT"
            setTextColor(COLOR_CYAN)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    }
                    startActivity(intent)
                }
            }
        }

        cardStatus.addView(pulseDot)
        cardStatus.addView(statusTextLayout)
        cardStatus.addView(btnDefault)
        root.addView(cardStatus)

        // --- MANUAL SCANNER CARD ---
        val cardScanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 14f)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
            }
        }

        val tvScannerTitle = TextView(this).apply {
            text = "🔍 Manual Message Scanner"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        etManualScan = EditText(this).apply {
            hint = "Paste any SMS, WhatsApp text, or email here..."
            setHintTextColor(COLOR_TEXT_SUB)
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 13f
            background = createRoundedDrawable(COLOR_BG_INPUT, strokeColor = COLOR_BORDER, radiusDp = 8f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            gravity = Gravity.TOP or Gravity.START
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(85)).apply {
                topMargin = dp(10)
            }
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }

        val btnPaste = Button(this).apply {
            text = "PASTE DEMO"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                etManualScan.setText("URGENT: Your Chase Bank account has been locked due to suspicious activity. Verify immediately at http://chase-auth.xyz or account will be closed.")
            }
        }

        btnRunScan = Button(this).apply {
            text = "SCAN WITH AI"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createRoundedDrawable(COLOR_BLUE, radiusDp = 8f)
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(8)
            }
            setOnClickListener { runManualScan() }
        }

        btnRow.addView(btnPaste)
        btnRow.addView(btnRunScan)

        pbScanning = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            }
        }

        // Result box
        layoutScanResult = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_INPUT, strokeColor = COLOR_BORDER, radiusDp = 8f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }

        val resultHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tvManualVerdict = TextView(this).apply {
            text = "[SPAM]"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = createRoundedDrawable(COLOR_SPAM, strokeColor = Color.TRANSPARENT, radiusDp = 4f)
        }

        tvManualConfidence = TextView(this).apply {
            text = "99.0% Confidence"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(10)
            }
        }

        resultHeader.addView(tvManualVerdict)
        resultHeader.addView(tvManualConfidence)

        tvManualHeadline = TextView(this).apply {
            setTextColor(COLOR_CYAN)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            }
        }

        tvManualRec = TextView(this).apply {
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            }
        }

        layoutScanResult.addView(resultHeader)
        layoutScanResult.addView(tvManualHeadline)
        layoutScanResult.addView(tvManualRec)

        cardScanner.addView(tvScannerTitle)
        cardScanner.addView(etManualScan)
        cardScanner.addView(btnRow)
        cardScanner.addView(pbScanning)
        cardScanner.addView(layoutScanResult)
        root.addView(cardScanner)

        // --- HISTORY HEADER ---
        val tvHistoryTitle = TextView(this).apply {
            text = "📥 Live Intercepted SMS Log"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(18)
                bottomMargin = dp(8)
            }
        }
        root.addView(tvHistoryTitle)

        // Empty placeholder
        tvEmptyHistory = TextView(this).apply {
            text = "No incoming messages intercepted yet.\nSend an SMS to this phone to see real-time AI detection in action!"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 12f
            gravity = Gravity.CENTER
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 12f)
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        root.addView(tvEmptyHistory)

        // History items container
        llHistoryContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(llHistoryContainer)

        scrollView.addView(root)
        setContentView(scrollView)
    }

    private fun runManualScan() {
        val text = etManualScan.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a message to scan", Toast.LENGTH_SHORT).show()
            return
        }

        pbScanning.visibility = View.VISIBLE
        layoutScanResult.visibility = View.GONE
        btnRunScan.isEnabled = false

        Thread {
            val result = SpamApiClient.analyzeMessage(this@MainActivity, text)
            runOnUiThread {
                pbScanning.visibility = View.GONE
                btnRunScan.isEnabled = true
                layoutScanResult.visibility = View.VISIBLE

                tvManualVerdict.text = "[${result.verdict}]"
                tvManualVerdict.background = createRoundedDrawable(
                    if (result.verdict == "SPAM") COLOR_SPAM else COLOR_HAM,
                    strokeColor = Color.TRANSPARENT,
                    radiusDp = 4f
                )

                tvManualConfidence.text = "${result.confidence} Confidence • ${result.riskLevel}"
                tvManualHeadline.text = result.headline
                tvManualRec.text = "Recommendation: ${result.recommendation}"
            }
        }.start()
    }

    private fun addDetectionCard(sender: String, text: String, verdict: String, confidence: String, headline: String) {
        tvEmptyHistory.visibility = View.GONE
        llHistoryContainer.visibility = View.VISIBLE

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG_CARD, radiusDp = 10f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
        }

        val cardHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvSender = TextView(this).apply {
            this.text = "From: $sender"
            setTextColor(COLOR_TEXT_MAIN)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvBadge = TextView(this).apply {
            this.text = "[$verdict]"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = createRoundedDrawable(
                if (verdict == "SPAM") COLOR_SPAM else COLOR_HAM,
                strokeColor = Color.TRANSPARENT,
                radiusDp = 4f
            )
        }

        cardHeader.addView(tvSender)
        cardHeader.addView(tvBadge)

        val tvHead = TextView(this).apply {
            this.text = headline
            setTextColor(COLOR_CYAN)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            }
        }

        val tvSnippet = TextView(this).apply {
            this.text = text
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 12f
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            }
        }

        val tvConf = TextView(this).apply {
            this.text = "Confidence: $confidence"
            setTextColor(COLOR_TEXT_SUB)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            }
        }

        card.addView(cardHeader)
        card.addView(tvHead)
        card.addView(tvSnippet)
        card.addView(tvConf)

        llHistoryContainer.addView(card, 0)
    }

    private fun showSettingsDialog() {
        val currentUrl = SpamApiClient.getBackendUrl(this)
        val input = EditText(this).apply {
            setText(currentUrl)
            setSingleLine()
            setTextColor(Color.BLACK)
        }

        AlertDialog.Builder(this)
            .setTitle("AI Backend URL")
            .setMessage("Enter your Render cloud link (e.g. https://your-service.onrender.com):")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    SpamApiClient.setBackendUrl(this, newUrl)
                    Toast.makeText(this, "Backend URL updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }

        if (needed.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(needed.toTypedArray(), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SmsReceiver.ACTION_NEW_DETECTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(updateReceiver)
        } catch (_: Exception) {}
    }
}

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
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private lateinit var etManualScan: EditText
    private lateinit var btnRunScan: Button
    private lateinit var btnPasteTest: Button
    private lateinit var pbScanning: ProgressBar
    private lateinit var layoutScanResult: LinearLayout
    private lateinit var tvManualVerdict: TextView
    private lateinit var tvManualConfidence: TextView
    private lateinit var tvManualHeadline: TextView
    private lateinit var tvManualRec: TextView
    private lateinit var llHistoryContainer: LinearLayout
    private lateinit var tvEmptyHistory: TextView
    private lateinit var btnDefaultApp: Button
    private lateinit var btnSettings: ImageButton

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
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupListeners()
    }

    private fun initViews() {
        etManualScan = findViewById(R.id.etManualScan)
        btnRunScan = findViewById(R.id.btnRunScan)
        btnPasteTest = findViewById(R.id.btnPasteTest)
        pbScanning = findViewById(R.id.pbScanning)
        layoutScanResult = findViewById(R.id.layoutScanResult)
        tvManualVerdict = findViewById(R.id.tvManualVerdict)
        tvManualConfidence = findViewById(R.id.tvManualConfidence)
        tvManualHeadline = findViewById(R.id.tvManualHeadline)
        tvManualRec = findViewById(R.id.tvManualRec)
        llHistoryContainer = findViewById(R.id.llHistoryContainer)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        btnDefaultApp = findViewById(R.id.btnDefaultApp)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupListeners() {
        // Paste Demo Button
        btnPasteTest.setOnClickListener {
            etManualScan.setText("URGENT: Your Chase Bank account has been locked due to suspicious activity. Verify immediately at http://chase-auth.xyz or account will be closed.")
        }

        // Run Manual Scan
        btnRunScan.setOnClickListener {
            val text = etManualScan.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter a message to scan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbScanning.visibility = View.VISIBLE
            layoutScanResult.visibility = View.GONE
            btnRunScan.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val result = SpamApiClient.analyzeMessage(this@MainActivity, text)
                pbScanning.visibility = View.GONE
                btnRunScan.isEnabled = true
                layoutScanResult.visibility = View.VISIBLE

                tvManualVerdict.text = "[${result.verdict}]"
                if (result.verdict == "SPAM") {
                    tvManualVerdict.setBackgroundColor(Color.parseColor("#EF4444"))
                } else {
                    tvManualVerdict.setBackgroundColor(Color.parseColor("#10B981"))
                }

                tvManualConfidence.text = "${result.confidence} Confidence • ${result.riskLevel}"
                tvManualHeadline.text = result.headline
                tvManualRec.text = "Recommendation: ${result.recommendation}"
            }
        }

        // Set as Default SMS App Button
        btnDefaultApp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }
                startActivity(intent)
            }
        }

        // Settings (Configure Render URL)
        btnSettings.setOnClickListener {
            val currentUrl = SpamApiClient.getBackendUrl(this)
            val input = EditText(this).apply {
                setText(currentUrl)
                setSingleLine()
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
    }

    private fun addDetectionCard(sender: String, text: String, verdict: String, confidence: String, headline: String) {
        tvEmptyHistory.visibility = View.GONE
        llHistoryContainer.visibility = View.VISIBLE

        val cardView = LayoutInflater.from(this).inflate(R.layout.item_detection_log, llHistoryContainer, false)
        val tvSender = cardView.findViewById<TextView>(R.id.tvSender)
        val tvVerdictBadge = cardView.findViewById<TextView>(R.id.tvVerdictBadge)
        val tvHeadline = cardView.findViewById<TextView>(R.id.tvHeadline)
        val tvMessageSnippet = cardView.findViewById<TextView>(R.id.tvMessageSnippet)
        val tvConfidence = cardView.findViewById<TextView>(R.id.tvConfidence)

        tvSender.text = "From: $sender"
        tvVerdictBadge.text = "[$verdict]"
        if (verdict == "SPAM") {
            tvVerdictBadge.setBackgroundColor(Color.parseColor("#EF4444"))
        } else {
            tvVerdictBadge.setBackgroundColor(Color.parseColor("#10B981"))
        }

        tvHeadline.text = headline
        tvMessageSnippet.text = text
        tvConfidence.text = "Confidence: $confidence"

        llHistoryContainer.addView(cardView, 0)
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

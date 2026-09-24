package com.antigravity.aispamdetector

import com.antigravity.aispamdetector.R
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etManualScan: EditText
    private lateinit var btnRunScan: Button
    private lateinit var btnPasteTest: Button
    private lateinit var pbScanning: ProgressBar
    private lateinit var layoutScanResult: LinearLayout
    private lateinit var tvManualVerdict: TextView
    private lateinit var tvManualConfidence: TextView
    private lateinit var tvManualHeadline: TextView
    private lateinit var tvManualRec: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyHistory: TextView
    private lateinit var btnDefaultApp: Button
    private lateinit var btnSettings: ImageButton

    private val detectionList = mutableListOf<DetectionItem>()
    private lateinit var adapter: DetectionAdapter

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_NEW_DETECTION) {
                val sender = intent.getStringExtra("sender") ?: "Unknown"
                val text = intent.getStringExtra("text") ?: ""
                val verdict = intent.getStringExtra("verdict") ?: "HAM"
                val confidence = intent.getStringExtra("confidence") ?: "90.0%"
                val headline = intent.getStringExtra("headline") ?: "Analyzed"

                val item = DetectionItem(sender, text, verdict, confidence, headline)
                adapter.addItem(item)
                tvEmptyHistory.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
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
        rvHistory = findViewById(R.id.rvHistory)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        btnDefaultApp = findViewById(R.id.btnDefaultApp)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupRecyclerView() {
        adapter = DetectionAdapter(detectionList)
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter
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

            lifecycleScope.launch {
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

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
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

package com.antigravity.aispamdetector

import com.antigravity.aispamdetector.R
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DetectionItem(
    val sender: String,
    val text: String,
    val verdict: String,
    val confidence: String,
    val headline: String
)

class DetectionAdapter(private val items: MutableList<DetectionItem>) :
    RecyclerView.Adapter<DetectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvVerdictBadge: TextView = view.findViewById(R.id.tvVerdictBadge)
        val tvHeadline: TextView = view.findViewById(R.id.tvHeadline)
        val tvMessageSnippet: TextView = view.findViewById(R.id.tvMessageSnippet)
        val tvConfidence: TextView = view.findViewById(R.id.tvConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvSender.text = "From: ${item.sender}"
        holder.tvVerdictBadge.text = "[${item.verdict}]"
        
        if (item.verdict == "SPAM") {
            holder.tvVerdictBadge.setBackgroundColor(Color.parseColor("#EF4444"))
        } else {
            holder.tvVerdictBadge.setBackgroundColor(Color.parseColor("#10B981"))
        }

        holder.tvHeadline.text = item.headline
        holder.tvMessageSnippet.text = item.text
        holder.tvConfidence.text = "Confidence: ${item.confidence}"
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: DetectionItem) {
        items.add(0, item)
        notifyItemInserted(0)
    }
}

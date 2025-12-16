package com.example.dropzone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dropzone.R
import com.example.dropzone.models.AIMatchResponse

class AIMatchAdapter(
    private val matches: List<AIMatchResponse>,
    private val onItemClick: (AIMatchResponse) -> Unit
) : RecyclerView.Adapter<AIMatchAdapter.MatchViewHolder>() {

    class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val desc: TextView = view.findViewById(R.id.matchDescription)
        val score: TextView = view.findViewById(R.id.matchScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val item = matches[position]

        holder.desc.text = item.item
        holder.score.text = "Match: ${item.match_percent}%"


        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = matches.size
}

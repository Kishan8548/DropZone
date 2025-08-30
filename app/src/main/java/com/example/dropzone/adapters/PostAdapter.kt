package com.example.dropzone.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dropzone.R
import com.example.dropzone.databinding.ItemPostBinding
import com.example.dropzone.models.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostAdapter(
    private var posts: List<Post>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(post: Post)
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(posts[position])
                }
            }
        }

        fun bind(post: Post) {
            binding.itemPostTitle.text = post.title
            binding.itemPostShortDescription.text = post.description

            binding.itemPostStatus.text = post.status
            if (post.status == "Lost") {
                binding.itemPostStatus.setBackgroundResource(R.drawable.status_lost_background)
            } else {
                binding.itemPostStatus.setBackgroundResource(R.drawable.status_found_background)
            }

            post.timestamp?.let {
                binding.itemPostTime.text = formatTimestamp(it)
            } ?: run {
                binding.itemPostTime.text = ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Date): String {
        val now = Date()
        val diff = now.time - timestamp.time
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestamp)
        }
    }
}

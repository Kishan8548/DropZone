package com.example.dropzone.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dropzone.R
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

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val itemPostImage: ImageView = itemView.findViewById(R.id.itemPostImage)
        val itemPostTitle: TextView = itemView.findViewById(R.id.itemPostTitle)
        val itemPostShortDescription: TextView = itemView.findViewById(R.id.itemPostShortDescription)
        val itemPostStatus: TextView = itemView.findViewById(R.id.itemPostStatus)
        val itemPostTime: TextView = itemView.findViewById(R.id.itemPostTime)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(posts[position])
                }
            }
        }

        fun bind(post: Post) {
            itemPostTitle.text = post.title
            itemPostShortDescription.text = post.description

            itemPostStatus.text = post.status
            if (post.status == "Lost") {
                itemPostStatus.setBackgroundResource(R.drawable.status_lost_background)
            } else {
                itemPostStatus.setBackgroundResource(R.drawable.status_found_background)
            }

            if (!post.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(post.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
//                    .into(itemPostImage)
            } else {
//                itemPostImage.setImageResource(R.drawable.ic_image_placeholder)
            }

            post.timestamp?.let {
                itemPostTime.text = formatTimestamp(it)
            } ?: run {
                itemPostTime.text = ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
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
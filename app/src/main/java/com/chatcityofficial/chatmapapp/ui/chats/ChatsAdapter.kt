package com.chatcityofficial.chatmapapp.ui.chats

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.databinding.ItemChatModernBinding
import kotlinx.coroutines.*

class ChatsAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private var updateJob: Job? = null
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        startTimestampUpdates()
    }
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        stopTimestampUpdates()
    }
    
    private fun startTimestampUpdates() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(60000) // Update every minute
                notifyDataSetChanged() // Refresh all visible items
            }
        }
    }
    
    private fun stopTimestampUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: ItemChatModernBinding,
        private val onChatClick: (Chat) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.tvUsername.text = chat.name
            binding.tvLastMessage.text = chat.lastMessage
            
            // Format time
            binding.tvTime.text = formatTimeAgo(chat.lastMessageTime)
            
            // Set click listener
            binding.root.setOnClickListener {
                onChatClick(chat)
            }
        }
        
        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < DateUtils.MINUTE_IN_MILLIS -> "now"
                diff < DateUtils.HOUR_IN_MILLIS -> {
                    val minutes = (diff / DateUtils.MINUTE_IN_MILLIS).toInt()
                    if (minutes == 1) "1m" else "${minutes}m"
                }
                diff < DateUtils.DAY_IN_MILLIS -> {
                    val hours = (diff / DateUtils.HOUR_IN_MILLIS).toInt()
                    if (hours == 1) "1h" else "${hours}h"
                }
                diff < DateUtils.WEEK_IN_MILLIS -> {
                    val days = (diff / DateUtils.DAY_IN_MILLIS).toInt()
                    if (days == 1) "1d" else "${days}d"
                }
                diff < 4 * DateUtils.WEEK_IN_MILLIS -> {
                    val weeks = (diff / DateUtils.WEEK_IN_MILLIS).toInt()
                    if (weeks == 1) "1w" else "${weeks}w"
                }
                else -> {
                    val months = (diff / (30 * DateUtils.DAY_IN_MILLIS)).toInt()
                    if (months == 1) "1mo" else "${months}mo"
                }
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}
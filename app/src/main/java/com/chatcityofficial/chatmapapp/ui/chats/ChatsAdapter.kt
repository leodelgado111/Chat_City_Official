package com.chatcityofficial.chatmapapp.ui.chats

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.databinding.ItemChatBinding

class ChatsAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
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
        private val binding: ItemChatBinding,
        private val onChatClick: (Chat) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.nameText.text = chat.name
            binding.lastMessageText.text = chat.lastMessage
            
            // Format time
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                chat.lastMessageTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            binding.timeTextView.text = formatTimeAgo(chat.lastMessageTime)
            
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
                    "${minutes}m"
                }
                diff < DateUtils.DAY_IN_MILLIS -> {
                    val hours = (diff / DateUtils.HOUR_IN_MILLIS).toInt()
                    "${hours}h"
                }
                diff < DateUtils.WEEK_IN_MILLIS -> {
                    val days = (diff / DateUtils.DAY_IN_MILLIS).toInt()
                    "${days}d"
                }
                else -> {
                    val weeks = (diff / DateUtils.WEEK_IN_MILLIS).toInt()
                    "${weeks}w"
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
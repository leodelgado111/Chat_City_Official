package com.chatcityofficial.chatmapapp.ui.chats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.data.User

class UsersAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], onUserClick)
    }
    
    override fun getItemCount() = users.size
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userStatus: TextView = itemView.findViewById(R.id.user_status)
        private val onlineIndicator: View = itemView.findViewById(R.id.online_indicator)
        
        fun bind(user: User, onUserClick: (User) -> Unit) {
            userName.text = user.name
            userStatus.text = if (user.isOnline) "Online" else "Offline"
            onlineIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            
            // Load profile image
            if (user.profileImage.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profileImage)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
            
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}
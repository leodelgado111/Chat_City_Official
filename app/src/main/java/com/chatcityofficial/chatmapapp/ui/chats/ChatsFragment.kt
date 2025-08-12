package com.chatcityofficial.chatmapapp.ui.chats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.data.User
import com.chatcityofficial.chatmapapp.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var usersAdapter: UsersAdapter
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val users = mutableListOf<User>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_chats, container, false)
        
        recyclerView = root.findViewById(R.id.chats_recycler_view)
        emptyView = root.findViewById(R.id.empty_view)
        
        setupRecyclerView()
        loadUsers()
        
        return root
    }
    
    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(users) { user ->
            // Open chat with selected user
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("RECIPIENT_ID", user.id)
                putExtra("RECIPIENT_NAME", user.name)
            }
            startActivity(intent)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = usersAdapter
        }
    }
    
    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                users.clear()
                
                for (document in documents) {
                    // Skip current user
                    if (document.id == currentUserId) continue
                    
                    val user = User(
                        id = document.id,
                        name = document.getString("name") ?: "Unknown User",
                        email = document.getString("email") ?: "",
                        profileImage = document.getString("profileImage") ?: "",
                        isOnline = document.getBoolean("isOnline") ?: false
                    )
                    users.add(user)
                }
                
                usersAdapter.notifyDataSetChanged()
                
                // Show/hide empty view
                if (users.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
                emptyView.text = "Error loading users: ${exception.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }
}
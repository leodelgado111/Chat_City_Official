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
        
        // Add demo user for testing
        addDemoUser()
        
        // Then load real users from Firebase
        loadUsers()
        
        return root
    }
    
    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(users) { user ->
            // Open chat with selected user
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("RECIPIENT_ID", user.id)
                putExtra("RECIPIENT_NAME", user.name)
                putExtra("IS_DEMO", user.id == "demo_user_123") // Flag for demo user
            }
            startActivity(intent)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = usersAdapter
        }
    }
    
    private fun addDemoUser() {
        // Add a demo user that's always available
        val demoUser = User(
            id = "demo_user_123",
            name = "Chat City Assistant",
            email = "assistant@chatcity.com",
            profileImage = "",
            isOnline = true
        )
        
        // Add to the beginning of the list
        users.add(0, demoUser)
        usersAdapter.notifyDataSetChanged()
        
        // Hide empty view since we have at least one user
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
    
    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                // Don't clear the list completely - keep the demo user
                val tempUsers = mutableListOf<User>()
                
                // Keep the demo user if it exists
                if (users.isNotEmpty() && users[0].id == "demo_user_123") {
                    tempUsers.add(users[0])
                }
                
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
                    tempUsers.add(user)
                }
                
                users.clear()
                users.addAll(tempUsers)
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
                // Handle error but keep demo user visible
                if (users.isEmpty() || (users.size == 1 && users[0].id == "demo_user_123")) {
                    // We still have the demo user, so don't show error
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                } else {
                    emptyView.text = "Error loading users: ${exception.message}"
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            }
    }
}
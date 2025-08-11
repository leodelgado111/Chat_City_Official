package com.chatcityofficial.chatmapapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R

class ChatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_chats, container, false)
        val textView: TextView = root.findViewById(R.id.text_chats)
        textView.text = "This is chats fragment"
        return root
    }
}
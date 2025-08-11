package com.chatcityofficial.chatmapapp.ui.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R

class SavedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_saved, container, false)
        val textView: TextView = root.findViewById(R.id.text_saved)
        textView.text = "This is saved fragment"
        return root
    }
}
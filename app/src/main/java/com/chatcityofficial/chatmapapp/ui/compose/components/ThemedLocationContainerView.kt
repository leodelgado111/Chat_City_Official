package com.chatcityofficial.chatmapapp.ui.compose.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class ThemedLocationContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var locationTextState by mutableStateOf("Locating...")
    private var onClickListener: (() -> Unit)? = null

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun setLocationText(text: String) {
        locationTextState = text
    }

    fun setOnLocationClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    @Composable
    override fun Content() {
        ChatCityTheme {
            ThemedLocationContainer(
                locationText = locationTextState,
                onClick = { onClickListener?.invoke() },
                modifier = Modifier
            )
        }
    }
}
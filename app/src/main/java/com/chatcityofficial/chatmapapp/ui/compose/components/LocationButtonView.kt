package com.chatcityofficial.chatmapapp.ui.compose.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class LocationButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var onClickListener: (() -> Unit)? = null

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun setOnLocationClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    @Composable
    override fun Content() {
        ChatCityTheme {
            LocationButton(
                onClick = { onClickListener?.invoke() }
            )
        }
    }
}
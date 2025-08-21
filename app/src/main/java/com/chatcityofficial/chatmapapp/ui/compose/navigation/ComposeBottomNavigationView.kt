package com.chatcityofficial.chatmapapp.ui.compose.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.*
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class ComposeBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var _selectedTab by mutableStateOf(NavigationTab.HOME)
    private var onTabSelectedListener: ((NavigationTab) -> Unit)? = null

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun setSelectedTab(tab: NavigationTab) {
        _selectedTab = tab
    }

    fun setOnTabSelectedListener(listener: (NavigationTab) -> Unit) {
        onTabSelectedListener = listener
    }

    @Composable
    override fun Content() {
        ChatCityTheme {
            BottomNavigationBar(
                selectedTab = _selectedTab,
                onTabSelected = { tab ->
                    _selectedTab = tab
                    onTabSelectedListener?.invoke(tab)
                }
            )
        }
    }
}
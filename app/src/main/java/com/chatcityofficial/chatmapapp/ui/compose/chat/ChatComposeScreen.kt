package com.chatcityofficial.chatmapapp.ui.compose.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.data.models.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatComposeScreen(
    chatId: String,
    chatName: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId = viewModel.currentUserId
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    
    // State to track if text field should be focused
    var isTextFieldFocused by remember { mutableStateOf(false) }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var wasKeyboardVisible by remember { mutableStateOf(false) }
    
    // State to track keyboard height
    var keyboardHeight by remember { mutableStateOf(0) }
    
    // Track keyboard visibility and manage focus state
    DisposableEffect(view) {
        val listener = ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val keyboardShowing = imeHeight > 0
            
            // Update keyboard height
            keyboardHeight = imeHeight
            
            // Detect when keyboard closes
            if (!keyboardShowing && isTextFieldFocused) {
                // Keyboard closed, remove TextField completely
                isTextFieldFocused = false
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
            
            wasKeyboardVisible = keyboardShowing
            isKeyboardVisible = keyboardShowing
            
            ViewCompat.onApplyWindowInsets(v, insets)
        }
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }
    
    // Clear focus when keyboard height becomes 0
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight == 0 && isTextFieldFocused) {
            isTextFieldFocused = false
            focusManager.clearFocus(force = true)
        }
    }
    
    // When user requests focus, show keyboard
    LaunchedEffect(isTextFieldFocused) {
        if (isTextFieldFocused && !isKeyboardVisible) {
            keyboardController?.show()
        }
    }
    
    // Also handle back button press when keyboard is visible as backup
    BackHandler(enabled = isKeyboardVisible && isTextFieldFocused) {
        // Clear focus and hide keyboard when back is pressed
        isTextFieldFocused = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    
    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Clear focus and hide keyboard when clicking outside
                isTextFieldFocused = false
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        // Gradient background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0x5297D4F0),
                            Color(0x528D93D0),
                            Color(0x52FB86BB),
                            Color(0x5297D4F0)
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()  // Add padding for keyboard
        ) {
            // Top Bar
            ChatTopBar(
                chatName = chatName,
                onBackClick = onBackClick,
                onArchiveClick = { /* TODO: Implement archive */ }
            )
            
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId
                    )
                }
            }
            
            // Message Input
            MessageInput(
                messageText = messageText,
                onMessageChange = viewModel::updateMessageText,
                isTextFieldFocused = isTextFieldFocused,
                isKeyboardVisible = isKeyboardVisible,
                onFocusChange = { focused ->
                    isTextFieldFocused = focused
                },
                onSendClick = {
                    viewModel.sendMessage(chatId)
                    // Clear focus after sending
                    isTextFieldFocused = false
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    chatName: String,
    onBackClick: () -> Unit,
    onArchiveClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = chatName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_arrow),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onArchiveClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_archive),
                    contentDescription = "Archive",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isCurrentUser) Color(0xFFFB86BB) else Color(0xFF2A2A2A)
    val textColor = if (isCurrentUser) Color.Black else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 48.dp else 0.dp,
                end = if (isCurrentUser) 0.dp else 48.dp
            ),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderName ?: "Unknown",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
            
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bubbleColor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    isTextFieldFocused: Boolean,
    isKeyboardVisible: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onSendClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // Clear focus when isTextFieldFocused becomes false
    LaunchedEffect(isTextFieldFocused) {
        if (!isTextFieldFocused) {
            try {
                focusRequester.freeFocus()
            } catch (e: Exception) {
                // Ignore if focus requester is not attached
            }
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo button
        IconButton(
            onClick = {
                // TODO: Implement photo picker
                // For now, just a placeholder action
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_photo),
                contentDescription = "Add Photo",
                tint = Color(0xFFFB86BB),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Show TextField when focused or has text (even if keyboard not yet visible)
        if (isTextFieldFocused || messageText.isNotEmpty()) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("Type a message...", color = Color.Gray)
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color(0xFFFB86BB),
                    unfocusedTextColor = Color(0xFFFB86BB),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFFFB86BB)
                ),
                singleLine = true
            )
            
            // Auto-focus when TextField appears
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            // Show clickable placeholder when keyboard is hidden
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable {
                        // Set focus state to true, which will trigger TextField to appear
                        onFocusChange(true)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Type a message...",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = onSendClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF97D4F0),
                            Color(0xFF8D93D0),
                            Color(0xFFFB86BB)
                        )
                    )
                ),
            enabled = messageText.isNotBlank()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = "Send",
                tint = if (messageText.isNotBlank()) Color.White else Color.Gray
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun ChatComposeScreenPreview() {
    ChatComposeScreen(
        chatId = "preview",
        chatName = "Chat Preview",
        onBackClick = {}
    )
}
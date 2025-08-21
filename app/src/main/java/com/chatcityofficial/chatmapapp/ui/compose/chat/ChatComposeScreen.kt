package com.chatcityofficial.chatmapapp.ui.compose.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.data.models.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
                onSendClick = {
                    viewModel.sendMessage(chatId)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            placeholder = {
                Text("Type a message...", color = Color.Gray)
            },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFF1A1A1A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
        
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
package com.chatcityofficial.chatmapapp.ui.compose.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcityofficial.chatmapapp.R
import java.util.concurrent.TimeUnit

// Data class for chat bubble view activities
data class ChatBubbleActivity(
    val message: String,
    val category: String? = null,
    val subcategory: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Helper function to format time ago
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "${minutes}m"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "${hours}h"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "${days}d"
        }
        else -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            "${weeks}w"
        }
    }
}

@Composable
fun ProfileComposeScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    recentActivities: List<ChatBubbleActivity> = emptyList()
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Display the full profile screen vector drawable
        Image(
            painter = painterResource(id = R.drawable.profile_main_screen_vector),
            contentDescription = "Profile Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Add gradient container 50dp below the Profile title
        // Profile title is at y=83dp (baseline at 96dp), so container at ~146dp
        // Width and height increased by 5% (315dp to 331dp, 150dp to 158dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 126.dp) // Moved up by 20dp total from 146dp
                .size(width = 331.dp, height = 158.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            // Base white layer with 30% opacity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
            
            // Sweep gradient overlay with further reduced color intensity (now 32% instead of 42%)
            // Colors adjusted: more blue and purple, less pink
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF6699FF).copy(alpha = 0.336f), // Blue (from top) - brightness increased by 5%
                                Color(0xFF8877DD).copy(alpha = 0.336f), // Blue-Purple blend
                                Color(0xFFB366D9).copy(alpha = 0.336f), // Purple (from top-right)
                                Color(0xFFCC88DD).copy(alpha = 0.336f), // Purple-Pink blend (reduced pink)
                                Color(0xFFDD99CC).copy(alpha = 0.336f), // Subtle pink
                                Color(0xFF7788EE).copy(alpha = 0.336f), // Pink-Blue blend
                                Color(0xFF6699FF).copy(alpha = 0.336f)  // Back to blue
                            ),
                            // Center moved 10% further away (negative values to move outside)
                            center = Offset(-0.1f, -0.1f) // Further from container
                        )
                    )
            )
            
            // Text overlay centered in the container
            Image(
                painter = painterResource(id = R.drawable.profile_container_textoverlay_vector),
                contentDescription = "Profile Info",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 269.dp, height = 99.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        // Activity container 20dp below profile container
        // Profile container ends at y=146+158=304dp, so activity container at 324dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 304.dp) // Moved up by 20dp total from 324dp
                .size(width = 331.dp, height = 320.dp) // Width matched to profile container
                .clip(RoundedCornerShape(30.dp))
        ) {
            // Base white layer with 30% opacity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
            
            // Same gradient as profile container with colors rotated for pink in top left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                // Reordered to place pink in top-left quadrant - brightness increased by 5%
                                Color(0xFF7788EE).copy(alpha = 0.336f), // Blue-Pink blend (32% + 5% = 33.6%)
                                Color(0xFF6699FF).copy(alpha = 0.336f), // Blue (bottom)
                                Color(0xFF8877DD).copy(alpha = 0.336f), // Blue-Purple blend
                                Color(0xFFB366D9).copy(alpha = 0.336f), // Purple (left side)
                                Color(0xFFCC88DD).copy(alpha = 0.336f), // Purple-Pink blend
                                Color(0xFFDD99CC).copy(alpha = 0.336f), // Pink (top-left quadrant)
                                Color(0xFFDD99CC).copy(alpha = 0.336f), // Pink continues
                                Color(0xFF7788EE).copy(alpha = 0.336f)  // Back to Blue-Pink blend
                            ),
                            center = Offset(0.5f, 0.5f) // Center in middle
                        )
                    )
            )
            
            // Activity title in top left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 17.dp), // Moved down by 1dp from 16dp
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Activity icon (list icon)
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                    contentDescription = "Activity",
                    modifier = Modifier.size(24.dp), // Increased by 20% from 20dp
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(9.dp)) // Reduced from 10dp to move text 1dp closer
                Text(
                    text = "Activity log",
                    fontSize = 15.84.sp, // Increased by 20% from 13.2sp
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
            
            // Activity feed overlay - displays recent chat bubble views
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(top = 27.dp) // Extra padding to account for title, increased by 3dp
                    .background(Color.Transparent), // Ensure transparency
                verticalArrangement = Arrangement.Top
            ) {
                // If no activities, show placeholder
                if (recentActivities.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No recent activity",
                                fontSize = 13.65.sp,  // Increased by 5% from 13sp
                                fontWeight = FontWeight.Normal,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "View chat bubbles on the map to see them here",
                                fontSize = 11.55.sp,  // Increased by 5% from 11sp
                                fontWeight = FontWeight.Normal,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Display activities
                    items(recentActivities) { activity ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp) // Reduced from 12.dp
                        ) {
                            // Combined "You viewed" with message and time on right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("You viewed: ")
                                        }
                                        append(activity.message)
                                    },
                                    fontSize = 11.55.sp,  // Increased by 5% from 11sp
                                    color = Color.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatTimeAgo(activity.timestamp),
                                    fontSize = 10.5.sp,  // Increased by 5% from 10sp
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                            }
                            
                            // Category tags if available
                            if (activity.category != null) {
                                Spacer(modifier = Modifier.height(4.dp)) // Reduced from 8.dp
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activity.category,
                                        fontSize = 10.5.sp,  // Increased by 5% from 10sp
                                        fontWeight = FontWeight.Bold, // Made bold
                                        color = Color.Black
                                    )
                                    if (activity.subcategory != null) {
                                        Text(
                                            text = " • ",
                                            fontSize = 10.5.sp,  // Increased by 5% from 10sp
                                            color = Color.Black
                                        )
                                        Text(
                                            text = activity.subcategory,
                                            fontSize = 10.5.sp,  // Increased by 5% from 10sp
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            
                            // Separator line
                            Spacer(modifier = Modifier.height(6.dp)) // Reduced from 12.dp
                            Divider(
                                color = Color.Black,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
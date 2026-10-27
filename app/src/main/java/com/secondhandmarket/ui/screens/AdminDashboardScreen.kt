package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secondhandmarket.data.models.NotificationItem
import com.secondhandmarket.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by adminViewModel.notifications.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // Background Image with Overlay
        AsyncImage(
            model = "file:///android_asset/images/backgrounds/admin_bg.jpg", // Placeholder
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Admin Dashboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recent Activity & Notifications", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (notifications.isEmpty()) {
                        Text("No new notifications.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(top = 8.dp)) {
                            items(notifications) { notification ->
                                NotificationRow(notification = notification)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Navigate to User Management */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Manage Users & Vendors")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { /* Navigate to Product Moderation */ },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Moderate Listings")
            }
        }
    }
}

@Composable
fun NotificationRow(notification: NotificationItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = when(notification.type) {
                "PAYMENT_UPDATE" -> Color.Green.copy(alpha = 0.2f)
                "NEW_VENDOR" -> Color.Blue.copy(alpha = 0.2f)
                else -> Color.Gray.copy(alpha = 0.2f)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = notification.icon ?: "!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = notification.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.DarkGray)
            Text(text = notification.message, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
            Text(text = notification.timestamp.toString(), fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// Helper import for AsyncImage if not globally imported in this file context
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import coil.compose.AsyncImage
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.viewmodel.VendorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDashboardScreen(
    vendorViewModel: VendorViewModel,
    onUploadProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by vendorViewModel.orders.collectAsState()
    var showUploadDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Image with Overlay
            AsyncImage(
                model = "file:///android_asset/images/backgrounds/vendor_bg.jpg",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.1f
            )
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Orders",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage your sales and update payment statuses below.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Orders List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order = order, onUpdateStatus = { newStatus ->
                            vendorViewModel.updateOrderPaymentStatus(order.id, newStatus)
                        })
                    }
                }
            }

            if (showUploadDialog) {
                UploadProductDialog(
                    onDismiss = { showUploadDialog = false },
                    onSubmit = { title, desc, price, category ->
                        vendorViewModel.uploadProduct(title, desc, price, category)
                        showUploadDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, onUpdateStatus: (PaymentStatus) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "#${order.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusBadge(status = order.paymentStatus)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = order.productTitle, fontSize = 14.sp, color = Color.DarkGray)
            Text(text = "₦${order.price.toLong()} - ${order.customerName}", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payment Status Update Button
            OutlinedButton(
                onClick = { 
                    val nextStatus = when(order.paymentStatus) {
                        PaymentStatus.AWAITING_PAYMENT -> PaymentStatus.PAYMENT_RECEIVED
                        PaymentStatus.PAYMENT_RECEIVED -> PaymentStatus.DELIVERED
                        else -> PaymentStatus.AWAITING_PAYMENT
                    }
                    onUpdateStatus(nextStatus)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Update Status")
            }
        }
    }
}

@Composable
fun StatusBadge(status: PaymentStatus) {
    val (color, label) = when(status) {
        PaymentStatus.AWAITING_PAYMENT -> Pair(Color(0xFFFFA726), "Pending")
        PaymentStatus.PAYMENT_RECEIVED -> Pair(Color(0xFF4CAF50), "Received")
        PaymentStatus.DELIVERED -> Pair(Color(0xFF2196F3), "Delivered")
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = color.darken(0.2f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun UploadProductDialog(onDismiss: () -> Unit, onSubmit: (String, String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electronics") }
    val categories = listOf("Electronics", "Furniture", "Fashion", "Jewelry", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && price > 0) {
                        onSubmit(title, description, price, selectedCategory)
                    }
                }
            ) { Text("Upload") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("List New Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (NGN)") })
                
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true
                    )
                    // Simplified for brevity: In a real app, implement the dropdown menu properly
                }
            }
        }
    )
}

private fun Color.darken(amount: Float): Color {
    return copy(red = red * (1 - amount), green = green * (1 - amount), blue = blue * (1 - amount))
}
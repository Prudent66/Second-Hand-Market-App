package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.data.models.Product
import com.secondhandmarket.viewmodel.DASHBOARD_CATEGORIES
import com.secondhandmarket.viewmodel.DashboardViewModel

/**
 * Dashboard with a Shopping / Selling segmented pill below the app bar.
 *
 * - Shopping: search bar, category chips and a two-column product grid.
 * - Selling: upload item, bank (payout) details and an orders list where
 *   each order card cycles Awaiting Transfer -> Payment Received -> Delivered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUploadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.uiMessage) {
        uiState.uiMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { DashboardTopBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModeSwitchBar(
                isVendorMode = uiState.isVendorMode,
                onModeChanged = viewModel::onModeChanged
            )
            if (uiState.isVendorMode) {
                VendorModeContent(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    onUploadClick = { showUploadDialog = true }
                )
            } else {
                UserModeContent(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showUploadDialog) {
            UploadItemDialog(
                isUploading = uiState.isUploadingItem,
                onDismiss = { showUploadDialog = false },
                onConfirm = { title, description, price, category ->
                    viewModel.uploadItem(title, description, price, category)
                    showUploadDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Second Hand Market",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    )
}

/**
 * Segmented "Shopping / Selling" pill shown under the top bar. Replaces the
 * cramped mode switch that used to live in the app bar.
 */
@Composable
private fun ModeSwitchBar(
    isVendorMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            ModeOption(
                label = "Shopping",
                selected = !isVendorMode,
                onClick = { onModeChanged(false) },
                modifier = Modifier.weight(1f)
            )
            ModeOption(
                label = "Selling",
                selected = isVendorMode,
                onClick = { onModeChanged(true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// ---------------------------------------------------------------- User Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserModeContent(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredProducts = remember(uiState.products, uiState.searchQuery, uiState.selectedCategory) {
        uiState.products.filter { product ->
            val matchesCategory = uiState.selectedCategory == "All" ||
                product.category.equals(uiState.selectedCategory, ignoreCase = true)
            val matchesQuery = uiState.searchQuery.isBlank() ||
                product.title.contains(uiState.searchQuery, ignoreCase = true) ||
                product.description.contains(uiState.searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search laptops, phones, furniture...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DASHBOARD_CATEGORIES) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No items match your search yet.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            // Two fixed columns so narrow devices (~240dp wide) still get a grid.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product)
                }
            }
        }
    }
}

/**
 * Per-category gradients used as artwork when a product has no photo (or the
 * photo fails to load) - looks intentional instead of a flat colored block.
 */
private val CategoryGradients = mapOf(
    "electronics" to listOf(Color(0xFF123456), Color(0xFF2C6BAE)),
    "phones" to listOf(Color(0xFF0E3B2E), Color(0xFF2E8B62)),
    "fashion" to listOf(Color(0xFF3B2145), Color(0xFF7C4591)),
    "furniture" to listOf(Color(0xFF3E2C17), Color(0xFF8A6430))
)
private val FallbackGradient = listOf(Color(0xFF1B2735), Color(0xFF3A506B))

@Composable
private fun CategoryArt(category: String, modifier: Modifier = Modifier) {
    val colors = CategoryGradients[category.lowercase()] ?: FallbackGradient
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.take(1).uppercase(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.25f)
        )
    }
}

@Composable
private fun ProductCard(product: Product) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                // Category art sits behind the photo and shows through when
                // there is no image (or loading fails).
                CategoryArt(category = product.category, modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = product.imageUrl.ifBlank { null },
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = ColorPainter(Color.Transparent)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = product.condition,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = product.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "₦%,d".format(product.price.toLong()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -------------------------------------------------------------- Vendor Mode

@Composable
private fun VendorModeContent(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onUploadClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Upload Item button
        Button(
            onClick = onUploadClick,
            enabled = !uiState.isUploadingItem,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Upload Item", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bank (payout) details
        BankDetailsCard(
            accountNumber = uiState.bankAccountNumber,
            bankName = uiState.bankName,
            accountHolderName = uiState.accountHolderName,
            isSaved = uiState.isBankDetailsSaved,
            onAccountNumberChanged = viewModel::onBankAccountNumberChanged,
            onBankNameChanged = viewModel::onBankNameChanged,
            onAccountHolderChanged = viewModel::onAccountHolderNameChanged,
            onSave = viewModel::saveBankDetails
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Orders",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No orders yet. They will appear here when buyers check out.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onAdvanceStatus = { viewModel.advanceOrderStatus(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BankDetailsCard(
    accountNumber: String,
    bankName: String,
    accountHolderName: String,
    isSaved: Boolean,
    onAccountNumberChanged: (String) -> Unit,
    onBankNameChanged: (String) -> Unit,
    onAccountHolderChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Payout Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Payments from completed orders are transferred to this account.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = onAccountNumberChanged,
                label = { Text("Vendor Account Number") },
                placeholder = { Text("10-digit NUBAN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = onBankNameChanged,
                label = { Text("Bank Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accountHolderName,
                onValueChange = onAccountHolderChanged,
                label = { Text("Account Holder Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSave,
                enabled = accountNumber.length == 10 && bankName.isNotBlank() &&
                    accountHolderName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isSaved) "Update Bank Details" else "Save Bank Details",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onAdvanceStatus: () -> Unit
) {
    val nextStatus = DashboardViewModel.nextPaymentStatus(order.paymentStatus)

    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DashboardViewModel.orderDisplayId(order.id),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                PaymentStatusBadge(status = order.paymentStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = order.productTitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = "₦%,d · %s · %s".format(
                    order.price.toLong(),
                    order.customerName,
                    order.paymentMethod
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Payment status action - colored by the status being moved TO.
            if (nextStatus == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Button(
                    onClick = onAdvanceStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (nextStatus) {
                            PaymentStatus.PAYMENT_RECEIVED -> Color(0xFF2E7D32)
                            else -> Color(0xFF1565C0)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mark as ${DashboardViewModel.statusLabel(nextStatus)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(status: PaymentStatus) {
    val (label, color) = when (status) {
        PaymentStatus.AWAITING_PAYMENT -> "Awaiting Transfer" to Color(0xFFFFA726)
        PaymentStatus.PAYMENT_RECEIVED -> "Payment Received" to Color(0xFF4CAF50)
        PaymentStatus.DELIVERED -> "Delivered" to Color(0xFF2196F3)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ------------------------------------------------------------ Upload dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UploadItemDialog(
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, price: Double, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electronics") }

    val price = priceText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "List New Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Price (₦)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DASHBOARD_CATEGORIES.filter { it != "All" }.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, price, selectedCategory) },
                enabled = !isUploading && title.isNotBlank() && price > 0.0
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Upload")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

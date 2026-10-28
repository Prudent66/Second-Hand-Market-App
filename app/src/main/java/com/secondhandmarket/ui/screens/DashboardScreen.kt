package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.secondhandmarket.R
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.data.models.Product
import com.secondhandmarket.viewmodel.DASHBOARD_CATEGORIES
import com.secondhandmarket.viewmodel.DashboardViewModel

/**
 * Dashboard with a User Mode / Vendor Mode toggle in the top bar.
 *
 * - User Mode: search bar, category chips and a product grid.
 * - Vendor Mode: upload item, bank (payout) details and an orders list where
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

    Scaffold(
        topBar = {
            DashboardTopBar(
                isVendorMode = uiState.isVendorMode,
                onModeChanged = viewModel::onModeChanged
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isVendorMode) {
                VendorModeContent(
                    viewModel = viewModel,
                    onUploadClick = { showUploadDialog = true }
                )
            } else {
                UserModeContent(viewModel = viewModel)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isVendorMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Second Hand Market",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isVendorMode) "Vendor Mode" else "User Mode",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = {
            Text(
                text = "User Mode",
                fontSize = 12.sp,
                color = if (isVendorMode) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Switch(
                checked = isVendorMode,
                onCheckedChange = onModeChanged,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "Vendor Mode",
                fontSize = 12.sp,
                color = if (isVendorMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    )
}

// ---------------------------------------------------------------- User Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserModeContent(viewModel: DashboardViewModel) {
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search laptops, phones, furniture…") },
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
            // Product grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
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

@Composable
private fun ProductCard(product: Product) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = product.imageUrl.ifBlank { null },
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.bg_placeholder)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₦%,d".format(product.price.toLong()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = product.condition,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Vendor Mode

@Composable
private fun VendorModeContent(
    viewModel: DashboardViewModel,
    onUploadClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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

            // Payment status button - cycles Awaiting Transfer -> Payment Received -> Delivered.
            Button(
                onClick = onAdvanceStatus,
                enabled = nextStatus != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (order.paymentStatus) {
                        PaymentStatus.AWAITING_PAYMENT -> Color(0xFFF57C00)
                        PaymentStatus.PAYMENT_RECEIVED -> Color(0xFF2E7D32)
                        PaymentStatus.DELIVERED -> Color(0xFF1565C0)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (nextStatus) {
                        null -> "Delivered ✓"
                        else -> "Mark: ${DashboardViewModel.statusLabel(nextStatus)}"
                    },
                    fontWeight = FontWeight.SemiBold
                )
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

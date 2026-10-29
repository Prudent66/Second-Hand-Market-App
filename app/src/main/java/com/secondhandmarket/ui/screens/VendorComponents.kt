package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.ui.components.GradientButton
import com.secondhandmarket.ui.components.SectionHeader
import com.secondhandmarket.ui.components.formatNaira
import com.secondhandmarket.ui.components.pressScale
import com.secondhandmarket.ui.theme.AquaSoft
import com.secondhandmarket.ui.theme.StatusAmber
import com.secondhandmarket.ui.theme.StatusBlue
import com.secondhandmarket.ui.theme.StatusGreen
import com.secondhandmarket.viewmodel.DASHBOARD_CATEGORIES
import com.secondhandmarket.viewmodel.DashboardUiState
import com.secondhandmarket.viewmodel.DashboardViewModel

/** Seller Hub: quick stats, upload CTA, payout card and live order list. */
@Composable
internal fun VendorContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Seller Hub",
                subtitle = "Manage listings, orders and payouts"
            )
        }
        item {
            VendorStatsRow(orders = uiState.orders)
        }
        item {
            GradientButton(
                text = "List a new item",
                onClick = onUploadClick,
                icon = Icons.Default.Add,
                loading = uiState.isUploadingItem
            )
        }
        item {
            PayoutCard(uiState = uiState, viewModel = viewModel)
        }
        item {
            SectionHeader(
                title = "Orders",
                subtitle = if (uiState.orders.isEmpty()) {
                    "No orders yet"
                } else {
                    "${uiState.orders.size} orders to manage"
                }
            )
        }
        if (uiState.orders.isEmpty()) {
            item {
                Text(
                    text = "Orders appear here as soon as a buyer checks out one of your items.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        } else {
            items(uiState.orders, key = { order -> order.id }) { order ->
                OrderCard(
                    order = order,
                    onAdvanceStatus = { viewModel.advanceOrderStatus(order.id) }
                )
            }
        }
    }
}

/** Three compact stat cards summarising the seller's activity. */
@Composable
private fun VendorStatsRow(orders: List<Order>) {
    val awaiting = orders.count { it.paymentStatus == PaymentStatus.AWAITING_PAYMENT }
    val earned = orders
        .filter { it.paymentStatus != PaymentStatus.AWAITING_PAYMENT }
        .sumOf { it.price }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VendorStatCard(
            label = "Orders",
            value = orders.size.toString(),
            accent = StatusBlue,
            modifier = Modifier.weight(1f)
        )
        VendorStatCard(
            label = "Awaiting",
            value = awaiting.toString(),
            accent = StatusAmber,
            modifier = Modifier.weight(1f)
        )
        VendorStatCard(
            label = "Earned",
            value = formatNaira(earned),
            accent = StatusGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VendorStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Credit-card style payout panel: masked number when saved, form when editing. */
@Composable
private fun PayoutCard(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var editing by rememberSaveable(uiState.isBankDetailsSaved) {
        mutableStateOf(!uiState.isBankDetailsSaved)
    }
    val showForm = editing || !uiState.isBankDetailsSaved

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B3A4A), Color(0xFF0F766E))
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAYOUT ACCOUNT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "NUBAN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            if (showForm) {
                PayoutForm(
                    accountNumber = uiState.bankAccountNumber,
                    bankName = uiState.bankName,
                    accountHolderName = uiState.accountHolderName,
                    onAccountNumberChanged = viewModel::onBankAccountNumberChanged,
                    onBankNameChanged = viewModel::onBankNameChanged,
                    onAccountHolderChanged = viewModel::onAccountHolderNameChanged,
                    onSave = {
                        viewModel.saveBankDetails()
                        editing = false
                    }
                )
            } else {
                PayoutSummary(
                    accountNumber = uiState.bankAccountNumber,
                    bankName = uiState.bankName,
                    accountHolderName = uiState.accountHolderName,
                    onEdit = { editing = true }
                )
            }
        }
    }
}

@Composable
private fun PayoutSummary(
    accountNumber: String,
    bankName: String,
    accountHolderName: String,
    onEdit: () -> Unit
) {
    Column {
        Text(
            text = "**** **** **** ${accountNumber.takeLast(4)}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                PayoutLabel(text = "BANK")
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                PayoutLabel(text = "ACCOUNT HOLDER")
                Text(
                    text = accountHolderName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
        TextButton(onClick = onEdit) {
            Text(
                text = "Update details",
                color = AquaSoft,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PayoutLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Color.White.copy(alpha = 0.6f)
    )
}

@Composable
private fun PayoutForm(
    accountNumber: String,
    bankName: String,
    accountHolderName: String,
    onAccountNumberChanged: (String) -> Unit,
    onBankNameChanged: (String) -> Unit,
    onAccountHolderChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Payments from completed orders are transferred here.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        PayoutField(
            value = accountNumber,
            onValueChange = onAccountNumberChanged,
            label = "Account number",
            keyboardType = KeyboardType.Number
        )
        PayoutField(
            value = bankName,
            onValueChange = onBankNameChanged,
            label = "Bank name",
            keyboardType = KeyboardType.Text
        )
        PayoutField(
            value = accountHolderName,
            onValueChange = onAccountHolderChanged,
            label = "Account holder",
            keyboardType = KeyboardType.Text
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pressScale(interaction)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    enabled = accountNumber.length == DashboardViewModel.BANK_ACCOUNT_LENGTH &&
                        bankName.isNotBlank() &&
                        accountHolderName.isNotBlank(),
                    onClick = onSave
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Save payout account",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B3A4A)
            )
        }
    }
}

@Composable
private fun PayoutField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AquaSoft,
            focusedBorderColor = AquaSoft,
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            focusedLabelColor = AquaSoft,
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Order card with a colour-coded status pill and a full-width action button. */
@Composable
private fun OrderCard(order: Order, onAdvanceStatus: () -> Unit) {
    val nextStatus = DashboardViewModel.nextPaymentStatus(order.paymentStatus)
    val interaction = remember { MutableInteractionSource() }
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DashboardViewModel.orderDisplayId(order.id),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OrderStatusBadge(status = order.paymentStatus)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = order.productTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatNaira(order.price)}  |  ${order.customerName}  |  ${order.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (nextStatus == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order completed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusGreen
                    )
                }
            } else {
                val actionColor = when (nextStatus) {
                    PaymentStatus.PAYMENT_RECEIVED -> StatusGreen
                    else -> StatusBlue
                }
                Button(
                    onClick = onAdvanceStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressScale(interaction),
                    interactionSource = interaction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor)
                ) {
                    Text(
                        text = "Mark as ${DashboardViewModel.statusLabel(nextStatus)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/** Amber for awaiting, green for received, blue for delivered. */
@Composable
private fun OrderStatusBadge(status: PaymentStatus) {
    val color = when (status) {
        PaymentStatus.AWAITING_PAYMENT -> StatusAmber
        PaymentStatus.PAYMENT_RECEIVED -> StatusGreen
        PaymentStatus.DELIVERED -> StatusBlue
    }
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.16f),
        contentColor = color
    ) {
        Text(
            text = DashboardViewModel.statusLabel(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Dialog used by the seller hub to publish a new listing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UploadItemDialog(
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "List a new item",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Price in Naira") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DASHBOARD_CATEGORIES.filter { it != "All" }.forEach { category ->
                        DialogCategoryChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, price, selectedCategory) },
                enabled = !isUploading && title.isNotBlank() && price > 0.0,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Publish", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

@Composable
private fun DialogCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

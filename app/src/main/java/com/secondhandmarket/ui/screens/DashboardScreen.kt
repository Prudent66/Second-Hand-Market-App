package com.secondhandmarket.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.ui.components.BrandGradient
import com.secondhandmarket.ui.components.SectionHeader
import com.secondhandmarket.viewmodel.DashboardUiState
import com.secondhandmarket.viewmodel.DashboardViewModel

private const val TAB_HOME = 0
private const val TAB_SEARCH = 1
private const val TAB_PROFILE = 2

/**
 * Main dashboard. Home shows the marketplace or the seller hub depending on
 * the segmented mode switch, Search is a focused search experience, and
 * Profile shows the signed-in account. Long-pressing the app title opens the
 * hidden admin console.
 */
@Composable
fun DashboardScreen(
    onAdminClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.uiMessage) {
        val message = uiState.uiMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearUiMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                onTitleLongPress = onAdminClick,
                onProfileClick = { selectedTab = TAB_PROFILE }
            )
        },
        bottomBar = {
            DashboardBottomBar(
                selectedTab = selectedTab,
                onSelect = { tab -> selectedTab = tab }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                TAB_HOME -> {
                    ModeSwitchBar(
                        isVendorMode = uiState.isVendorMode,
                        onModeChanged = viewModel::onModeChanged,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    if (uiState.isVendorMode) {
                        VendorContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onUploadClick = { showUploadDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        MarketplaceContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TAB_SEARCH -> SearchTab(uiState = uiState, viewModel = viewModel)
                else -> ProfileTab(uiState = uiState, viewModel = viewModel)
            }
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

/** Brand bar. Long-press the title to reveal the admin console. */
@Composable
private fun DashboardTopBar(
    onTitleLongPress: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(BrandGradient.horizontal(), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SH",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onTitleLongPress() })
                }
        ) {
            Text(
                text = "Second Hand Market",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Premium pre-owned marketplace",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DashboardBottomBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == TAB_HOME,
            onClick = { onSelect(TAB_HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text(text = "Home", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = selectedTab == TAB_SEARCH,
            onClick = { onSelect(TAB_SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text(text = "Search", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = selectedTab == TAB_PROFILE,
            onClick = { onSelect(TAB_PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text(text = "Profile", fontSize = 11.sp) }
        )
    }
}

/** Segmented Shopping / Selling switch with an animated pill. */
@Composable
private fun ModeSwitchBar(
    isVendorMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeOption(
            label = "Shopping",
            selected = !isVendorMode,
            modifier = Modifier.weight(1f),
            onClick = { onModeChanged(false) }
        )
        ModeOption(
            label = "Selling",
            selected = isVendorMode,
            modifier = Modifier.weight(1f),
            onClick = { onModeChanged(true) }
        )
    }
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "modeBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "modeContent"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun SearchTab(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    val products = remember(uiState.products, uiState.searchQuery, uiState.selectedCategory) {
        filterProducts(uiState.products, uiState.searchQuery, uiState.selectedCategory)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(
                title = "Search",
                subtitle = "Find exactly what you need"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MarketSearchField(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                autofocus = true
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        CategoryChips(
            selected = uiState.selectedCategory,
            onSelect = viewModel::onCategorySelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductGrid(products = products, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileTab(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileHeaderCard(uiState = uiState)
        ProfileInfoCard(uiState = uiState)
        SectionHeader(
            title = "Seller mode",
            subtitle = "Manage listings, orders and payouts"
        )
        ModeSwitchBar(
            isVendorMode = uiState.isVendorMode,
            onModeChanged = viewModel::onModeChanged
        )
        ProfileStatsRow(uiState = uiState)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileHeaderCard(uiState: DashboardUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradient.horizontal())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.userName.trim().take(1).uppercase().ifEmpty { "S" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = uiState.userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (uiState.userPhone.isBlank()) "Add a phone number" else uiState.userPhone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (uiState.userLocation.isBlank()) "Location not set" else uiState.userLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(uiState: DashboardUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        val listings = uiState.products.count { it.vendorId == DashboardViewModel.VENDOR_ID }
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoRow(
                label = "Account type",
                value = if (uiState.isVendorMode) "Seller" else "Buyer"
            )
            ProfileInfoRow(label = "Listings", value = "$listings")
            ProfileInfoRow(label = "Active orders", value = "${uiState.orders.size}")
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileStatsRow(uiState: DashboardUiState) {
    val delivered = uiState.orders.count { order ->
        order.paymentStatus == PaymentStatus.DELIVERED
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileStatCard(
            value = uiState.orders.size.toString(),
            label = "Orders",
            modifier = Modifier.weight(1f)
        )
        ProfileStatCard(
            value = delivered.toString(),
            label = "Completed",
            modifier = Modifier.weight(1f)
        )
        ProfileStatCard(
            value = if (uiState.isVendorMode) "On" else "Off",
            label = "Selling",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

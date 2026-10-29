package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secondhandmarket.ui.components.BrandGradient
import com.secondhandmarket.ui.components.formatNaira
import com.secondhandmarket.ui.theme.StatusAmber
import com.secondhandmarket.ui.theme.StatusBlue
import com.secondhandmarket.ui.theme.StatusGreen

private data class AdminUser(
    val name: String,
    val phone: String,
    val orders: Int,
    val verified: Boolean
)

private data class AdminVendor(
    val store: String,
    val owner: String,
    val listings: Int,
    val active: Boolean
)

private data class AdminOrder(
    val id: String,
    val item: String,
    val buyer: String,
    val amount: Double,
    val status: String
)

private val mockUsers = listOf(
    AdminUser("Amina Bello", "0803 412 7788", orders = 4, verified = true),
    AdminUser("Chinedu Okafor", "0806 220 1144", orders = 2, verified = true),
    AdminUser("Fatima Yusuf", "0701 887 3322", orders = 1, verified = false),
    AdminUser("Tunde Adeyemi", "0902 554 9011", orders = 7, verified = true),
    AdminUser("Grace Eze", "0814 339 0022", orders = 3, verified = true),
    AdminUser("Musa Ibrahim", "0705 991 4477", orders = 0, verified = false)
)

private val mockVendors = listOf(
    AdminVendor("TechDen", "Yusuf Lawal", listings = 18, active = true),
    AdminVendor("Fashion Loft", "Ngozi Obi", listings = 34, active = true),
    AdminVendor("HomeWorks", "Bala Sani", listings = 11, active = true),
    AdminVendor("Gadget Plug", "Emeka Nwosu", listings = 7, active = false),
    AdminVendor("Vintage Vault", "Halima Garba", listings = 22, active = true)
)

private val mockOrders = listOf(
    AdminOrder("SH-1042", "HP Laptop 15-inch", "Amina Bello", 250000.0, "Awaiting"),
    AdminOrder("SH-1041", "Samsung Galaxy S21", "Chinedu Okafor", 350000.0, "Received"),
    AdminOrder("SH-1039", "Wooden Wardrobe", "Grace Eze", 80000.0, "Delivered"),
    AdminOrder("SH-1036", "iPhone 13 Pro", "Tunde Adeyemi", 620000.0, "Received"),
    AdminOrder("SH-1032", "Study Desk with Shelves", "Fatima Yusuf", 38000.0, "Delivered"),
    AdminOrder("SH-1028", "PlayStation 5 Console", "Musa Ibrahim", 480000.0, "Awaiting")
)

/**
 * Hidden admin console, opened by long-pressing the dashboard title.
 * Shows mock marketplace data grouped into Users, Vendors and Orders.
 */
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AdminTopBar(onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AdminStatsRow(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                listOf("Users", "Vendors", "Orders").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        }
                    )
                }
            }
            when (selectedTab) {
                0 -> UsersTab()
                1 -> VendorsTab()
                else -> OrdersTab()
            }
        }
    }
}

@Composable
private fun AdminTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Admin Console",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Marketplace overview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "ADMIN",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun AdminStatsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdminStatCard(
            value = mockUsers.size.toString(),
            label = "Users",
            modifier = Modifier.weight(1f)
        )
        AdminStatCard(
            value = mockVendors.size.toString(),
            label = "Vendors",
            modifier = Modifier.weight(1f)
        )
        AdminStatCard(
            value = mockOrders.size.toString(),
            label = "Orders",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGradient.vertical())
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
private fun UsersTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(mockUsers) { user ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminAvatar(name = user.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${user.phone}  |  ${user.orders} orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = if (user.verified) "Verified" else "Pending",
                        color = if (user.verified) StatusGreen else StatusAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(mockVendors) { vendor ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminAvatar(name = vendor.store)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vendor.store,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vendor.owner}  |  ${vendor.listings} listings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = if (vendor.active) "Active" else "Paused",
                        color = if (vendor.active) StatusGreen else StatusAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(mockOrders) { order ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.id,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(text = order.status, color = orderStatusColor(order.status))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = order.item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.buyer}  |  ${formatNaira(order.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGradient.horizontal()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.16f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun orderStatusColor(status: String): Color = when (status) {
    "Awaiting" -> StatusAmber
    "Received" -> StatusGreen
    else -> StatusBlue
}

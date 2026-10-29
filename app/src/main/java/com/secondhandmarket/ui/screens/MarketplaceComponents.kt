package com.secondhandmarket.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.secondhandmarket.data.models.Product
import com.secondhandmarket.ui.components.ConditionBadge
import com.secondhandmarket.ui.components.SectionHeader
import com.secondhandmarket.ui.components.formatNaira
import com.secondhandmarket.ui.theme.CyanAccent
import com.secondhandmarket.ui.theme.OceanBlue
import com.secondhandmarket.ui.theme.OceanDeep
import com.secondhandmarket.ui.theme.TealAccent
import com.secondhandmarket.viewmodel.DASHBOARD_CATEGORIES
import com.secondhandmarket.viewmodel.DashboardUiState
import com.secondhandmarket.viewmodel.DashboardViewModel

/** Applies the active search query and category filter to the catalogue. */
internal fun filterProducts(
    products: List<Product>,
    query: String,
    category: String
): List<Product> {
    val trimmed = query.trim()
    return products.filter { product ->
        val matchesCategory = category == DASHBOARD_CATEGORIES.first() ||
            product.category.equals(category, ignoreCase = true)
        val matchesQuery = trimmed.isEmpty() ||
            product.title.contains(trimmed, ignoreCase = true) ||
            product.category.contains(trimmed, ignoreCase = true) ||
            product.description.contains(trimmed, ignoreCase = true)
        matchesCategory && matchesQuery
    }
}

/** Home tab content: greeting, floating search bar, category chips, product grid. */
@Composable
internal fun MarketplaceContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val products = remember(uiState.products, uiState.searchQuery, uiState.selectedCategory) {
        filterProducts(uiState.products, uiState.searchQuery, uiState.selectedCategory)
    }
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(
                title = "Discover",
                subtitle = "Fresh pre-loved finds near you"
            )
            Spacer(modifier = Modifier.height(14.dp))
            MarketSearchField(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CategoryChips(
            selected = uiState.selectedCategory,
            onSelect = viewModel::onCategorySelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fresh picks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${products.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProductGrid(products = products, modifier = Modifier.weight(1f))
    }
}

/** Rounded search field that floats above the marketplace with a soft shadow. */
@Composable
internal fun MarketSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    autofocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    if (autofocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "Search laptops, phones, furniture...",
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/** Horizontally scrolling category pills with an animated selection state. */
@Composable
internal fun CategoryChips(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DASHBOARD_CATEGORIES) { category ->
            val isSelected = category == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                label = "categoryBackground"
            )
            val content by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "categoryContent"
            )
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = background,
                shadowElevation = if (isSelected) 0.dp else 1.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable { onSelect(category) }
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = content
                )
            }
        }
    }
}

/** Two column product grid with an empty state. */
@Composable
internal fun ProductGrid(products: List<Product>, modifier: Modifier = Modifier) {
    if (products.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No items match your search yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { product -> product.id }) { product ->
            ProductCard(product = product)
        }
    }
}

/** Elevated marketplace card with a Coil photo, condition badge and price. */
@Composable
private fun ProductCard(product: Product) {
    var favourite by rememberSaveable(product.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val imageRequest = remember(product.id, product.imageUrl, product.title) {
        ImageRequest.Builder(context)
            .data(product.imageUrl.ifBlank { picsumUrl(product.title) })
            .crossfade(true)
            .build()
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                CategoryArt(category = product.category, modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = imageRequest,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                ConditionBadge(
                    text = product.condition,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { favourite = !favourite },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (favourite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = "Save item",
                        tint = if (favourite) Color(0xFFFF6B81) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Text(
                    text = product.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatNaira(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Coloured gradient drawn behind each photo while Coil loads (or if it fails). */
@Composable
private fun CategoryArt(category: String, modifier: Modifier = Modifier) {
    val colors = when (category) {
        "Electronics" -> listOf(OceanBlue, CyanAccent)
        "Fashion" -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
        "Furniture" -> listOf(Color(0xFF92400E), Color(0xFFD97706))
        "Phones" -> listOf(Color(0xFF0F766E), Color(0xFF22D3EE))
        else -> listOf(OceanDeep, TealAccent)
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.28f),
            modifier = Modifier.size(44.dp)
        )
    }
}

/** Deterministic Picsum URL so every item keeps a stable placeholder photo. */
private fun picsumUrl(title: String): String {
    val seed = title.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "https://picsum.photos/seed/$seed/400/400"
}

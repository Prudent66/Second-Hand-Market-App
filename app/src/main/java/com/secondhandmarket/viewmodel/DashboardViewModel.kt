package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.data.models.Product
import com.secondhandmarket.data.repository.OrderRepository
import com.secondhandmarket.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Categories shown as filter chips in User Mode. */
val DASHBOARD_CATEGORIES = listOf("All", "Electronics", "Fashion", "Furniture", "Phones")

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Transient success/info message, consumed by a snackbar. */
    val uiMessage: String? = null,
    /** true = Vendor Mode, false = User Mode. */
    val isVendorMode: Boolean = false,
    // ----- User Mode -----
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = DASHBOARD_CATEGORIES.first(),
    // ----- Vendor Mode -----
    val orders: List<Order> = emptyList(),
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val accountHolderName: String = "",
    val isBankDetailsSaved: Boolean = false,
    val isUploadingItem: Boolean = false
)

/**
 * Backing ViewModel for the dashboard that toggles between User Mode
 * (browsing products) and Vendor Mode (uploads, payouts, orders).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadOrders()
    }

    // ---------------------------------------------------------------- Mode

    fun onModeChanged(isVendorMode: Boolean) {
        _uiState.update { it.copy(isVendorMode = isVendorMode, error = null) }
    }

    // ---------------------------------------------------------- User Mode

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val products = productRepository.getProducts()
                _uiState.update { it.copy(isLoading = false, products = products) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load products") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // --------------------------------------------------------- Vendor Mode

    fun loadOrders() {
        viewModelScope.launch {
            try {
                // Mock vendor id until vendors are wired to the authenticated user.
                val orders = orderRepository.getVendorOrders(VENDOR_ID)
                _uiState.update { it.copy(orders = orders) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load orders") }
            }
        }
    }

    fun onBankAccountNumberChanged(value: String) {
        // Only digits, max 10 (NUBAN).
        val filtered = value.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(bankAccountNumber = filtered) }
    }

    fun onBankNameChanged(value: String) {
        _uiState.update { it.copy(bankName = value) }
    }

    fun onAccountHolderNameChanged(value: String) {
        _uiState.update { it.copy(accountHolderName = value) }
    }

    fun saveBankDetails() {
        val state = _uiState.value
        if (state.bankAccountNumber.length != BANK_ACCOUNT_LENGTH) {
            _uiState.update { it.copy(error = "Enter the $BANK_ACCOUNT_LENGTH-digit account number") }
            return
        }
        if (state.bankName.isBlank() || state.accountHolderName.isBlank()) {
            _uiState.update { it.copy(error = "Bank name and account holder are required") }
            return
        }
        // TODO: persist through a VendorRepository once the vendors API is wired.
        _uiState.update {
            it.copy(
                isBankDetailsSaved = true,
                error = null,
                uiMessage = "Bank details saved for payouts"
            )
        }
    }

    fun uploadItem(title: String, description: String, price: Double, category: String) {
        if (title.isBlank() || price <= 0.0) {
            _uiState.update { it.copy(error = "Title and a price above ₦0 are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingItem = true, error = null) }
            try {
                productRepository.createProduct(
                    vendorId = VENDOR_ID,
                    title = title,
                    description = description,
                    price = price,
                    imageUrl = "",
                    category = category,
                    condition = "Good"
                )
                _uiState.update {
                    it.copy(
                        isUploadingItem = false,
                        uiMessage = "\"$title\" is now live"
                    )
                }
                loadProducts()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isUploadingItem = false, error = "Failed to upload item")
                }
            }
        }
    }

    /**
     * Cycles an order's payment status:
     * Awaiting Transfer -> Payment Received -> Delivered.
     */
    fun advanceOrderStatus(orderId: String) {
        val current = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        val next = nextPaymentStatus(current.paymentStatus) ?: return
        viewModelScope.launch {
            try {
                orderRepository.updatePaymentStatus(orderId, next)
                _uiState.update { state ->
                    state.copy(
                        orders = state.orders.map { order ->
                            if (order.id == orderId) order.copy(paymentStatus = next) else order
                        },
                        uiMessage = "Order ${orderDisplayId(orderId)} → ${statusLabel(next)}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update order status") }
            }
        }
    }

    fun clearUiMessage() {
        _uiState.update { it.copy(uiMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        /** Mock vendor id used until auth is wired into the dashboard. */
        const val VENDOR_ID = "v1"
        const val BANK_ACCOUNT_LENGTH = 10

        fun nextPaymentStatus(current: PaymentStatus): PaymentStatus? = when (current) {
            PaymentStatus.AWAITING_PAYMENT -> PaymentStatus.PAYMENT_RECEIVED
            PaymentStatus.PAYMENT_RECEIVED -> PaymentStatus.DELIVERED
            PaymentStatus.DELIVERED -> null
        }

        fun statusLabel(status: PaymentStatus): String = when (status) {
            PaymentStatus.AWAITING_PAYMENT -> "Awaiting Transfer"
            PaymentStatus.PAYMENT_RECEIVED -> "Payment Received"
            PaymentStatus.DELIVERED -> "Delivered"
        }

        fun orderDisplayId(orderId: String): String = "#${orderId.removePrefix("o")}"
    }
}

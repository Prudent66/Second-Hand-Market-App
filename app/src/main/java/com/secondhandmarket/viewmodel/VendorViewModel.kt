package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import com.secondhandmarket.data.repository.OrderRepository
import com.secondhandmarket.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                // Fetch orders for the current vendor (mocked as ID "v1" for now)
                val fetchedOrders = orderRepository.getVendorOrders("v1")
                _orders.value = fetchedOrders
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateOrderPaymentStatus(orderId: String, newStatus: PaymentStatus) {
        viewModelScope.launch {
            try {
                orderRepository.updatePaymentStatus(orderId, newStatus)
                // Refresh list to reflect changes
                loadOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadProduct(title: String, description: String, price: Double, category: String) {
        viewModelScope.launch {
            try {
                // Mock implementation: In a real app, this would handle image uploads too
                productRepository.createProduct(
                    vendorId = "v1",
                    title = title,
                    description = description,
                    price = price,
                    imageUrl = "", 
                    category = category,
                    condition = "Good"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
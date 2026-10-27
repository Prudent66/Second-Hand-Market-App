package com.secondhandmarket.data.repository

import com.secondhandmarket.data.models.Order
import com.secondhandmarket.data.models.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor() {

    // Mock data for now. In a real app, this would call the API or Firebase.
    private val mockOrders = mutableListOf(
        Order(
            id = "o1",
            productId = "1",
            productTitle = "HP Laptop 15-inch",
            customerName = "John Doe",
            customerId = "u1",
            vendorId = "v1",
            price = 250000.0,
            paymentMethod = "Bank Transfer",
            paymentStatus = PaymentStatus.AWAITING_PAYMENT,
            createdAt = System.currentTimeMillis() - 3600000
        ),
        Order(
            id = "o2",
            productId = "2",
            productTitle = "Samsung Galaxy S21",
            customerName = "Jane Smith",
            customerId = "u2",
            vendorId = "v1",
            price = 350000.0,
            paymentMethod = "Pay on Delivery",
            paymentStatus = PaymentStatus.PAYMENT_RECEIVED,
            createdAt = System.currentTimeMillis() - 7200000
        )
    )

    suspend fun getVendorOrders(vendorId: String): List<Order> {
        kotlinx.coroutines.delay(400)
        return mockOrders.filter { it.vendorId == vendorId }.sortedByDescending { it.createdAt }
    }

    suspend fun updatePaymentStatus(orderId: String, newStatus: PaymentStatus) {
        val index = mockOrders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            mockOrders[index] = mockOrders[index].copy(paymentStatus = newStatus)
        }
    }
}
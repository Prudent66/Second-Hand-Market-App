package com.secondhandmarket.data.models

enum class PaymentStatus {
    AWAITING_PAYMENT,
    PAYMENT_RECEIVED,
    DELIVERED
}

data class Order(
    val id: String,
    val productId: String,
    val productTitle: String,
    val customerName: String,
    val customerId: String,
    val vendorId: String,
    val price: Double,
    val paymentMethod: String, // "Bank Transfer" or "Pay on Delivery"
    val paymentStatus: PaymentStatus,
    val createdAt: Long
)
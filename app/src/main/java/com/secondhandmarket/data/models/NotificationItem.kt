package com.secondhandmarket.data.models

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String // e.g., "PAYMENT_UPDATE", "NEW_ORDER"
)
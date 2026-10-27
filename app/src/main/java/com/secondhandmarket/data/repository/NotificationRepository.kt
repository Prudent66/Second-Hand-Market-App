package com.secondhandmarket.data.repository

import com.secondhandmarket.data.models.NotificationItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {

    // Mock data for now. In a real app, this would call the API or Firebase.
    private val mockNotifications = mutableListOf(
        NotificationItem(
            id = "n1",
            title = "Payment Status Updated",
            message = "Vendor v1 marked order o1 as PAYMENT_RECEIVED.",
            timestamp = System.currentTimeMillis() - 60000,
            isRead = false,
            type = "PAYMENT_UPDATE"
        ),
        NotificationItem(
            id = "n2",
            title = "New Order Placed",
            message = "Customer u2 placed an order for Samsung Galaxy S21.",
            timestamp = System.currentTimeMillis() - 120000,
            isRead = true,
            type = "NEW_ORDER"
        )
    )

    suspend fun getAdminNotifications(): List<NotificationItem> {
        kotlinx.coroutines.delay(300)
        return mockNotifications.toList()
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        val index = mockNotifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            mockNotifications[index] = mockNotifications[index].copy(isRead = true)
        }
    }
}
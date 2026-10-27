package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.models.NotificationItem
import com.secondhandmarket.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                // Fetch all unread/recent notifications for admin
                val fetchedNotifications = notificationRepository.getAdminNotifications()
                _notifications.value = fetchedNotifications.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markNotificationAsRead(notificationId)
                loadNotifications() // Refresh list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
package com.secondhandmarket.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging pushes (payment updates, new orders).
 * Requires a configured Firebase project (google-services.json) to receive
 * real messages; safe to run unconfigured in the meantime.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${remoteMessage.data}")
        // TODO: surface payment/order updates as local notifications.
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM registration token: $token")
        // TODO: send the token to the backend so admins/vendors get pushes.
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}

package com.secondhandmarket.data.repository

import com.secondhandmarket.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun sendOtp(phoneNumber: String): Boolean {
        return try {
            val response = apiService.sendOtp(mapOf("phone_number" to phoneNumber))
            response.isSuccessful
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otpCode: String): Boolean {
        return try {
            val response = apiService.verifyOtp(
                mapOf(
                    "phone_number" to phoneNumber,
                    "otp_code" to otpCode
                )
            )
            if (response.isSuccessful && response.body()?.token != null) {
                // Save token locally (implement DataStore or SharedPreferences here)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
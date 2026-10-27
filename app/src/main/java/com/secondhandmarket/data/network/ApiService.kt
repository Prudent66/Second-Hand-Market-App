package com.secondhandmarket.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    
    @POST("/api/auth/send-otp")
    suspend fun sendOtp(@Body request: Map<String, String>): Response<Unit>

    @POST("/api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: Map<String, String>): Response<AuthTokenResponse>
}

data class AuthTokenResponse(
    val token: String,
    val userId: String
)
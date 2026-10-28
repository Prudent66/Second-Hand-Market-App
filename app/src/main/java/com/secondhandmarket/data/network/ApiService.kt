package com.secondhandmarket.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DTOs matching the backend auth API (see backend/src/controllers/authController.js).
 */
data class RegisterRequest(
    val phoneNumber: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("mockOtp") val mockOtp: String? = null
)

data class VerifyOtpRequest(
    val phoneNumber: String,
    val otp: String
)

data class VerifyOtpResponse(
    val success: Boolean,
    val token: String? = null,
    @SerializedName("isNewUser") val isNewUser: Boolean = false,
    val user: UserDto? = null
)

data class ProfileUpdateRequest(
    val name: String,
    val state: String,
    val lga: String,
    val address: String
)

data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserDto? = null
)

data class UserDto(
    val id: String,
    val phoneNumber: String? = null,
    val fullName: String? = null,
    val state: String? = null,
    val lga: String? = null,
    val address: String? = null,
    val isVendor: Boolean = false
)

interface ApiService {

    // POST /api/auth/register - generates a mock OTP for the phone number
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // POST /api/auth/verify-otp - mock verifies the OTP and returns a JWT
    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<VerifyOtpResponse>

    // POST /api/auth/profile - JWT protected profile update
    @POST("api/auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileUpdateRequest
    ): Response<ProfileResponse>
}

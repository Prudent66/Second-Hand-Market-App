package com.secondhandmarket.data.repository

import com.secondhandmarket.data.local.TokenStore
import com.secondhandmarket.data.network.ApiService
import com.secondhandmarket.data.network.ProfileUpdateRequest
import com.secondhandmarket.data.network.RegisterRequest
import com.secondhandmarket.data.network.VerifyOtpRequest
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Result of POST /api/auth/register */
sealed interface RegisterResult {
    data class Success(val mockOtp: String?, val isMock: Boolean) : RegisterResult
    data class Failure(val message: String) : RegisterResult
}

/** Result of POST /api/auth/verify-otp */
sealed interface VerifyResult {
    data class Success(val token: String, val isMock: Boolean) : VerifyResult
    data class Failure(val message: String) : VerifyResult
}

/** Result of POST /api/auth/profile */
sealed interface ProfileResult {
    data class Success(val isMock: Boolean) : ProfileResult
    data class Failure(val message: String) : ProfileResult
}

/**
 * Auth repository backed by the Node.js auth API.
 *
 * The backend ships with a mock OTP flow, and while the API server is not
 * reachable (e.g. demo mode on a device without the server) the repository
 * falls back to a local mock so the UI flow never dead-ends. Mock behaviour
 * is always flagged with `isMock = true` so the UI can show a hint.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) {

    suspend fun register(phoneNumber: String): RegisterResult {
        return try {
            val response = apiService.register(RegisterRequest(phoneNumber = phoneNumber))
            val body = response.body()
            if (response.isSuccessful && body != null && body.success) {
                RegisterResult.Success(mockOtp = body.mockOtp, isMock = false)
            } else {
                RegisterResult.Failure(body?.message ?: "Failed to send OTP (${response.code()})")
            }
        } catch (e: IOException) {
            // Backend unreachable - demo fallback so the flow can continue.
            RegisterResult.Success(mockOtp = MOCK_OTP, isMock = true)
        } catch (e: Exception) {
            RegisterResult.Failure(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otpCode: String): VerifyResult {
        return try {
            val response = apiService.verifyOtp(
                VerifyOtpRequest(phoneNumber = phoneNumber, otp = otpCode)
            )
            val body = response.body()
            val token = body?.token
            if (response.isSuccessful && body != null && token != null) {
                tokenStore.saveAuth(token = token, phoneNumber = phoneNumber)
                VerifyResult.Success(token = token, isMock = false)
            } else {
                VerifyResult.Failure("Invalid OTP code")
            }
        } catch (e: IOException) {
            if (otpCode == MOCK_OTP) {
                // Demo fallback: accept the well-known mock OTP without a server.
                val mockToken = "$MOCK_TOKEN_PREFIX${UUID.randomUUID()}"
                tokenStore.saveAuth(token = mockToken, phoneNumber = phoneNumber)
                VerifyResult.Success(token = mockToken, isMock = true)
            } else {
                VerifyResult.Failure("Cannot reach the server. Please try again.")
            }
        } catch (e: Exception) {
            VerifyResult.Failure(e.message ?: "Verification failed")
        }
    }

    suspend fun updateProfile(
        fullName: String,
        state: String,
        lga: String,
        address: String
    ): ProfileResult {
        val token = tokenStore.authToken()
            ?: return ProfileResult.Failure("You are not signed in")

        if (token.startsWith(MOCK_TOKEN_PREFIX)) {
            // Demo mode: persist locally only.
            tokenStore.saveProfile(fullName, state, lga, address)
            return ProfileResult.Success(isMock = true)
        }

        return try {
            val response = apiService.updateProfile(
                authorization = "Bearer $token",
                request = ProfileUpdateRequest(
                    name = fullName,
                    state = state,
                    lga = lga,
                    address = address
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                tokenStore.saveProfile(fullName, state, lga, address)
                ProfileResult.Success(isMock = false)
            } else {
                ProfileResult.Failure("Failed to save profile (${response.code()})")
            }
        } catch (e: IOException) {
            // Backend unreachable - keep the demo flow alive by saving locally.
            tokenStore.saveProfile(fullName, state, lga, address)
            ProfileResult.Success(isMock = true)
        } catch (e: Exception) {
            ProfileResult.Failure(e.message ?: "Unknown error occurred")
        }
    }

    companion object {
        /** Well-known OTP accepted in demo mode when the backend is offline. */
        const val MOCK_OTP = "123456"

        /** Prefix used to recognise locally issued demo tokens. */
        const val MOCK_TOKEN_PREFIX = "mock-token-"
    }
}

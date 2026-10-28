package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.repository.AuthRepository
import com.secondhandmarket.data.repository.ProfileResult
import com.secondhandmarket.data.repository.RegisterResult
import com.secondhandmarket.data.repository.VerifyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val phoneNumber: String = "",
    val isOtpSent: Boolean = false,
    /** Shown only when the backend is unreachable and a demo OTP is used. */
    val mockOtp: String? = null,
    val isVerified: Boolean = false,
    val isProfileSaved: Boolean = false,
    val error: String? = null
)

/**
 * Drives the whole authentication flow: phone registration (mock OTP),
 * OTP verification (JWT) and the profile setup form.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value, error = null) }
    }

    /** POST /api/auth/register - send a mock OTP to the phone number. */
    fun register() {
        val phone = _uiState.value.phoneNumber.trim()
        if (phone.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter your phone number") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(phone)) {
                is RegisterResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOtpSent = true,
                        mockOtp = result.mockOtp,
                        error = null
                    )
                }
                is RegisterResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    /** Resend OTP - repeats the register call for the given phone number. */
    fun resendOtp(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        _uiState.update { it.copy(phoneNumber = phoneNumber) }
        register()
    }

    /** POST /api/auth/verify-otp - mock verify, receive a JWT. */
    fun verifyOtp(phoneNumber: String, otpCode: String) {
        if (otpCode.length != OTP_LENGTH) {
            _uiState.update { it.copy(error = "Enter the complete $OTP_LENGTH-digit code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyOtp(phoneNumber, otpCode)) {
                is VerifyResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isVerified = true, error = null)
                }
                is VerifyResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    /** POST /api/auth/profile - save Name, State, LGA and Address. */
    fun saveProfile(fullName: String, state: String, lga: String, address: String) {
        if (fullName.isBlank() || state.isBlank() || lga.isBlank() || address.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.updateProfile(fullName, state, lga, address)) {
                is ProfileResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isProfileSaved = true, error = null)
                }
                is ProfileResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        const val OTP_LENGTH = 6
    }
}

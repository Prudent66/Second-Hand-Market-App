package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpUiState(
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _otpUiState = MutableStateFlow(OtpUiState())
    val otpUiState: StateFlow<OtpUiState> = _otpUiState.asStateFlow()

    fun sendOtp(phoneNumber: String) {
        viewModelScope.launch {
            _otpUiState.value = _otpUiState.value.copy(isLoading = true, error = null)
            try {
                val success = authRepository.sendOtp(phoneNumber)
                if (success) {
                    _otpUiState.value = _otpUiState.value.copy(
                        isLoading = false,
                        isOtpSent = true
                    )
                } else {
                    _otpUiState.value = _otpUiState.value.copy(
                        isLoading = false,
                        error = "Failed to send OTP"
                    )
                }
            } catch (e: Exception) {
                _otpUiState.value = _otpUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun verifyOtp(phoneNumber: String, otpCode: String) {
        viewModelScope.launch {
            _otpUiState.value = _otpUiState.value.copy(isLoading = true, error = null)
            try {
                val success = authRepository.verifyOtp(phoneNumber, otpCode)
                if (success) {
                    _otpUiState.value = _otpUiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                } else {
                    _otpUiState.value = _otpUiState.value.copy(
                        isLoading = false,
                        error = "Invalid OTP code"
                    )
                }
            } catch (e: Exception) {
                _otpUiState.value = _otpUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun resetOtpState() {
        _otpUiState.value = OtpUiState()
    }
}
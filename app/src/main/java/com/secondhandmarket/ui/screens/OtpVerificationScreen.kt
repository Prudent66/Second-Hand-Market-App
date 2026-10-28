package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondhandmarket.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private const val OTP_DIGITS = 6
private const val RESEND_TIMEOUT_SECONDS = 60

/**
 * OTP verification: six single-digit fields with automatic forward focus,
 * a Verify button and a countdown-gated "Resend OTP" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cells = remember { mutableStateListOf(*Array(OTP_DIGITS) { "" }) }
    val focusRequesters = remember { List(OTP_DIGITS) { FocusRequester() } }
    val isCodeComplete = cells.all { it.isNotEmpty() }

    var resendAttempts by remember { mutableIntStateOf(0) }
    var secondsUntilResend by remember { mutableIntStateOf(RESEND_TIMEOUT_SECONDS) }

    // Focus the first cell as soon as the screen appears.
    LaunchedEffect(Unit) {
        focusRequesters.first().requestFocus()
    }

    // Resend countdown timer.
    LaunchedEffect(resendAttempts) {
        var remaining = RESEND_TIMEOUT_SECONDS
        secondsUntilResend = remaining
        while (remaining > 0) {
            delay(1000)
            remaining--
            secondsUntilResend = remaining
        }
    }

    // Move on as soon as the code is verified.
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verify your number",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit code sent to $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(OTP_DIGITS) { index ->
                OtpCell(
                    value = cells[index],
                    focusRequester = focusRequesters[index],
                    enabled = !uiState.isLoading,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        when {
                            digits.isEmpty() -> {
                                // Content was deleted - clear this cell.
                                cells[index] = ""
                            }
                            digits.length == 1 -> {
                                // Auto-focus the next cell on input.
                                cells[index] = digits
                                if (index < OTP_DIGITS - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                            else -> {
                                // Paste support: distribute digits from this cell onward.
                                digits.take(OTP_DIGITS - index).forEachIndexed { offset, c ->
                                    cells[index + offset] = c.toString()
                                }
                                val target = (index + digits.length).coerceAtMost(OTP_DIGITS - 1)
                                focusRequesters[target].requestFocus()
                            }
                        }
                    },
                    onBackspace = {
                        // Backspace in an empty cell moves focus backwards.
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Verify button
        Button(
            onClick = { viewModel.verifyOtp(phoneNumber, cells.joinToString(separator = "")) },
            enabled = isCodeComplete && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Verify", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Resend OTP with countdown
        if (secondsUntilResend > 0) {
            Text(
                text = "Resend OTP in ${secondsUntilResend}s",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            TextButton(onClick = {
                cells.indices.forEach { cells[it] = "" }
                resendAttempts++
                focusRequesters.first().requestFocus()
                viewModel.resendOtp(phoneNumber)
            }) {
                Text(
                    text = "Resend OTP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Demo hint when the backend is unreachable and a mock OTP is used.
        uiState.mockOtp?.let { mockOtp ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Demo mode — use OTP $mockOtp",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OtpCell(
    value: String,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(60.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (value.isEmpty()) 1.dp else 2.dp,
                color = if (value.isEmpty()) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                        onBackspace()
                        true
                    } else {
                        false
                    }
                },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

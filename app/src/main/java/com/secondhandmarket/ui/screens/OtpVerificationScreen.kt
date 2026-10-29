package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.secondhandmarket.ui.components.BrandGradient
import com.secondhandmarket.ui.components.GradientButton
import com.secondhandmarket.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private const val OTP_DIGITS = 6
private const val RESEND_TIMEOUT_SECONDS = 60

/**
 * OTP verification: six adaptive single-digit cells with automatic forward
 * focus, a gradient Verify button and a countdown-gated resend action.
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
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
            delay(1000L)
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
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "STEP 2 OF 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(BrandGradient.vertical(), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Verify your number",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit code we sent to",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = phoneNumber,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Demo hint: the backend is offline, so no SMS is actually sent and a
        // locally generated code is accepted. Tap to autofill the cells.
        uiState.mockOtp?.let { mockOtp ->
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "Demo mode - no SMS sent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap fill to use code $mockOtp",
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = {
                        mockOtp.filter { it.isDigit() }.take(OTP_DIGITS)
                            .forEachIndexed { index, c ->
                                cells[index] = c.toString()
                            }
                    }) {
                        Text(text = "Fill", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(OTP_DIGITS) { index ->
                OtpCell(
                    value = cells[index],
                    focusRequester = focusRequesters[index],
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f),
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        when {
                            digits.isEmpty() -> {
                                cells[index] = ""
                            }
                            digits.length == 1 -> {
                                cells[index] = digits
                                if (index < OTP_DIGITS - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                            else -> {
                                digits.take(OTP_DIGITS - index).forEachIndexed { offset, c ->
                                    cells[index + offset] = c.toString()
                                }
                                val target = (index + digits.length).coerceAtMost(OTP_DIGITS - 1)
                                focusRequesters[target].requestFocus()
                            }
                        }
                    },
                    onBackspace = {
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        GradientButton(
            text = "Verify",
            onClick = { viewModel.verifyOtp(phoneNumber, cells.joinToString(separator = "")) },
            enabled = isCodeComplete && !uiState.isLoading,
            loading = uiState.isLoading
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (secondsUntilResend > 0) {
                Text(
                    text = "Resend code in ${secondsUntilResend}s",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = {
                    cells.indices.forEach { cells[it] = "" }
                    resendAttempts++
                    focusRequesters.first().requestFocus()
                    viewModel.resendOtp(phoneNumber)
                }) {
                    Text(
                        text = "Resend code",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OtpCell(
    value: String,
    focusRequester: FocusRequester,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val active = focused || value.isNotEmpty()
    val borderColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .height(62.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
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
            interactionSource = interaction,
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

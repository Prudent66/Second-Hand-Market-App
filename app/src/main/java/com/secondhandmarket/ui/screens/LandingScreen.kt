package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.secondhandmarket.viewmodel.AuthViewModel

// Placeholder photo of people trading second-hand items (laptop/furniture swap).
private const val LANDING_BACKGROUND_URL =
    "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1200&q=80"

/**
 * Solid dark blue used while the background photo loads (and if it fails).
 * Note: painterResource() cannot be used here - bg_placeholder.xml is a
 * <shape> drawable and painterResource only supports vectors/rasters.
 */
private val BackgroundPlaceholder = ColorPainter(Color(0xFF0D2A4A))

/**
 * Landing screen: full-bleed photo of a second-hand market with a dark
 * gradient overlay, the app title and the phone-number entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onOtpSent: (phoneNumber: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to OTP verification as soon as the OTP has been dispatched.
    // Consume the flag right after so returning here with the back button
    // shows a fresh landing screen instead of bouncing forward again.
    LaunchedEffect(uiState.isOtpSent) {
        if (uiState.isOtpSent) {
            onOtpSent(uiState.phoneNumber.trim())
            viewModel.consumeOtpSent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Full-screen background image placeholder (Coil).
        AsyncImage(
            model = LANDING_BACKGROUND_URL,
            contentDescription = "People trading second-hand items",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = BackgroundPlaceholder,
            error = BackgroundPlaceholder
        )

        // Semi-transparent black gradient for text readability.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Second Hand Market",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Buy & sell pre-loved items: laptops, furniture, fashion and more.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Phone number entry
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number") },
                placeholder = { Text("+2348012345678") },
                leadingIcon = {
                    Text(
                        text = "📞",
                        fontSize = 18.sp
                    )
                },
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button
            Button(
                onClick = viewModel::register,
                enabled = !uiState.isLoading &&
                    uiState.phoneNumber.filter { it.isDigit() }.length >= 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You'll get a 6-digit code to continue. No server? Demo mode shows it on the next screen.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

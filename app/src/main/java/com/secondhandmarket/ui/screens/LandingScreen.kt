package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.secondhandmarket.ui.components.BrandGradient
import com.secondhandmarket.ui.components.GradientButton
import com.secondhandmarket.ui.theme.AquaSoft
import com.secondhandmarket.ui.theme.OceanDeep
import com.secondhandmarket.viewmodel.AuthViewModel

// Photo of people browsing and trading second-hand goods at a market.
private const val LANDING_BACKGROUND_URL =
    "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1200&q=80"

/**
 * Landing screen: a full-bleed photo of a second-hand market with a
 * transparent-to-black gradient, the brand lockup and the phone number entry.
 */
@Composable
fun LandingScreen(
    onOtpSent: (phoneNumber: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigate to OTP verification as soon as the OTP has been dispatched.
    // Consume the flag right after so returning here with the back button
    // shows a fresh landing screen instead of bouncing forward again.
    LaunchedEffect(uiState.isOtpSent) {
        if (uiState.isOtpSent) {
            onOtpSent(uiState.phoneNumber.trim())
            viewModel.consumeOtpSent()
        }
    }

    val imageRequest = remember {
        ImageRequest.Builder(context)
            .data(LANDING_BACKGROUND_URL)
            .crossfade(true)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize().background(OceanDeep)) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "People trading second-hand items",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Transparent at the top so the photo breathes, solid at the bottom so
        // the headline and form stay perfectly readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.35f to Color.Black.copy(alpha = 0.25f),
                        0.62f to Color.Black.copy(alpha = 0.72f),
                        1f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AquaSoft, RoundedCornerShape(percent = 50))
                    )
                    Text(
                        text = "  LOCAL MARKETPLACE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Second Hand",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Text(
                text = "Market.",
                style = MaterialTheme.typography.displaySmall,
                color = AquaSoft
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Quality pre-loved finds near you. Buy smart, sell fast, and give good things a second life.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number") },
                placeholder = { Text("0801 234 5678") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = AquaSoft,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AquaSoft,
                    focusedBorderColor = AquaSoft,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    focusedLabelColor = AquaSoft,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color.White.copy(alpha = 0.10f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.10f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = "Continue",
                onClick = viewModel::register,
                enabled = !uiState.isLoading &&
                    uiState.phoneNumber.filter { it.isDigit() }.length >= 10,
                loading = uiState.isLoading
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error.orEmpty(),
                    color = Color(0xFFFFC2C2),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "We will text you a 6-digit verification code.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Slim brand bar pinned to the top of the photo.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(BrandGradient.horizontal(), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SH",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "  pre-loved, re-loved",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
        }
    }
}

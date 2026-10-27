package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.secondhandmarket.data.models.OtpState
import com.secondhandmarket.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun OtpLoginScreen(
    authViewModel: AuthViewModel,
    onOtpVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.otpUiState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // Background Image
        AsyncImage(
            model = "file:///android_asset/images/backgrounds/login_bg.jpg",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // Semi-transparent overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Phone Number",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                    focusedBorderColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    textColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                enabled = !uiState.isLoading
            )

            Button(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        scope.launch {
                            authViewModel.sendOtp(phoneNumber)
                        }
                    }
                },
                enabled = !uiState.isLoading && phoneNumber.length >= 10,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Send OTP", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isOtpSent) {
                Text(
                    text = "Enter Verification Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("OTP Code") },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        cursorColor = Color.White,
                        textColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    enabled = !uiState.isLoading
                )

                Button(
                    onClick = {
                        if (otpCode.isNotEmpty()) {
                            scope.launch {
                                authViewModel.verifyOtp(phoneNumber, otpCode)
                            }
                        }
                    },
                    enabled = !uiState.isLoading && otpCode.length == 6,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Verify & Continue", fontSize = 16.sp)
                }
            }

            // Error Message
            uiState.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Success Navigation
            LaunchedEffect(uiState.isAuthenticated) {
                if (uiState.isAuthenticated) {
                    onOtpVerified()
                }
            }
        }
    }
}
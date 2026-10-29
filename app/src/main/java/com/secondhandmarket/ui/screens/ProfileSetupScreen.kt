package com.secondhandmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondhandmarket.ui.components.BrandGradient
import com.secondhandmarket.ui.components.GradientButton
import com.secondhandmarket.viewmodel.AuthViewModel

/**
 * Profile setup: collects Name, State, LGA and Address, then saves them to
 * the user record through the auth API before entering the dashboard.
 */
@Composable
fun ProfileSetupScreen(
    onCompleted: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var lga by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Move on as soon as the profile is saved. Consume the flag right after
    // so pressing back from the dashboard lands on a fresh setup screen
    // instead of instantly navigating forward again.
    LaunchedEffect(uiState.isProfileSaved) {
        if (uiState.isProfileSaved) {
            onCompleted()
            viewModel.consumeProfileSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row3StepBadge()

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .background(BrandGradient.vertical(), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Complete your profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Tell buyers where you are so they can find items near them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full name",
            enabled = !uiState.isLoading,
            capitalization = KeyboardCapitalization.Words
        )
        ProfileField(
            value = state,
            onValueChange = { state = it },
            label = "State",
            placeholder = "e.g. Gombe",
            enabled = !uiState.isLoading,
            capitalization = KeyboardCapitalization.Words
        )
        ProfileField(
            value = lga,
            onValueChange = { lga = it },
            label = "LGA (Local Government Area)",
            placeholder = "e.g. Akko",
            enabled = !uiState.isLoading,
            capitalization = KeyboardCapitalization.Words
        )
        ProfileField(
            value = address,
            onValueChange = { address = it },
            label = "Address",
            placeholder = "Street, area, landmark",
            enabled = !uiState.isLoading,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(10.dp))

        GradientButton(
            text = "Complete setup",
            onClick = { viewModel.saveProfile(fullName, state, lga, address) },
            enabled = !uiState.isLoading &&
                fullName.isNotBlank() &&
                state.isNotBlank() &&
                lga.isNotBlank() &&
                address.isNotBlank(),
            loading = uiState.isLoading
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun Row3StepBadge() {
    Text(
        text = "STEP 3 OF 3",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    capitalization: KeyboardCapitalization,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder.orEmpty()) }
        } else {
            null
        },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

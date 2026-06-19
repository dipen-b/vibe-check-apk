package com.vibecheck.app.ui.friendship

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.launch

@Composable
fun FriendshipScreen(container: AppContainer) {
    var step by remember { mutableStateOf(FriendshipStep.PHONE_INPUT) } // Phone input, OTP, Profile, Friends
    var countryCode by remember { mutableStateOf(COUNTRIES[0]) }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val currentUserIdFlow = container.friendshipRepository.getCurrentUserId()
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(currentUserIdFlow.value) {
        currentUserIdFlow.value?.let {
            currentUserId = it
            step = FriendshipStep.FRIENDS_LIST
        }
    }

    when (step) {
        FriendshipStep.PHONE_INPUT -> {
            PhoneInputScreen(
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it },
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                loading = loading,
                error = error,
                onSendOTP = {
                    if (phoneNumber.isBlank()) {
                        error = "Please enter a phone number"
                        return@PhoneInputScreen
                    }
                    error = ""
                    loading = true
                    scope.launch {
                        container.friendshipRepository.sendOTP(phoneNumber, countryCode.dialCode)
                            .onSuccess {
                                step = FriendshipStep.OTP_VERIFICATION
                                loading = false
                            }
                            .onFailure { e ->
                                error = e.message ?: "Failed to send OTP"
                                loading = false
                            }
                    }
                },
            )
        }

        FriendshipStep.OTP_VERIFICATION -> {
            OTPVerificationScreen(
                otp = otp,
                onOtpChange = { otp = it },
                loading = loading,
                error = error,
                onVerifyOTP = {
                    if (otp.isBlank()) {
                        error = "Please enter OTP"
                        return@OTPVerificationScreen
                    }
                    error = ""
                    loading = true
                    scope.launch {
                        container.friendshipRepository.verifyOTP(phoneNumber, otp)
                            .onSuccess { userId ->
                                currentUserId = userId
                                step = FriendshipStep.PROFILE_CREATION
                                loading = false
                            }
                            .onFailure { e ->
                                error = e.message ?: "Invalid OTP"
                                loading = false
                            }
                    }
                },
                onBackClick = {
                    step = FriendshipStep.PHONE_INPUT
                    otp = ""
                    error = ""
                },
            )
        }

        FriendshipStep.PROFILE_CREATION -> {
            ProfileCreationScreen(
                firstName = firstName,
                onFirstNameChange = { firstName = it },
                lastName = lastName,
                onLastNameChange = { lastName = it },
                loading = loading,
                error = error,
                onCreateProfile = {
                    if (firstName.isBlank() || lastName.isBlank()) {
                        error = "Please fill in all fields"
                        return@ProfileCreationScreen
                    }
                    error = ""
                    loading = true
                    scope.launch {
                        container.friendshipRepository.createUserProfile(
                            currentUserId, firstName, lastName
                        )
                            .onSuccess {
                                step = FriendshipStep.FRIENDS_LIST
                                loading = false
                            }
                            .onFailure { e ->
                                error = e.message ?: "Failed to create profile"
                                loading = false
                            }
                    }
                },
            )
        }

        FriendshipStep.FRIENDS_LIST -> {
            FriendsListScreen(container = container)
        }
    }
}

enum class FriendshipStep {
    PHONE_INPUT,
    OTP_VERIFICATION,
    PROFILE_CREATION,
    FRIENDS_LIST,
}

@Composable
private fun PhoneInputScreen(
    countryCode: Country,
    onCountryCodeChange: (Country) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    loading: Boolean,
    error: String,
    onSendOTP: () -> Unit,
) {
    var showCountryDropdown by remember { mutableStateOf(false) }
    var countrySearch by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            "Find Your Friends",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Violet,
        )

        Text(
            "Enter your phone number to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        // Country Code Selector
        Box {
            OutlinedButton(
                onClick = { showCountryDropdown = !showCountryDropdown },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${countryCode.flag} ${countryCode.dialCode}", Modifier.weight(1f), textAlign = TextAlign.Start)
                Text("▼")
            }

            if (showCountryDropdown) {
                Dialog(onDismissRequest = { showCountryDropdown = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = countrySearch,
                                onValueChange = { countrySearch = it },
                                placeholder = { Text("Search countries...") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(12.dp))

                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                val filtered = COUNTRIES.filter { c ->
                                    c.name.contains(countrySearch, ignoreCase = true) ||
                                            c.dialCode.contains(countrySearch)
                                }

                                items(filtered.size) { idx ->
                                    val c = filtered[idx]
                                    Text(
                                        "${c.flag} ${c.name} (${c.dialCode})",
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onCountryCodeChange(c)
                                                showCountryDropdown = false
                                                countrySearch = ""
                                            }
                                            .padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Phone Number Input
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(countryCode.dialCode, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { onPhoneNumberChange(it.take(15)) },
                placeholder = { Text("9876543210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
            )
        }

        if (error.isNotEmpty()) {
            Text(
                error,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSendOTP,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Send OTP")
            }
        }
    }
}

@Composable
private fun OTPVerificationScreen(
    otp: String,
    onOtpChange: (String) -> Unit,
    loading: Boolean,
    error: String,
    onVerifyOTP: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            "Verify OTP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Violet,
        )

        Text(
            "Enter the 6-digit code sent to your phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { onOtpChange(it.take(6)) },
            placeholder = { Text("000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onVerifyOTP,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Verify OTP")
            }
        }

        OutlinedButton(
            onClick = onBackClick,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ProfileCreationScreen(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    loading: Boolean,
    error: String,
    onCreateProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            "Create Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Violet,
        )

        Text(
            "Let others find you by name",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            placeholder = { Text("John") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            placeholder = { Text("Doe") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onCreateProfile,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create Profile")
            }
        }
    }
}

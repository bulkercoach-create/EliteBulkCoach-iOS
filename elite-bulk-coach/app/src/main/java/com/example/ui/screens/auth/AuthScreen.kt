package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthStep {
    CREDENTIALS,
    TERMS_ACCEPTANCE,
    OTP_VERIFICATION,
    FORGOT_PASSWORD_REQUEST_EMAIL,
    FORGOT_PASSWORD_VERIFY_CODE,
    FORGOT_PASSWORD_SET_NEW_PASSWORD
}

@Composable
fun AuthScreen(
    viewModel: BulkCoachViewModel? = null,
    onLoginSuccess: (email: String, name: String) -> Unit,
    onStartOnboarding: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var authStep by remember { mutableStateOf(AuthStep.CREDENTIALS) }
    var isRegisterMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password & Reset State
    var resetEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmNewPasswordVisible by remember { mutableStateOf(false) }

    // OTP State
    var otpCode by remember { mutableStateOf("") }
    var lastGeneratedOtp by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var showEmailNotificationBanner by remember { mutableStateOf(true) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var resetEmailError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Mandatory AGB / Terms of Service State
    var hasScrolledToEndOfTerms by remember { mutableStateOf(false) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var termsError by remember { mutableStateOf<String?>(null) }

    // Cooldown timer effect
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    fun validateCredentials(): Boolean {
        var isValid = true
        val cleanEmail = email.trim()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        if (isRegisterMode) {
            if (name.trim().isBlank()) {
                nameError = "Bitte gib deinen Namen ein."
                isValid = false
            } else {
                nameError = null
            }
        } else {
            nameError = null
        }
        termsError = null

        if (cleanEmail.isBlank()) {
            emailError = "E-Mail-Adresse darf nicht leer sein."
            isValid = false
        } else if (!emailRegex.matches(cleanEmail)) {
            emailError = "Bitte gib eine gültige E-Mail-Adresse ein."
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Passwort darf nicht leer sein."
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Passwort muss mindestens 6 Zeichen lang sein."
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    fun validateResetEmail(): Boolean {
        val cleanEmail = resetEmail.trim()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return if (cleanEmail.isBlank()) {
            resetEmailError = "E-Mail-Adresse darf nicht leer sein."
            false
        } else if (!emailRegex.matches(cleanEmail)) {
            resetEmailError = "Bitte gib eine gültige E-Mail-Adresse ein."
            false
        } else {
            resetEmailError = null
            true
        }
    }

    fun validateNewPasswordForm(): Boolean {
        var isValid = true
        if (newPassword.isBlank()) {
            newPasswordError = "Neues Passwort darf nicht leer sein."
            isValid = false
        } else if (newPassword.length < 6) {
            newPasswordError = "Passwort muss mindestens 6 Zeichen lang sein."
            isValid = false
        } else {
            newPasswordError = null
        }

        if (confirmNewPassword != newPassword) {
            confirmPasswordError = "Passwörter stimmen nicht überein."
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ==========================================
            // STEP 1: CREDENTIALS (REGISTER / LOGIN)
            // ==========================================
            if (authStep == AuthStep.CREDENTIALS) {
                // App Header Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ELITE BULK COACH",
                        color = PrimaryLilac,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRegisterMode) "Konto erstellen" else "Willkommen zurück",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRegisterMode) 
                            "Erstelle dein Profil für personalisiertes Coaching & Tracking" 
                        else 
                            "Melde dich an, um auf dein Coaching & Daten zuzugreifen",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Success Message Banner (e.g. after password reset)
                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryLilac.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryLilac,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = successMessage ?: "",
                                color = PrimaryLilac,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error Banner
                AnimatedVisibility(
                    visible = authError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Fehler",
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = authError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name Field (only in Sign Up mode)
                    if (isRegisterMode) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = null
                                    authError = null
                                },
                                label = { Text("Dein Name", color = TextMuted) },
                                placeholder = { Text("z.B. Alex Müller", color = TextMuted.copy(alpha = 0.5f)) },
                                leadingIcon = { 
                                    Icon(Icons.Default.Person, contentDescription = null, tint = if (nameError != null) AccentBufferCoral else TextMuted) 
                                },
                                isError = nameError != null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_name"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceCard,
                                    unfocusedContainerColor = DarkSurfaceCard,
                                    focusedBorderColor = PrimaryLilac,
                                    unfocusedBorderColor = DarkOutlineVariant,
                                    errorBorderColor = AccentBufferCoral
                                )
                            )
                            if (nameError != null) {
                                Text(
                                    text = nameError ?: "",
                                    color = AccentBufferCoral,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                                authError = null
                            },
                            label = { Text("E-Mail-Adresse", color = TextMuted) },
                            placeholder = { Text("name@beispiel.de", color = TextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { 
                                Icon(Icons.Default.Mail, contentDescription = null, tint = if (emailError != null) AccentBufferCoral else TextMuted) 
                            },
                            isError = emailError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                    .fillMaxWidth()
                                .testTag("input_email"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard,
                                focusedBorderColor = PrimaryLilac,
                                unfocusedBorderColor = DarkOutlineVariant,
                                errorBorderColor = AccentBufferCoral
                            )
                        )
                        if (emailError != null) {
                            Text(
                                text = emailError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                authError = null
                            },
                            label = { Text("Passwort", color = TextMuted) },
                            placeholder = { Text("mind. 6 Zeichen", color = TextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { 
                                Icon(Icons.Default.Lock, contentDescription = null, tint = if (passwordError != null) AccentBufferCoral else TextMuted) 
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard,
                                focusedBorderColor = PrimaryLilac,
                                unfocusedBorderColor = DarkOutlineVariant,
                                errorBorderColor = AccentBufferCoral
                            )
                        )
                        if (passwordError != null) {
                            Text(
                                text = passwordError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }

                    // "Passwort vergessen?" Button (visible in Login Mode)
                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmail = email.trim()
                                    authStep = AuthStep.FORGOT_PASSWORD_REQUEST_EMAIL
                                    authError = null
                                    resetEmailError = null
                                    otpError = null
                                    successMessage = null
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_forgot_password")
                            ) {
                                Text(
                                    text = "Passwort vergessen?",
                                    color = PrimaryLilac,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Action Button (Proceed to AGB in Register Mode, or Instant Login in Login Mode)
                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (!validateCredentials()) return@Button

                            if (isRegisterMode) {
                                isLoading = true
                                authError = null
                                coroutineScope.launch {
                                    val cleanEmail = email.trim().lowercase()
                                    try {
                                        val checkResult = viewModel?.checkEmailAvailable(cleanEmail)
                                        if (checkResult != null && checkResult.isFailure) {
                                            authError = checkResult.exceptionOrNull()?.message ?: "Diese E-Mail wird bereits verwendet."
                                        } else {
                                            // Validated credentials and available email -> go to AGB step!
                                            hasScrolledToEndOfTerms = false
                                            hasAcceptedTerms = false
                                            termsError = null
                                            authStep = AuthStep.TERMS_ACCEPTANCE
                                        }
                                    } catch (e: Exception) {
                                        authError = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                isLoading = true
                                authError = null
                                coroutineScope.launch {
                                    val cleanEmail = email.trim().lowercase()
                                    val cleanName = name.trim().ifBlank { "Athlet" }
                                    try {
                                        val res = viewModel?.login(cleanEmail, password)
                                        if (res != null && res.isSuccess) {
                                            val prof = res.getOrNull()
                                            onLoginSuccess(cleanEmail, prof?.name ?: cleanName)
                                        } else {
                                            authError = res?.exceptionOrNull()?.message ?: "E-Mail oder Passwort falsch."
                                        }
                                    } catch (e: Exception) {
                                        authError = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_auth_submit"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLilac,
                            contentColor = PrimaryLilacDark,
                            disabledContainerColor = DarkSurfaceCard,
                            disabledContentColor = TextMuted.copy(alpha = 0.5f)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = PrimaryLilacDark,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRegisterMode) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isRegisterMode) "Weiter" else "Jetzt Einloggen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Switch Mode Link
                    Row(
                        modifier = Modifier
                            .clickable {
                                isRegisterMode = !isRegisterMode
                                authError = null
                                emailError = null
                                passwordError = null
                                nameError = null
                                successMessage = null
                                name = ""
                                email = ""
                                password = ""
                            }
                            .padding(vertical = 8.dp)
                            .testTag("link_switch_auth_mode"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Bereits ein Konto? " else "Noch kein Konto? ",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isRegisterMode) "Jetzt einloggen" else "Jetzt registrieren",
                            color = PrimaryLilac,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // STEP 2: TERMS OF SERVICE & DISCLAIMER (AGB & HAFTUNGSAUSSCHLUSS)
            // ==========================================
            else if (authStep == AuthStep.TERMS_ACCEPTANCE) {
                val termsScrollState = rememberScrollState()
                val isScrolledToBottom by remember {
                    derivedStateOf {
                        val max = termsScrollState.maxValue
                        max > 0 && termsScrollState.value >= (max - 24)
                    }
                }

                LaunchedEffect(isScrolledToBottom) {
                    if (isScrolledToBottom) {
                        hasScrolledToEndOfTerms = true
                    }
                }

                // Top Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHRITT 2 VON 2",
                        color = PrimaryLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "AGB & Haftungsausschluss",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bitte scrolle den Text vollständig nach unten durch, um die Bedingungen zu akzeptieren.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Error Banner
                AnimatedVisibility(
                    visible = authError != null || termsError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .border(1.dp, AccentBufferCoral.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = (authError ?: termsError) ?: "",
                                color = AccentBufferCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // AGB Text Container (Already open and scrollable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141C28))
                        .border(1.dp, Color(0xFF222F43), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(termsScrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ALLGEMEINE GESCHÄFTSBEDINGUNGEN & HAFTUNGSAUSSCHLUSS (AXON App)",
                            color = PrimaryLilac,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )

                        TermsSectionItem(
                            number = "1.",
                            title = "Nutzung auf eigene Gefahr:",
                            content = "Die Nutzung der App 'AXON' erfolgt ausdrücklich und vollständig auf eigene Verantwortung und eigene Gefahr des Nutzers."
                        )

                        TermsSectionItem(
                            number = "2.",
                            title = "Kein medizinischer oder sportlicher Rat:",
                            content = "Alle in der App bereitgestellten Informationen, Kalorienberechnungen, Nährstoffverteilungen, KI-Generierungen, Trainingspläne und Empfehlungen stellen weder eine medizinische, gesundheitliche noch eine professionelle Ernährungs- oder Sportberatung dar. Bei gesundheitlichen Beschwerden, Einschränkungen oder Vorerkrankungen ist vor der Nutzung ein Arzt oder Fachpersonal zu konsultieren."
                        )

                        TermsSectionItem(
                            number = "3.",
                            title = "Haftungsausschluss:",
                            content = "Der Entwickler übernimmt keinerlei Haftung für Schäden jeglicher Art (z. B. gesundheitliche, körperliche, materielle oder finanzielle Schäden), die aus der Nutzung der App, fehlerhaften Nährwertangaben, fehlerhaften KI-Analysen oder der Umsetzung von Trainings- und Ernährungsempfehlungen entstehen."
                        )

                        TermsSectionItem(
                            number = "4.",
                            title = "Datengenauigkeit:",
                            content = "Für die absolute Richtigkeit, Vollständigkeit und Aktualität der angezeigten Lebensmittel-, Barcode-, KI- oder Kaloriendaten wird keine Gewähr übernommen."
                        )

                        Spacer(Modifier.height(8.dp))

                        // Bottom reached badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E2A3C))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryLilac,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Ende des Rechtstextes erreicht.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Scroll Lock status hint if not scrolled to bottom
                if (!hasScrolledToEndOfTerms && !isScrolledToBottom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryLilac.copy(alpha = 0.12f))
                            .border(1.dp, PrimaryLilac.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Bitte scrolle den Text bis ganz nach unten, um fortzufahren.",
                            color = PrimaryLilac,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Checkbox Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceCard)
                        .border(
                            width = 1.dp,
                            color = if (termsError != null) AccentBufferCoral else if (hasAcceptedTerms) PrimaryLilac.copy(alpha = 0.5f) else DarkOutlineVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (hasScrolledToEndOfTerms) {
                                hasAcceptedTerms = !hasAcceptedTerms
                                if (hasAcceptedTerms) termsError = null
                            } else {
                                termsError = "Bitte scrolle erst den Text ganz nach unten durch."
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = hasAcceptedTerms,
                        onCheckedChange = { isChecked ->
                            if (hasScrolledToEndOfTerms) {
                                hasAcceptedTerms = isChecked
                                if (isChecked) termsError = null
                            } else {
                                termsError = "Bitte scrolle erst den Text ganz nach unten durch."
                            }
                        },
                        enabled = hasScrolledToEndOfTerms,
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryLilac,
                            uncheckedColor = if (hasScrolledToEndOfTerms) TextMuted else TextMuted.copy(alpha = 0.3f),
                            checkmarkColor = PrimaryLilacDark,
                            disabledCheckedColor = PrimaryLilac.copy(alpha = 0.5f),
                            disabledUncheckedColor = TextMuted.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("checkbox_accept_terms")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ich habe die AGB gelesen und akzeptiere die Nutzung auf eigene Gefahr.",
                            color = if (hasScrolledToEndOfTerms) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontWeight = if (hasAcceptedTerms) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (!hasScrolledToEndOfTerms) {
                            Text(
                                text = "⚠️ Erst nach vollständigem Durchscrollen freigeschaltet",
                                color = PrimaryLilac.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Complete Registration Button
                Button(
                    onClick = {
                        if (!hasAcceptedTerms) {
                            termsError = "Bitte bestätige die Checkbox, um fortzufahren."
                            return@Button
                        }
                        isLoading = true
                        authError = null
                        termsError = null

                        coroutineScope.launch {
                            val cleanEmail = email.trim().lowercase()
                            val cleanName = name.trim().ifBlank { "Athlet" }

                            try {
                                val res = viewModel?.register(cleanName, cleanEmail, password)
                                if (res != null && res.isSuccess) {
                                    onLoginSuccess(cleanEmail, cleanName)
                                } else {
                                    authError = res?.exceptionOrNull()?.message ?: "Registrierung fehlgeschlagen."
                                }
                            } catch (e: Exception) {
                                authError = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_complete_registration"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark,
                        disabledContainerColor = DarkSurfaceCard,
                        disabledContentColor = TextMuted.copy(alpha = 0.5f)
                    ),
                    enabled = !isLoading && hasAcceptedTerms
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryLilacDark,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Zustimmen & Konto erstellen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Back to credentials button
                OutlinedButton(
                    onClick = {
                        authStep = AuthStep.CREDENTIALS
                        authError = null
                        termsError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_back_to_credentials"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Zurück zu Name & E-Mail", fontSize = 14.sp)
                    }
                }
            }

            // ==========================================
            // STEP 2: FORGOT PASSWORD - SCHRITT 1: E-MAIL EINGABE
            // ==========================================
            else if (authStep == AuthStep.FORGOT_PASSWORD_REQUEST_EMAIL) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHRITT 1 VON 3",
                        color = PrimaryLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Passwort vergessen?",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Gib deine registrierte E-Mail-Adresse ein. Wir senden dir einen 6-stelligen Code zur Bestätigung.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Error Banner
                AnimatedVisibility(
                    visible = authError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Fehler",
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = authError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Reset Email Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetEmailError = null
                            authError = null
                        },
                        label = { Text("Registrierte E-Mail-Adresse", color = TextMuted) },
                        placeholder = { Text("name@beispiel.de", color = TextMuted.copy(alpha = 0.5f)) },
                        leadingIcon = { 
                            Icon(Icons.Default.Mail, contentDescription = null, tint = if (resetEmailError != null) AccentBufferCoral else PrimaryLilac) 
                        },
                        isError = resetEmailError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reset_email"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard,
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = DarkOutlineVariant,
                            errorBorderColor = AccentBufferCoral
                        )
                    )
                    if (resetEmailError != null) {
                        Text(
                            text = resetEmailError ?: "",
                            color = AccentBufferCoral,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                // Request Code Button (Sends email and navigates to verify screen without logging in)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (!validateResetEmail()) return@Button

                        isLoading = true
                        authError = null

                        coroutineScope.launch {
                            val cleanEmail = resetEmail.trim().lowercase()
                            try {
                                val result = viewModel?.sendPasswordResetCode(cleanEmail)
                                if (result != null && result.isSuccess) {
                                    val code = result.getOrNull() ?: ""
                                    lastGeneratedOtp = code
                                    otpCode = ""
                                    resendCooldown = 30
                                    // Navigate to Step 2: Code Verification ONLY (no login!)
                                    authStep = AuthStep.FORGOT_PASSWORD_VERIFY_CODE
                                } else {
                                    authError = result?.exceptionOrNull()?.message ?: "Kein Konto mit dieser E-Mail gefunden."
                                }
                            } catch (e: Exception) {
                                authError = e.message ?: "Fehler beim Versenden des Codes."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_send_reset_code"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryLilacDark,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Code anfordern",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Back to Login Button
                TextButton(
                    onClick = {
                        authStep = AuthStep.CREDENTIALS
                        isRegisterMode = false
                        authError = null
                        resetEmailError = null
                    },
                    modifier = Modifier.testTag("btn_back_to_login")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                    Spacer(Modifier.width(4.dp))
                    Text("Zurück zum Login", color = TextMuted, fontSize = 14.sp)
                }
            }

            // ==========================================
            // STEP 3: FORGOT PASSWORD - SCHRITT 2: CODE BESTÄTIGEN
            // ==========================================
            else if (authStep == AuthStep.FORGOT_PASSWORD_VERIFY_CODE) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHRITT 2 VON 3",
                        color = PrimaryLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Code bestätigen",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wir haben einen 6-stelligen Code an deine E-Mail gesendet:\n${resetEmail.trim().lowercase()}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // E-Mail Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2838))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryLilac.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = PrimaryLilac,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "E-Mail versendet",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Der 6-stellige Code wurde an ${resetEmail.trim().lowercase()} gesendet. Bitte prüfe auch deinen Spam-Ordner.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Error Banner
                AnimatedVisibility(
                    visible = otpError != null || authError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = otpError ?: authError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 6-Digit Code Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(6)
                            otpCode = digitsOnly
                            otpError = null
                            authError = null
                        },
                        label = { Text("6-stelliger Code", color = TextMuted) },
                        placeholder = { Text("123456", color = TextMuted.copy(alpha = 0.5f)) },
                        leadingIcon = { 
                            Icon(Icons.Default.Security, contentDescription = null, tint = if (otpError != null) AccentBufferCoral else PrimaryLilac) 
                        },
                        isError = otpError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reset_otp"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard,
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = DarkOutlineVariant,
                            errorBorderColor = AccentBufferCoral
                        )
                    )
                    if (otpError != null) {
                        Text(
                            text = otpError ?: "",
                            color = AccentBufferCoral,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                // Confirm Code Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (otpCode.length != 6) {
                            otpError = "Bitte gib den vollständigen 6-stelligen Code ein."
                            return@Button
                        }

                        val cleanEmail = resetEmail.trim().lowercase()
                        val isValid = viewModel?.verifyPasswordResetCode(cleanEmail, otpCode) == true || otpCode == lastGeneratedOtp

                        if (isValid) {
                            authError = null
                            otpError = null
                            newPassword = ""
                            confirmNewPassword = ""
                            // Navigate to Step 3: Set New Password
                            authStep = AuthStep.FORGOT_PASSWORD_SET_NEW_PASSWORD
                        } else {
                            otpError = "Falscher oder abgelaufener Code. Bitte überprüfe deine Eingabe."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_verify_reset_code"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = !isLoading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Code bestätigen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Resend Code and Back Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            authStep = AuthStep.FORGOT_PASSWORD_REQUEST_EMAIL
                            authError = null
                            otpError = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text("E-Mail ändern", color = TextMuted, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = {
                            if (resendCooldown == 0) {
                                val cleanEmail = resetEmail.trim().lowercase()
                                coroutineScope.launch {
                                    val result = viewModel?.sendPasswordResetCode(cleanEmail)
                                    val newCode = result?.getOrNull() ?: (100000..999999).random().toString()
                                    lastGeneratedOtp = newCode
                                    resendCooldown = 30
                                    authError = null
                                    otpError = null
                                }
                            }
                        },
                        enabled = resendCooldown == 0
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (resendCooldown == 0) PrimaryLilac else TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (resendCooldown > 0) "Erneut in ${resendCooldown}s" else "Code erneut senden",
                            color = if (resendCooldown == 0) PrimaryLilac else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // STEP 4: FORGOT PASSWORD - SCHRITT 3: NEUES PASSWORT EINGEBEN
            // ==========================================
            else if (authStep == AuthStep.FORGOT_PASSWORD_SET_NEW_PASSWORD) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHRITT 3 VON 3",
                        color = PrimaryLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Neues Passwort festlegen",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wähle ein neues sicheres Passwort für dein Konto.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Error Banner
                AnimatedVisibility(
                    visible = authError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = authError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Input Fields: New Password & Confirm Password
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // New Password
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                newPasswordError = null
                                authError = null
                            },
                            label = { Text("Neues Passwort", color = TextMuted) },
                            placeholder = { Text("mind. 6 Zeichen", color = TextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { 
                                Icon(Icons.Default.Lock, contentDescription = null, tint = if (newPasswordError != null) AccentBufferCoral else TextMuted) 
                            },
                            trailingIcon = {
                                IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isNewPasswordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = newPasswordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_password"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard,
                                focusedBorderColor = PrimaryLilac,
                                unfocusedBorderColor = DarkOutlineVariant,
                                errorBorderColor = AccentBufferCoral
                            )
                        )
                        if (newPasswordError != null) {
                            Text(
                                text = newPasswordError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }

                    // Confirm New Password
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = {
                                confirmNewPassword = it
                                confirmPasswordError = null
                                authError = null
                            },
                            label = { Text("Neues Passwort bestätigen", color = TextMuted) },
                            placeholder = { Text("Passwort wiederholen", color = TextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = { 
                                Icon(Icons.Default.Lock, contentDescription = null, tint = if (confirmPasswordError != null) AccentBufferCoral else TextMuted) 
                            },
                            trailingIcon = {
                                IconButton(onClick = { isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isConfirmNewPasswordVisible) "Passwort verbergen" else "Passwort anzeigen",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (isConfirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = confirmPasswordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_confirm_password"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard,
                                focusedBorderColor = PrimaryLilac,
                                unfocusedBorderColor = DarkOutlineVariant,
                                errorBorderColor = AccentBufferCoral
                            )
                        )
                        if (confirmPasswordError != null) {
                            Text(
                                text = confirmPasswordError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // Submit Reset Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (!validateNewPasswordForm()) return@Button

                        val cleanEmail = resetEmail.trim().lowercase()
                        isLoading = true
                        authError = null

                        coroutineScope.launch {
                            try {
                                val resetRes = viewModel?.resetPassword(cleanEmail, otpCode, newPassword)
                                if (resetRes != null && resetRes.isSuccess) {
                                    // Return to login screen with prefilled values and success message (no auto-login)
                                    email = cleanEmail
                                    password = ""
                                    isRegisterMode = false
                                    successMessage = "Passwort erfolgreich geändert! Bitte logge dich mit deinem neuen Passwort ein."
                                    authStep = AuthStep.CREDENTIALS
                                } else {
                                    authError = resetRes?.exceptionOrNull()?.message ?: "Fehler beim Zurücksetzen des Passworts."
                                }
                            } catch (e: Exception) {
                                authError = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_reset_password"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryLilacDark,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Passwort speichern",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Back Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            authStep = AuthStep.FORGOT_PASSWORD_VERIFY_CODE
                            authError = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text("Zurück zur Code-Eingabe", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            // ==========================================
            // STEP 4: E-MAIL OTP VERIFICATION CODE (REGISTRATION 2FA)
            // ==========================================
            else if (authStep == AuthStep.OTP_VERIFICATION) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C1E4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SICHERHEITS-VERIFIZIERUNG",
                        color = PrimaryLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bestätigungscode eingeben",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wir haben einen 6-stelligen Code an deine E-Mail gesendet:\n${email.trim().lowercase()}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // E-Mail Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2838))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryLilac.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = PrimaryLilac,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "E-Mail Bestätigung",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Der 6-stellige Code wurde an ${email.trim().lowercase()} gesendet. Bitte prüfe auch deinen Spam-Ordner.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Error Banner for OTP
                AnimatedVisibility(
                    visible = otpError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBufferCoral.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = AccentBufferCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = otpError ?: "",
                                color = AccentBufferCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 6-Digit Code Input Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(6)
                            otpCode = digitsOnly
                            otpError = null
                        },
                        label = { Text("6-stelliger Code", color = TextMuted) },
                        placeholder = { Text("123456", color = TextMuted.copy(alpha = 0.5f)) },
                        leadingIcon = { 
                            Icon(Icons.Default.Security, contentDescription = null, tint = if (otpError != null) AccentBufferCoral else PrimaryLilac) 
                        },
                        isError = otpError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_otp_code"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard,
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = DarkOutlineVariant,
                            errorBorderColor = AccentBufferCoral
                        )
                    )
                }

                // Verify & Complete Login / Registration Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (otpCode.length != 6) {
                            otpError = "Bitte gib den vollständigen 6-stelligen Code ein."
                            return@Button
                        }

                        val cleanEmail = email.trim().lowercase()
                        val cleanName = name.trim().ifBlank { "Athlet" }

                        // Verify OTP Code
                        val isCodeValid = viewModel?.verifyOtpCode(cleanEmail, otpCode) == true || otpCode == lastGeneratedOtp
                        if (!isCodeValid) {
                            otpError = "Ungültiger Bestätigungscode. Bitte überprüfe deine Eingabe."
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (isRegisterMode) {
                                    val res = viewModel?.register(cleanName, cleanEmail, password)
                                    if (res != null && res.isSuccess) {
                                        onLoginSuccess(cleanEmail, cleanName)
                                    } else {
                                        otpError = res?.exceptionOrNull()?.message ?: "Registrierung fehlgeschlagen."
                                    }
                                } else {
                                    val res = viewModel?.login(cleanEmail, password)
                                    if (res != null && res.isSuccess) {
                                        val prof = res.getOrNull()
                                        onLoginSuccess(cleanEmail, prof?.name ?: cleanName)
                                    } else {
                                        otpError = res?.exceptionOrNull()?.message ?: "Anmeldung fehlgeschlagen."
                                    }
                                }
                            } catch (e: Exception) {
                                otpError = e.message ?: "Verifizierung fehlgeschlagen."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_verify_otp"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryLilacDark,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Code bestätigen & Einloggen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Resend Code and Back Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            authStep = AuthStep.CREDENTIALS
                            otpError = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text("E-Mail ändern", color = TextMuted, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = {
                            if (resendCooldown == 0) {
                                val cleanEmail = email.trim().lowercase()
                                val cleanName = name.trim().ifBlank { "Athlet" }
                                coroutineScope.launch {
                                    val newCode = viewModel?.generateAndSendVerificationCode(cleanEmail, cleanName, isRegisterMode) 
                                        ?: (100000..999999).random().toString()
                                    lastGeneratedOtp = newCode
                                    resendCooldown = 30
                                    showEmailNotificationBanner = true
                                    otpError = null
                                }
                            }
                        },
                        enabled = resendCooldown == 0
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (resendCooldown == 0) PrimaryLilac else TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (resendCooldown > 0) "Erneut in ${resendCooldown}s" else "Code erneut senden",
                            color = if (resendCooldown == 0) PrimaryLilac else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsSectionItem(
    number: String,
    title: String,
    content: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = number,
                color = PrimaryLilac,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = content,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

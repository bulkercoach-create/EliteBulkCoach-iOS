package com.example.ui.screens.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.AccentSuccessGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OnboardingScreen(
    initialEmail: String = "",
    initialName: String = "",
    onComplete: (
        name: String,
        email: String,
        heightCm: Int,
        currentWeightKg: Float,
        targetWeightKg: Float,
        weeklyGainKg: Float,
        age: Int,
        gender: String,
        goal: String,
        split: String,
        profileImageUri: String?
    ) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val minStep = if (initialEmail.isNotBlank() || initialName.isNotBlank()) 2 else 1
    var step by remember { mutableIntStateOf(minStep) }
    val totalSteps = 5

    // Form states
    var name by remember(initialName) { mutableStateOf(initialName.ifBlank { "" }) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail.ifBlank { "" }) }
    var heightCmStr by remember { mutableStateOf("180") }
    var currentWeightKgStr by remember { mutableStateOf("75") }
    var targetWeightKgStr by remember { mutableStateOf("82") }
    var weeklyGainPreset by remember { mutableFloatStateOf(0.5f) }
    var isCustomWeeklyGain by remember { mutableStateOf(false) }
    var customWeeklyGainStr by remember { mutableStateOf("0.4") }
    var ageStr by remember { mutableStateOf("24") }
    var gender by remember { mutableStateOf("männlich") }
    var goal by remember { mutableStateOf("Muskelaufbau (Hypertrophie)") }
    var split by remember { mutableStateOf("Push / Pull / Legs") }
    var profileImageUri by remember { mutableStateOf<String?>(null) }

    fun saveBitmapLocally(bitmap: Bitmap): String {
        val filename = "profile_avatar_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = com.example.util.ImageUtils.processAndSaveBitmapToAppStorage(context, bitmap, "profile_avatar")
            if (path.isNotBlank()) profileImageUri = path
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = com.example.util.ImageUtils.processAndSaveImageToAppStorage(context, uri.toString(), "profile_avatar")
            profileImageUri = path
        }
    }

    // Derived dynamic calculation values
    val heightCm = heightCmStr.toIntOrNull() ?: 180
    val currentWeightKg = currentWeightKgStr.toFloatOrNull() ?: 75f
    val targetWeightKg = targetWeightKgStr.toFloatOrNull() ?: (currentWeightKg + 6f)
    val effectiveWeeklyGain = if (isCustomWeeklyGain) (customWeeklyGainStr.toFloatOrNull() ?: 0.5f) else weeklyGainPreset
    val age = ageStr.toIntOrNull() ?: 24

    val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) || goal.contains("Definition", ignoreCase = true) || goal.contains("Cut", ignoreCase = true)
    val isMaintain = goal.contains("Halten", ignoreCase = true) || goal.contains("Recomp", ignoreCase = true)

    val lifesumProjection by remember(heightCm, currentWeightKg, targetWeightKg, effectiveWeeklyGain, age, gender, goal) {
        derivedStateOf {
            val s = if (gender.lowercase().contains("weib") || gender.lowercase() == "female") -161 else 5
            val bmr = (10.0 * currentWeightKg) + (6.25 * heightCm) - (5.0 * age) + s
            val tdee = (bmr * 1.55).toInt()
            val dailySurplus = when {
                isFatLoss -> -((effectiveWeeklyGain * 1100f).toInt().coerceIn(200, 1000))
                isMaintain -> 0
                else -> (effectiveWeeklyGain * 1100f).toInt().coerceIn(150, 1000)
            }
            val targetKcal = (tdee + dailySurplus).coerceAtLeast(1500)

            val proteinG = (currentWeightKg * 2.2f).toInt()
            val fatG = (currentWeightKg * 1.0f).toInt()
            val remainingKcal = (targetKcal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(400)
            val carbsG = remainingKcal / 4

            val diffKg = kotlin.math.abs(targetWeightKg - currentWeightKg).coerceAtLeast(0.1f)
            val weeks = (diffKg / effectiveWeeklyGain.coerceAtLeast(0.05f)).toInt().coerceAtLeast(1)
            val targetDate = LocalDate.now().plusWeeks(weeks.toLong())
            val dateFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

            LifesumUiCalc(
                tdeeKcal = tdee,
                dailySurplusKcal = dailySurplus,
                targetKcal = targetKcal,
                proteinGrams = proteinG,
                carbsGrams = carbsG,
                fatGrams = fatG,
                diffKg = diffKg,
                estimatedWeeks = weeks,
                targetDateFormatted = targetDate.format(dateFormatter)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar Progress
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > minStep) {
                            IconButton(
                                onClick = { step-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurück",
                                    tint = PrimaryLilac,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "AXON",
                            color = PrimaryLilac,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        text = "Schritt $step von $totalSteps",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Step indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= step) PrimaryLilac else Color(0xFF49454F))
                        )
                    }
                }
            }

            // Step Content with Animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "OnboardingStep"
                ) { currentStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                // Step 1: Name & E-Mail
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381E72)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryLilac,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Hallo! Ich bin dein Axon AI Coach.",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                )
                                Text(
                                    text = "Wie heißt du und wie lautet deine E-Mail-Adresse?",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Dein Name (z.B. Alex)", color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceCard,
                                        unfocusedContainerColor = DarkSurfaceCard,
                                        focusedBorderColor = PrimaryLilac,
                                        unfocusedBorderColor = DarkOutlineVariant
                                    ),
                                    singleLine = true
                                )

                                val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                                val isEmailValid = email.isBlank() || emailRegex.matches(email.trim())

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-Mail-Adresse (z.B. alex@example.com)", color = TextMuted) },
                                    isError = !isEmailValid,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceCard,
                                        unfocusedContainerColor = DarkSurfaceCard,
                                        focusedBorderColor = PrimaryLilac,
                                        unfocusedBorderColor = DarkOutlineVariant,
                                        errorBorderColor = AccentBufferCoral
                                    ),
                                    singleLine = true
                                )
                                if (!isEmailValid) {
                                    Text(
                                        text = "Bitte gib eine gültige Domain wie .com oder .de an.",
                                        color = AccentBufferCoral,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            2 -> {
                                // Step 2: Age, Gender, Height, Weight
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381E72)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MonitorWeight,
                                        contentDescription = null,
                                        tint = PrimaryLilac,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Körperdaten & Stoffwechsel",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wie alt bist du, wie groß und wie schwer bist du aktuell?",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ageStr,
                                        onValueChange = { ageStr = it },
                                        label = { Text("Alter", color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkSurfaceCard,
                                            unfocusedContainerColor = DarkSurfaceCard,
                                            focusedBorderColor = PrimaryLilac,
                                            unfocusedBorderColor = DarkOutlineVariant
                                        ),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = heightCmStr,
                                        onValueChange = { heightCmStr = it },
                                        label = { Text("Größe (cm)", color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkSurfaceCard,
                                            unfocusedContainerColor = DarkSurfaceCard,
                                            focusedBorderColor = PrimaryLilac,
                                            unfocusedBorderColor = DarkOutlineVariant
                                        ),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = currentWeightKgStr,
                                        onValueChange = { currentWeightKgStr = it },
                                        label = { Text("Gewicht (kg)", color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkSurfaceCard,
                                            unfocusedContainerColor = DarkSurfaceCard,
                                            focusedBorderColor = PrimaryLilac,
                                            unfocusedBorderColor = DarkOutlineVariant
                                        ),
                                        singleLine = true
                                    )
                                }

                                Text(
                                    text = "Geschlecht",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("männlich", "weiblich", "divers").forEach { g ->
                                        val isSelected = gender.equals(g, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) PrimaryLilac else DarkSurfaceCard)
                                                .clickable { gender = g }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = g.replaceFirstChar { it.uppercase() },
                                                color = if (isSelected) PrimaryLilacDark else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // Step 3: Fitness Goal & Dynamic Target Calculator
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "PRIMÄRES FITNESS-ZIEL",
                                        color = PrimaryLilac,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = "Was ist dein primäres Ziel?",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Goal Selection Cards
                                val goals = listOf(
                                    Triple("Muskelaufbau (Hypertrophie)", "Masseaufbau mit gezieltem Kalorienüberschuss für maximales Muskelwachstum", Icons.Default.TrendingUp),
                                    Triple("Fettabbau (Definition / Cut)", "Körperfett reduzieren bei maximalem Muskelerhalt mit moderatem Defizit", Icons.Default.TrendingDown),
                                    Triple("Gewicht halten (Recomposition)", "Erhaltungskalorien mit Fokus auf progressive Kraftsteigerung", Icons.Default.TrendingFlat)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    goals.forEach { (gTitle, gDesc, gIcon) ->
                                        val isSelected = goal.startsWith(gTitle.take(10), ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0xFF381E72) else DarkSurfaceCard)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) PrimaryLilac else DarkOutlineVariant,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { goal = gTitle }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) PrimaryLilac else Color(0xFF2B2930)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = gIcon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) PrimaryLilacDark else PrimaryLilac,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = gTitle,
                                                        color = TextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = gDesc,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp
                                                    )
                                                }

                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryLilac)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Target Weight Field
                                OutlinedTextField(
                                    value = targetWeightKgStr,
                                    onValueChange = { targetWeightKgStr = it },
                                    label = { Text("Was ist dein Zielgewicht (in kg)?", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceCard,
                                        unfocusedContainerColor = DarkSurfaceCard,
                                        focusedBorderColor = PrimaryLilac,
                                        unfocusedBorderColor = DarkOutlineVariant
                                    ),
                                    singleLine = true
                                )

                                // Weekly Rate Selection
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isFatLoss) "Abnahme-Rate pro Woche" else "Zunahme-Rate pro Woche",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            0.25f to "0.25 kg",
                                            0.50f to "0.50 kg",
                                            0.75f to "0.75 kg"
                                        ).forEach { (gainVal, label) ->
                                            val isSelected = !isCustomWeeklyGain && weeklyGainPreset == gainVal
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) PrimaryLilac else DarkSurfaceCard)
                                                    .clickable {
                                                        isCustomWeeklyGain = false
                                                        weeklyGainPreset = gainVal
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) PrimaryLilacDark else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        val isCustomSelected = isCustomWeeklyGain
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCustomSelected) PrimaryLilac else DarkSurfaceCard)
                                                .clickable { isCustomWeeklyGain = true }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Eigenes",
                                                color = if (isCustomSelected) PrimaryLilacDark else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isCustomWeeklyGain) {
                                        OutlinedTextField(
                                            value = customWeeklyGainStr,
                                            onValueChange = { customWeeklyGainStr = it },
                                            label = { Text("Rate pro Woche (in kg, z.B. 0.35)", color = TextMuted) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = DarkSurfaceCard,
                                                unfocusedContainerColor = DarkSurfaceCard,
                                                focusedBorderColor = PrimaryLilac,
                                                unfocusedBorderColor = DarkOutlineVariant
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }

                                // Live Calculation Preview Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF381E72))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "BERECHNETES KALORIENZIEL",
                                                color = PrimaryLilac,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = when {
                                                    isFatLoss -> "${lifesumProjection.dailySurplusKcal} kcal Defizit"
                                                    isMaintain -> "Erhaltung"
                                                    else -> "+${lifesumProjection.dailySurplusKcal} kcal Surplus"
                                                },
                                                color = PrimaryLilac,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${lifesumProjection.targetKcal} kcal",
                                                    color = TextPrimary,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "TDEE Grundbedarf: ${lifesumProjection.tdeeKcal} kcal",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${lifesumProjection.proteinGrams}g Protein",
                                                    color = Color(0xFFFFD8E4),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${lifesumProjection.carbsGrams}g Carbs • ${lifesumProjection.fatGrams}g Fett",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                // Step 4: Optional Profile Picture / Camera Capture
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381E72)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = PrimaryLilac,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "Dein Profilbild (Optional)",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wähle ein Profilbild oder nimm direkt ein Kamera-Foto auf. Du kannst diesen Schritt auch überspringen.",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )

                                // Big Avatar Preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceCard)
                                            .border(2.dp, if (profileImageUri != null) PrimaryLilac else DarkOutlineVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profileImageUri != null) {
                                            val rotatedAvatar = remember(profileImageUri) {
                                                com.example.util.ImageUtils.getRotatedBitmap(context, profileImageUri!!)
                                            }
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(rotatedAvatar ?: profileImageUri)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Profilbild",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            val initials = name.split(" ")
                                                .filter { it.isNotBlank() }
                                                .take(2)
                                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                                .joinToString("")
                                                .ifBlank { "EB" }

                                            Text(
                                                text = initials,
                                                color = PrimaryLilac,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Photo Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryLilac)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Kamera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryLilac)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Galerie", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (profileImageUri != null) {
                                    TextButton(
                                        onClick = { profileImageUri = null },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentBufferCoral, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ausgewähltes Bild entfernen", color = AccentBufferCoral, fontSize = 12.sp)
                                    }
                                }
                            }

                            5 -> {
                                // Step 5: Summary & Confirmation
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF381E72)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        tint = PrimaryLilac,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    text = "Onboarding abgeschlossen.",
                                    color = TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ich bin bereit. Hier ist dein individueller Profil-Überblick:",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )

                                // Final Summary Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, DarkOutlineVariant, RoundedCornerShape(20.dp))
                                        .padding(18.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF381E72))
                                                    .border(1.5.dp, PrimaryLilac, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (profileImageUri != null) {
                                                    val rotatedAvatar = remember(profileImageUri) {
                                                        com.example.util.ImageUtils.getRotatedBitmap(context, profileImageUri!!)
                                                    }
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(rotatedAvatar ?: profileImageUri)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    val initials = name.split(" ")
                                                        .filter { it.isNotBlank() }
                                                        .take(2)
                                                        .mapNotNull { it.firstOrNull()?.uppercase() }
                                                        .joinToString("")
                                                        .ifBlank { "EB" }

                                                    Text(initials, color = PrimaryLilac, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = name.ifBlank { "Bulk Athlet" },
                                                    color = TextPrimary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$age Jahre • $heightCm cm • ${gender.replaceFirstChar { it.uppercase() }}",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = goal,
                                                    color = PrimaryLilac,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(DarkOutlineVariant)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Startgewicht", color = TextMuted, fontSize = 11.sp)
                                                Text("${currentWeightKg} kg", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Zielgewicht", color = TextMuted, fontSize = 11.sp)
                                                Text("${targetWeightKg} kg", color = PrimaryLilac, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Täglich", color = TextMuted, fontSize = 11.sp)
                                                Text("${lifesumProjection.targetKcal} kcal", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Safety buffer note
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF2B2930))
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentSuccessGreen, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "8% Sicherheits-Puffer bei Mahlzeiten aktiv.",
                                                    color = AccentSuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (step < totalSteps) {
                            step++
                        } else {
                            val finalEmail = if (email.isBlank()) "athlet@bulkcoach.com" else email.trim()
                            val finalName = if (name.isBlank()) "Bulk Athlet" else name.trim()
                            onComplete(
                                finalName,
                                finalEmail,
                                heightCm,
                                currentWeightKg,
                                targetWeightKg,
                                effectiveWeeklyGain,
                                age,
                                gender,
                                goal,
                                split,
                                profileImageUri
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = when (step) {
                        1 -> name.isNotBlank() && (email.isBlank() || "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex().matches(email.trim()))
                        2 -> heightCm > 100 && currentWeightKg > 30f && age > 10
                        3 -> targetWeightKg > 30f
                        4 -> true
                        5 -> true
                        else -> true
                    }
                ) {
                    Text(
                        text = if (step < totalSteps) "Weiter" else "Jetzt durchstarten & Dashboard öffnen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        imageVector = if (step < totalSteps) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (step == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLogin() },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Bereits einen Account? ",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Cloud-Login",
                            color = PrimaryLilac,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class LifesumUiCalc(
    val tdeeKcal: Int,
    val dailySurplusKcal: Int,
    val targetKcal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val diffKg: Float,
    val estimatedWeeks: Int,
    val targetDateFormatted: String
)

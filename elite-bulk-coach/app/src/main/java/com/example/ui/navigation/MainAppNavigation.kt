package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.camera.FoodVisionCameraScreen
import com.example.ui.screens.chat.CoachChatScreen
import com.example.ui.screens.checkin.CheckInScreen
import androidx.compose.material3.MaterialTheme
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.nutrition.NutritionScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.performance.PerformanceScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.workout.WorkoutScreen
import com.example.ui.viewmodel.BulkCoachViewModel

enum class ScreenTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Home),
    NUTRITION("Ernährung", Icons.Default.Restaurant),
    WORKOUT("Training", Icons.Default.FitnessCenter),
    PERFORMANCE("Analyse", Icons.Default.TrendingUp),
    CHECKIN("Check-in", Icons.Default.EventRepeat),
    COACH("Coach AI", Icons.Default.AutoAwesome)
}

enum class NavigationFlow {
    MAIN_APP,
    ONBOARDING,
    AUTH,
    CAMERA_SCAN,
    PROFILE
}

@Composable
fun MainAppNavigation(
    viewModel: BulkCoachViewModel = viewModel(),
    launchMotivationOnStart: Boolean = false
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val showMondayPopup by viewModel.showMondayCheckInPopup.collectAsState()

    val isLoggedIn = viewModel.isLoggedIn()
    val isUserKnownOnboarded = userProfile?.isOnboarded == true
    val shouldBeInMainApp = isUserKnownOnboarded || (isLoggedIn && userProfile == null)

    var currentFlow by remember {
        mutableStateOf(if (shouldBeInMainApp) NavigationFlow.MAIN_APP else NavigationFlow.AUTH)
    }
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var prefillEmail by remember { mutableStateOf("") }
    var prefillName by remember { mutableStateOf("") }

    // Automatic Permission Launcher for Camera and Notifications
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            android.Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(userProfile) {
        if (userProfile != null && viewModel.isLoggedIn()) {
            if (userProfile?.isOnboarded == true && (currentFlow == NavigationFlow.AUTH || currentFlow == NavigationFlow.ONBOARDING)) {
                currentFlow = NavigationFlow.MAIN_APP
            } else if (userProfile?.isOnboarded == false && currentFlow == NavigationFlow.MAIN_APP) {
                currentFlow = NavigationFlow.ONBOARDING
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentFlow) {
            NavigationFlow.ONBOARDING -> {
                OnboardingScreen(
                    initialEmail = prefillEmail,
                    initialName = prefillName,
                    onComplete = { name, email, height, weight, targetWeight, weeklyGain, age, gender, goal, split, profileImageUri ->
                        viewModel.completeOnboarding(
                            name = name,
                            email = email,
                            heightCm = height,
                            currentWeightKg = weight,
                            targetWeightKg = targetWeight,
                            weeklyWeightGainKg = weeklyGain,
                            age = age,
                            gender = gender,
                            goal = goal,
                            split = split,
                            profileImageUri = profileImageUri
                        )
                        currentFlow = NavigationFlow.MAIN_APP
                    },
                    onNavigateToLogin = {
                        currentFlow = NavigationFlow.AUTH
                    }
                )
            }

            NavigationFlow.AUTH -> {
                AuthScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { email, name ->
                        prefillEmail = email
                        prefillName = name
                        if (userProfile?.isOnboarded == true) {
                            currentFlow = NavigationFlow.MAIN_APP
                        } else {
                            currentFlow = NavigationFlow.ONBOARDING
                        }
                    },
                    onStartOnboarding = {
                        prefillEmail = ""
                        prefillName = ""
                        currentFlow = NavigationFlow.ONBOARDING
                    }
                )
            }

            NavigationFlow.CAMERA_SCAN -> {
                FoodVisionCameraScreen(
                    viewModel = viewModel,
                    onBack = { currentFlow = NavigationFlow.MAIN_APP }
                )
            }

            NavigationFlow.PROFILE -> {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { currentFlow = NavigationFlow.MAIN_APP },
                    onLogout = { 
                        prefillEmail = ""
                        prefillName = ""
                        currentFlow = NavigationFlow.AUTH 
                    }
                )
            }

            NavigationFlow.MAIN_APP -> {
                Scaffold(
                    bottomBar = {
                        ElegantDarkBottomNav(
                            selectedTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            ScreenTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToWorkouts = { currentTab = ScreenTab.WORKOUT },
                                onNavigateToNutrition = { currentTab = ScreenTab.NUTRITION },
                                onNavigateToCamera = { currentFlow = NavigationFlow.CAMERA_SCAN },
                                onNavigateToProfile = { currentFlow = NavigationFlow.PROFILE }
                            )

                            ScreenTab.NUTRITION -> NutritionScreen(
                                viewModel = viewModel,
                                onNavigateToCamera = { currentFlow = NavigationFlow.CAMERA_SCAN }
                            )

                            ScreenTab.WORKOUT -> WorkoutScreen(
                                viewModel = viewModel
                            )

                            ScreenTab.PERFORMANCE -> PerformanceScreen(
                                viewModel = viewModel
                            )

                            ScreenTab.CHECKIN -> CheckInScreen(
                                viewModel = viewModel
                            )

                            ScreenTab.COACH -> CoachChatScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // Automatic Monday Check-In Pop-up
        if (showMondayPopup && currentFlow == NavigationFlow.MAIN_APP) {
            MondayCheckInDialog(
                viewModel = viewModel,
                profile = userProfile ?: UserProfile()
            )
        }

        // Global Toast / Banner for Status Messages
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF381E72))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = statusMessage!!,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MondayCheckInDialog(
    viewModel: BulkCoachViewModel,
    profile: UserProfile
) {
    var weightInput by remember(profile.currentWeightKg) { mutableStateOf(profile.currentWeightKg.toString()) }
    var heightInput by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var feelingScore by remember { mutableFloatStateOf(8f) }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.dismissMondayPopup() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Es ist Montag!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Wöchentlicher Check-in", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Trage dein aktuelles Gewicht und deine Daten ein. Dein Bulk-Kalorienziel wird für die neue Woche automatisch angepasst!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Gewicht (kg)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Größe (cm)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                }

                // Feeling Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Erholung / Energie:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("${feelingScore.toInt()} / 10", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = feelingScore,
                        onValueChange = { feelingScore = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("Notizen (z.B. Starker Pump, Schlaf gut)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightInput.toFloatOrNull() ?: profile.currentWeightKg
                    val h = heightInput.toIntOrNull() ?: profile.heightCm
                    viewModel.submitMondayCheckIn(w, feelingScore.toInt(), notesInput, h)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Check-in speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissMondayPopup() }) {
                Text("Später", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ElegantDarkBottomNav(
    selectedTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp, horizontal = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


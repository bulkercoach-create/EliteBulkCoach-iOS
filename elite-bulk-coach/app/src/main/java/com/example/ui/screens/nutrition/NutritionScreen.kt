package com.example.ui.screens.nutrition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.FoodItem
import com.example.data.model.MealType
import com.example.data.model.UserProfile
import com.example.ui.components.AddMealBottomSheet
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.MacroGrid
import com.example.ui.components.MealProgressGrid
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.ChipBadgeBg
import com.example.ui.theme.ChipBadgeText
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel
import java.time.LocalDate

// Fixed Muscle Building Calorie Distribution Percentages
fun MealType.muscleBuildingPercentage(): Float = when (this) {
    MealType.BREAKFAST -> 0.30f // 30% Frühstück
    MealType.LUNCH -> 0.35f     // 35% Mittagessen
    MealType.DINNER -> 0.25f    // 25% Abendessen
    MealType.SNACK_SHAKE -> 0.10f // 10% Snacks & Shakes
}

fun MealType.muscleBuildingPercentageLabel(): String = when (this) {
    MealType.BREAKFAST -> "30% Muskelaufbau"
    MealType.LUNCH -> "35% Muskelaufbau"
    MealType.DINNER -> "25% Muskelaufbau"
    MealType.SNACK_SHAKE -> "10% Muskelaufbau"
}

@Composable
fun NutritionScreen(
    viewModel: BulkCoachViewModel,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foods by viewModel.foodsForSelectedDate.collectAsState()
    val barcodeResult by viewModel.barcodeLookupResult.collectAsState()
    val isLookingUpBarcode by viewModel.isLookingUpBarcode.collectAsState()
    val searchResults by viewModel.foodSearchResults.collectAsState()
    val isSearchingFoods by viewModel.isSearchingFoods.collectAsState()

    var showAddMealSheet by remember { mutableStateOf(false) }
    var activeMealTypeForAdd by remember { mutableStateOf(MealType.LUNCH) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    // Camera Permission Launcher for Barcode Scanner
    val barcodePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBarcodeDialog = true
        } else {
            viewModel.showStatusMessage("Kamera-Zugriff abgelehnt. Du kannst Barcodes auch manuell eingeben.")
            showBarcodeDialog = true
        }
    }

    val openBarcodeScannerWithPermission = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showBarcodeDialog = true
        } else {
            barcodePermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val profile = userProfile ?: UserProfile()
    val nutritionSummary by viewModel.selectedDateNutritionSummary.collectAsState()
    val baseTargetKcal = nutritionSummary.baseCalorieTarget
    val workoutKcal = nutritionSummary.workoutCaloriesBurned
    val targetKcal = nutritionSummary.targetCalories
    val targetProtein = profile.proteinTargetGrams
    val targetCarbs = profile.carbsTargetGrams
    val targetFat = profile.fatTargetGrams

    val eatenKcal = nutritionSummary.eatenCalories
    val rawKcal = foods.sumOf { it.rawCalories }
    val bufferKcal = (rawKcal - eatenKcal).coerceAtLeast(0)
    val totalProtein = nutritionSummary.eatenProtein
    val totalCarbs = nutritionSummary.eatenCarbs
    val totalFat = nutritionSummary.eatenFat

    val dateLabel = viewModel.getDateDisplayLabel(selectedDate)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "ERNÄHRUNG & KALORIENLOGBUCH",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Muskelaufbau-Plan & Puffer",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Date Navigation Bar
            item {
                DateSelectorBar(
                    dateLabel = dateLabel,
                    selectedDate = selectedDate,
                    currentWeek = profile.currentTrainingWeek,
                    onPreviousDay = { viewModel.changeDateOffset(-1) },
                    onNextDay = { viewModel.changeDateOffset(1) },
                    onTodayClick = { viewModel.setDateToToday() }
                )
            }

            // Calorie & Buffer Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KALORIENSTATUS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (workoutKcal > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF381E72))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🔥 +$workoutKcal KCAL WORKOUT",
                                            color = PrimaryLilac,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ChipBadgeBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                    Text(
                                        text = "8% PUFFER EINGERECHNET",
                                        color = ChipBadgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "$eatenKcal",
                                    color = TextPrimary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "von $targetKcal kcal Tagesziel gegessen",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (workoutKcal > 0) {
                                    Text(
                                        text = "Basis: $baseTargetKcal + $workoutKcal kcal Training",
                                        color = PrimaryLilac,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val remaining = targetKcal - eatenKcal
                                Text(
                                    text = if (remaining >= 0) "$remaining kcal übrig" else "${Math.abs(remaining)} kcal Überschuss",
                                    color = if (remaining >= 0) PrimaryLilac else Color(0xFF81C784),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sicherheitspuffer: -$bufferKcal kcal",
                                    color = AccentBufferCoral,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Mahlzeiten-Übersicht mit animierten Fortschrittsbalken (Früh, Mittag, Abend, Snack)
            item {
                val breakfastKcal = foods.filter { it.mealType == MealType.BREAKFAST }.sumOf { it.bufferedCalories }
                val lunchKcal = foods.filter { it.mealType == MealType.LUNCH }.sumOf { it.bufferedCalories }
                val dinnerKcal = foods.filter { it.mealType == MealType.DINNER }.sumOf { it.bufferedCalories }
                val snackKcal = foods.filter { it.mealType == MealType.SNACK_SHAKE }.sumOf { it.bufferedCalories }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    MealProgressGrid(
                        targetDailyKcal = targetKcal,
                        breakfastKcal = breakfastKcal,
                        lunchKcal = lunchKcal,
                        dinnerKcal = dinnerKcal,
                        snackKcal = snackKcal,
                        onMealClick = { selectedMealType ->
                            activeMealTypeForAdd = selectedMealType
                            showAddMealSheet = true
                        }
                    )
                }
            }

            // Macros
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MacroGrid(
                        proteinGrams = totalProtein,
                        proteinTarget = targetProtein,
                        carbsGrams = totalCarbs,
                        carbsTarget = targetCarbs,
                        fatGrams = totalFat,
                        fatTarget = targetFat
                    )
                }
            }

            // Quick Add Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openBarcodeScannerWithPermission() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLilac,
                            contentColor = PrimaryLilacDark
                        )
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Barcode scannen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            activeMealTypeForAdd = MealType.LUNCH
                            showAddMealSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2930),
                            contentColor = PrimaryLilac
                        )
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Lebensmittel suchen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Meal Categories Sections with Muscle Building Progress Bars
            MealType.values().forEach { type ->
                val typeFoods = foods.filter { it.mealType == type }
                val typeKcal = typeFoods.sumOf { it.bufferedCalories }
                val typeProtein = typeFoods.sumOf { it.proteinGrams.toDouble() }.toInt()
                val typeCarbs = typeFoods.sumOf { it.carbsGrams.toDouble() }.toInt()
                val typeFat = typeFoods.sumOf { it.fatGrams.toDouble() }.toInt()

                // Muscle Building Target Calculation
                val recommendedKcal = (targetKcal * type.muscleBuildingPercentage()).toInt().coerceAtLeast(1)
                val progressFraction = (typeKcal.toFloat() / recommendedKcal.toFloat()).coerceIn(0f, 1f)

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section Header Card with Progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF25232B))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${type.displayName}: $typeKcal / $recommendedKcal kcal",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${typeProtein}g Protein • ${typeCarbs}g Carbs • ${typeFat}g Fett",
                                            color = PrimaryLilac,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryLilac.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = type.muscleBuildingPercentageLabel(),
                                            color = PrimaryLilac,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Linear Progress Bar for Muscle Building Target
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (typeKcal >= recommendedKcal) Color(0xFF81C784) else PrimaryLilac,
                                        trackColor = Color(0xFF383540),
                                        strokeCap = StrokeCap.Round
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(progressFraction * 100).toInt()}% erreicht",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                        val diff = recommendedKcal - typeKcal
                                        Text(
                                            text = if (diff > 0) "Noch $diff kcal bis zum Mahlzeitziel" else "Ziel erreicht (${Math.abs(diff)} kcal Überschuss)",
                                            color = if (diff > 0) TextMuted else Color(0xFF81C784),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (typeFoods.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2B2930).copy(alpha = 0.5f))
                                    .clickable {
                                        activeMealTypeForAdd = type
                                        showAddMealSheet = true
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "+ ${type.displayName} hinzufügen (Ziel: $recommendedKcal kcal)",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            typeFoods.forEach { food ->
                                DetailedMealRow(
                                    food = food,
                                    onDelete = { viewModel.deleteFoodItem(food.id) }
                                )
                            }

                            // Quick add more button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2B2930).copy(alpha = 0.35f))
                                    .clickable {
                                        activeMealTypeForAdd = type
                                        showAddMealSheet = true
                                    }
                                    .padding(vertical = 8.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "+ Weiteres zu ${type.displayName} hinzufügen",
                                    color = PrimaryLilac,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Camera Button
        FloatingActionButton(
            onClick = onNavigateToCamera,
            containerColor = PrimaryLilac,
            contentColor = PrimaryLilacDark,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Food Vision Kamera",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showAddMealSheet) {
        AddMealBottomSheet(
            presets = viewModel.getCommonBulkPresets(),
            initialMealType = activeMealTypeForAdd,
            searchResults = searchResults,
            isSearching = isSearchingFoods,
            onSearchQueryChange = { query -> viewModel.searchFoods(query) },
            onDismiss = { showAddMealSheet = false },
            onAddFood = { name, portion, kcal, p, c, f, mealType, isAi ->
                viewModel.addFoodItem(name, portion, kcal, p, c, f, mealType, isAi)
            },
            onOpenBarcodeScanner = {
                showAddMealSheet = false
                openBarcodeScannerWithPermission()
            }
        )
    }

    if (showBarcodeDialog) {
        BarcodeScannerDialog(
            lookupResult = barcodeResult,
            isLoading = isLookingUpBarcode,
            onLookup = { code -> viewModel.lookupBarcode(code) },
            onAddFood = { name, portion, kcal, p, c, f, mealType, barcode ->
                viewModel.addFoodItem(name, portion, kcal, p, c, f, mealType, isAiEstimated = false, barcode = barcode)
                viewModel.clearBarcodeResult()
            },
            onDismiss = {
                showBarcodeDialog = false
                viewModel.clearBarcodeResult()
            },
            onScanPhoto = { bitmap ->
                viewModel.analyzeBarcodeFromPhoto(bitmap)
            }
        )
    }
}

@Composable
fun DetailedMealRow(
    food: FoodItem,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryLilac.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = PrimaryLilac,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = food.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = food.portionDescription,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${food.proteinGrams.toInt()}g Protein • ${food.carbsGrams.toInt()}g Carbs • ${food.fatGrams.toInt()}g Fett",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${food.bufferedCalories} kcal",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (food.rawCalories != food.bufferedCalories) {
                        Text(
                            text = "${food.rawCalories} kcal (-8%)",
                            color = AccentBufferCoral,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eintrag löschen",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

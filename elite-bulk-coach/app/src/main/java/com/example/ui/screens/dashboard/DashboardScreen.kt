package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.data.model.MealType
import com.example.data.model.UserProfile
import com.example.ui.components.AddMealBottomSheet
import com.example.ui.components.AppTopHeader
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CircularCalorieMeter
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.MacroGrid
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.PrimaryLilacLightContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel
import java.time.LocalDate

@Composable
fun DashboardScreen(
    viewModel: BulkCoachViewModel,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foods by viewModel.foodsForSelectedDate.collectAsState()
    val nutritionSummary by viewModel.selectedDateNutritionSummary.collectAsState()
    val barcodeResult by viewModel.barcodeLookupResult.collectAsState()
    val isLookingUpBarcode by viewModel.isLookingUpBarcode.collectAsState()
    val splitTemplates by viewModel.allSplitTemplates.collectAsState()
    val searchResults by viewModel.foodSearchResults.collectAsState()
    val isSearchingFoods by viewModel.isSearchingFoods.collectAsState()

    var showAddMealSheet by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    val profile = userProfile ?: UserProfile()
    val baseTargetKcal = nutritionSummary.baseCalorieTarget
    val workoutKcal = nutritionSummary.workoutCaloriesBurned
    val targetKcal = nutritionSummary.targetCalories
    val targetProtein = profile.proteinTargetGrams
    val targetCarbs = profile.carbsTargetGrams
    val targetFat = profile.fatTargetGrams

    // Sum totals for selected date
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
                AppTopHeader(
                    userName = profile.name.ifBlank { "Bulk Athlet" },
                    trainingWeek = profile.currentTrainingWeek,
                    isDeload = profile.isDeloadActive,
                    profileImageUri = profile.profileImageUri,
                    onAvatarClick = onNavigateToProfile
                )
            }

            // Date Bar
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

            // Circular Calorie Meter Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CircularCalorieMeter(
                        targetCalories = targetKcal,
                        eatenCalories = eatenKcal,
                        bufferCalories = if (bufferKcal > 0) bufferKcal else (targetKcal * 0.08f).toInt(),
                        workoutCalories = workoutKcal,
                        baseTargetCalories = baseTargetKcal
                    )
                }
            }

            // Macros Row
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

            // Next Workout Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val nextWorkoutName = splitTemplates.firstOrNull()?.title
                    NextWorkoutBannerCard(
                        workoutName = nextWorkoutName,
                        isDeload = profile.isDeloadActive,
                        week = profile.currentTrainingWeek,
                        onClick = onNavigateToWorkouts
                    )
                }
            }



            // Today's Meals Quick List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GETRACKTE MAHLZEITEN",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "${foods.size} Einträge",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (foods.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = PrimaryLilac.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (selectedDate == LocalDate.now()) "Noch keine Mahlzeiten für heute geloggt" else "Keine Einträge an diesem Tag",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { showAddMealSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF381E72),
                                    contentColor = PrimaryLilac
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Mahlzeit hinzufügen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(foods) { food ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MealItemCard(
                            food = food,
                            onDelete = { viewModel.deleteFoodItem(food.id) }
                        )
                    }
                }
            }
        }

        // Floating Camera Button for Instant Food AI Vision
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
            initialMealType = MealType.LUNCH,
            searchResults = searchResults,
            isSearching = isSearchingFoods,
            onSearchQueryChange = { query -> viewModel.searchFoods(query) },
            onDismiss = { showAddMealSheet = false },
            onAddFood = { name, portion, kcal, p, c, f, mealType, isAi ->
                viewModel.addFoodItem(name, portion, kcal, p, c, f, mealType, isAi)
            },
            onOpenBarcodeScanner = {
                showAddMealSheet = false
                showBarcodeDialog = true
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
            }
        )
    }
}

@Composable
fun NextWorkoutBannerCard(
    workoutName: String?,
    isDeload: Boolean,
    week: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryLilacLightContainer)
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF21005D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isDeload) "NÄCHSTES TRAINING (DELOAD -20%)" else "NÄCHSTES TRAINING",
                        color = Color(0xFF21005D).copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = workoutName ?: "Kein Training für heute geplant",
                        color = Color(0xFF21005D),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF21005D)
            )
        }
    }
}

@Composable
fun MealItemCard(
    food: FoodItem,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceCard)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = food.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (food.isAiEstimated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF381E72))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("KI Foto", color = PrimaryLilac, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "${food.mealType.displayName} • ${food.portionDescription}",
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${food.bufferedCalories} kcal",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (food.rawCalories != food.bufferedCalories) {
                    Text(
                        text = "(${food.rawCalories} -8%)",
                        color = AccentBufferCoral,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

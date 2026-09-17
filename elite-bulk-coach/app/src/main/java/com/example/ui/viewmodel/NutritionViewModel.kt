package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FoodItem
import com.example.data.model.MacroNutrients
import com.example.data.model.MealType
import com.example.data.model.UserProfile
import com.example.data.repository.BarcodeLookupResult
import com.example.data.repository.CoachRepository
import com.example.data.repository.FoodItemPreset
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI State for Daily Caloric Intake & Macro Logging
 */
data class DailyNutritionUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val formattedDate: String = "",
    val isToday: Boolean = true,
    val foodList: List<FoodItem> = emptyList(),
    val mealsByType: Map<MealType, List<FoodItem>> = emptyMap(),
    val dailyCalorieGoal: Int = 3200,
    val dailyProteinGoalGrams: Int = 160,
    val dailyCarbsGoalGrams: Int = 400,
    val dailyFatGoalGrams: Int = 85,
    val totalRawCalories: Int = 0,
    val totalBufferedCalories: Int = 0,
    val safetyBufferCalories: Int = 0,
    val totalProteinGrams: Float = 0f,
    val totalCarbsGrams: Float = 0f,
    val totalFatGrams: Float = 0f,
    val remainingCalories: Int = 3200,
    val remainingProteinGrams: Float = 160f,
    val remainingCarbsGrams: Float = 400f,
    val remainingFatGrams: Float = 85f,
    val calorieProgress: Float = 0f,
    val proteinProgress: Float = 0f,
    val carbsProgress: Float = 0f,
    val fatProgress: Float = 0f,
    val isSurplusAchieved: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val barcodeSearchResult: BarcodeLookupResult? = null,
    val isScanningBarcode: Boolean = false
)

/**
 * ViewModel dedicated to managing daily caloric intake and macro logging.
 * Integrates directly with Room Database via [NutritionRepository] and [CoachRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val nutritionRepo = NutritionRepository(application)
    private val coachRepo = CoachRepository(application)

    // Current selected date state
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Barcode state
    private val _barcodeLookupResult = MutableStateFlow<BarcodeLookupResult?>(null)
    val barcodeLookupResult: StateFlow<BarcodeLookupResult?> = _barcodeLookupResult.asStateFlow()

    private val _isLookingUpBarcode = MutableStateFlow(false)
    val isLookingUpBarcode: StateFlow<Boolean> = _isLookingUpBarcode.asStateFlow()

    // Status snackbar / feedback message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // User profile Flow from Room
    val userProfile: StateFlow<UserProfile?> = coachRepo.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Reactive list of food items for the currently selected date
    val foodsForSelectedDate: StateFlow<List<FoodItem>> = _selectedDate
        .flatMapLatest { date ->
            nutritionRepo.getFoodsForDateFlow(date.toString())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Complete Combined Daily Nutrition UI State
    val uiState: StateFlow<DailyNutritionUiState> = combine(
        _selectedDate,
        foodsForSelectedDate,
        userProfile,
        combine(
            _barcodeLookupResult,
            _isLookingUpBarcode,
            _statusMessage
        ) { barcodeRes, isBarcodeLoading, msg ->
            Triple(barcodeRes, isBarcodeLoading, msg)
        }
    ) { date, foods, profile, barcodeAndStatus ->
        val (barcodeRes, isBarcodeLoading, msg) = barcodeAndStatus
        val user = profile ?: UserProfile()
        val targetKcal = user.dailyCalorieTarget
        val targetProtein = user.proteinTargetGrams
        val targetCarbs = user.carbsTargetGrams
        val targetFat = user.fatTargetGrams

        val rawKcal = foods.sumOf { it.rawCalories }
        val bufferedKcal = foods.sumOf { it.bufferedCalories }
        val bufferKcal = (rawKcal - bufferedKcal).coerceAtLeast(0)

        val totalProtein = foods.map { it.proteinGrams.toDouble() }.sum().toFloat()
        val totalCarbs = foods.map { it.carbsGrams.toDouble() }.sum().toFloat()
        val totalFat = foods.map { it.fatGrams.toDouble() }.sum().toFloat()

        val remainingKcal = (targetKcal - bufferedKcal).coerceAtLeast(0)
        val remainingP = (targetProtein - totalProtein).coerceAtLeast(0f)
        val remainingC = (targetCarbs - totalCarbs).coerceAtLeast(0f)
        val remainingF = (targetFat - totalFat).coerceAtLeast(0f)

        val kcalProg = if (targetKcal > 0) (bufferedKcal.toFloat() / targetKcal).coerceIn(0f, 1.5f) else 0f
        val proteinProg = if (targetProtein > 0) (totalProtein / targetProtein).coerceIn(0f, 1.5f) else 0f
        val carbsProg = if (targetCarbs > 0) (totalCarbs / targetCarbs).coerceIn(0f, 1.5f) else 0f
        val fatProg = if (targetFat > 0) (totalFat / targetFat).coerceIn(0f, 1.5f) else 0f

        val grouped = MealType.entries.associateWith { type ->
            foods.filter { it.mealType == type }
        }

        DailyNutritionUiState(
            selectedDate = date,
            formattedDate = formatDateLabel(date),
            isToday = date == LocalDate.now(),
            foodList = foods,
            mealsByType = grouped,
            dailyCalorieGoal = targetKcal,
            dailyProteinGoalGrams = targetProtein,
            dailyCarbsGoalGrams = targetCarbs,
            dailyFatGoalGrams = targetFat,
            totalRawCalories = rawKcal,
            totalBufferedCalories = bufferedKcal,
            safetyBufferCalories = bufferKcal,
            totalProteinGrams = totalProtein,
            totalCarbsGrams = totalCarbs,
            totalFatGrams = totalFat,
            remainingCalories = remainingKcal,
            remainingProteinGrams = remainingP,
            remainingCarbsGrams = remainingC,
            remainingFatGrams = remainingF,
            calorieProgress = kcalProg,
            proteinProgress = proteinProg,
            carbsProgress = carbsProg,
            fatProgress = fatProg,
            isSurplusAchieved = bufferedKcal >= targetKcal,
            statusMessage = msg,
            barcodeSearchResult = barcodeRes,
            isScanningBarcode = isBarcodeLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyNutritionUiState()
    )

    // Predefined German bodybuilding & bulk nutrition foods
    val commonBulkFoods: List<FoodItemPreset> = nutritionRepo.getCommonBulkFoods()

    // Date Navigation
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    // Macro & Food Logging Operations
    fun logFoodItem(
        name: String,
        portionDescription: String,
        rawCalories: Int,
        proteinGrams: Float,
        carbsGrams: Float,
        fatGrams: Float,
        mealType: MealType,
        isAiEstimated: Boolean = false,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.toString()
            nutritionRepo.addFoodItem(
                name = name.ifBlank { "Mahlzeit" },
                portionDescription = portionDescription.ifBlank { "1 Portion" },
                rawCalories = rawCalories.coerceAtLeast(0),
                proteinGrams = proteinGrams.coerceAtLeast(0f),
                carbsGrams = carbsGrams.coerceAtLeast(0f),
                fatGrams = fatGrams.coerceAtLeast(0f),
                mealType = mealType,
                dateString = dateStr,
                isAiEstimated = isAiEstimated,
                barcode = barcode
            )
            val profile = coachRepo.getProfile()
            val goal = profile?.primaryGoal ?: "Masseaufbau"
            val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) || goal.contains("Definition", ignoreCase = true) || goal.contains("Cut", ignoreCase = true) || goal.contains("Abnahme", ignoreCase = true)
            val isMaintain = goal.contains("Halten", ignoreCase = true) || goal.contains("Recomp", ignoreCase = true) || goal.contains("Erhalt", ignoreCase = true) || goal.contains("Beibehalten", ignoreCase = true)

            val bufferNote = if (isAiEstimated) {
                when {
                    isFatLoss -> " (+8% Puffer)"
                    isMaintain -> ""
                    else -> " (-8% Puffer)"
                }
            } else ""
            showStatus("$name hinzugefügt$bufferNote")
        }
    }

    fun logPreset(preset: FoodItemPreset, mealType: MealType = preset.defaultMealType) {
        logFoodItem(
            name = preset.name,
            portionDescription = preset.portion,
            rawCalories = preset.rawCalories,
            proteinGrams = preset.protein,
            carbsGrams = preset.carbs,
            fatGrams = preset.fat,
            mealType = mealType,
            isAiEstimated = false
        )
    }

    fun updateFoodItem(item: FoodItem) {
        viewModelScope.launch {
            nutritionRepo.updateFoodItem(item)
            showStatus("${item.name} aktualisiert")
        }
    }

    fun deleteFoodItem(id: String) {
        viewModelScope.launch {
            nutritionRepo.deleteFoodItem(id)
            showStatus("Eintrag gelöscht")
        }
    }

    fun clearDayLogs(date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            nutritionRepo.deleteFoodsForDate(date.toString())
            showStatus("Alle Einträge für ${formatDateLabel(date)} gelöscht")
        }
    }

    // Barcode Lookup
    fun lookupBarcode(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            _isLookingUpBarcode.value = true
            try {
                val result = nutritionRepo.lookupBarcode(barcode.trim())
                _barcodeLookupResult.value = result
                if (result == null) {
                    showStatus("Barcode nicht gefunden. Bitte manuell eingeben.")
                }
            } catch (e: Exception) {
                showStatus("Fehler beim Barcode-Scan: ${e.localizedMessage}")
            } finally {
                _isLookingUpBarcode.value = false
            }
        }
    }

    fun clearBarcodeResult() {
        _barcodeLookupResult.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun showStatus(message: String) {
        _statusMessage.value = message
    }

    private fun formatDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        val formatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)
        return when (date) {
            today -> "Heute, ${date.format(formatter)}"
            yesterday -> "Gestern, ${date.format(formatter)}"
            tomorrow -> "Morgen, ${date.format(formatter)}"
            else -> {
                val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMM", Locale.GERMAN)
                date.format(dayOfWeekFormatter)
            }
        }
    }
}

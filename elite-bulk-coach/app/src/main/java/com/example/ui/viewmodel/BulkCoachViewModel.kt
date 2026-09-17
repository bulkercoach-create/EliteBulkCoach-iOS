package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalisthenicsAnalysisResult
import com.example.data.model.CalisthenicsAssistanceDrill
import com.example.data.model.ChatMessage
import com.example.data.model.Exercise
import com.example.data.model.ExerciseSet
import com.example.data.model.FoodItem
import com.example.data.model.MealType
import com.example.data.model.ProgressPhoto
import com.example.data.model.UserProfile
import com.example.data.model.WeightCheckIn
import com.example.data.model.WorkoutCalorieResult
import com.example.data.model.WorkoutCalorieResultSummary
import com.example.data.model.WorkoutLogEntry
import com.example.data.model.WorkoutSession
import com.example.data.repository.BarcodeLookupResult
import com.example.data.repository.CoachRepository
import com.example.data.repository.FoodPhotoAnalysisResult
import com.example.data.repository.FoodSearchResultItem
import com.example.data.repository.GeminiRepository
import com.example.data.repository.NutritionRepository
import com.example.data.repository.WorkoutRepository
import com.example.data.repository.WorkoutSplitTemplate
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class BulkCoachViewModel(application: Application) : AndroidViewModel(application) {

    private val coachRepo = CoachRepository(application)
    private val nutritionRepo = NutritionRepository(application)
    private val workoutRepo = WorkoutRepository(application)
    private val geminiRepo = GeminiRepository(application)

    init {
        viewModelScope.launch {
            coachRepo.getOrRestoreProfile()
        }
    }

    fun isLoggedIn(): Boolean = coachRepo.isLoggedIn()

    fun getApiKey(): String = geminiRepo.getApiKey()

    fun getSavedCustomApiKey(): String = geminiRepo.getSavedCustomApiKey()

    fun saveCustomApiKey(key: String) {
        geminiRepo.saveCustomApiKey(key)
        showStatusMessage(if (key.isBlank()) "API-Key zurückgesetzt" else "API-Key gespeichert!")
    }

    fun testApiKey(customKey: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = geminiRepo.testApiKey(customKey)
            onResult(success, message)
        }
    }

    fun setSampleFoodAnalysis(
        dishName: String,
        portion: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        val goal = userProfile.value?.primaryGoal ?: "Masseaufbau"
        val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) ||
                goal.contains("Definition", ignoreCase = true) ||
                goal.contains("Cut", ignoreCase = true) ||
                goal.contains("Abnahme", ignoreCase = true)
        val isMaintain = goal.contains("Halten", ignoreCase = true) ||
                goal.contains("Recomp", ignoreCase = true) ||
                goal.contains("Erhalt", ignoreCase = true) ||
                goal.contains("Beibehalten", ignoreCase = true)

        val (buffered, label) = when {
            isFatLoss -> Pair((calories * 1.08f).toInt(), "+8% Puffer")
            isMaintain -> Pair(calories, "Erhalt")
            else -> Pair((calories * 0.92f).toInt(), "-8% Puffer")
        }

        val summary = if (isMaintain) {
            "Erkannt: $dishName ($portion). Ca. $calories kcal, ${protein.toInt()}g Protein."
        } else {
            "Erkannt: $dishName ($portion). Ca. $calories kcal ($buffered kcal mit $label), ${protein.toInt()}g Protein."
        }

        _photoAnalysisResult.value = FoodPhotoAnalysisResult(
            isFood = true,
            dishName = dishName,
            portion = portion,
            rawCalories = calories,
            bufferedCalories = buffered,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            summary = summary
        )
    }

    val userProfile: StateFlow<UserProfile?> = coachRepo.userProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val checkIns: StateFlow<List<WeightCheckIn>> = coachRepo.checkInsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val workoutSessions: StateFlow<List<WorkoutSession>> = workoutRepo.workoutSessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Exercise Library Flow (Built-in + Custom Created Exercises)
    val allLibraryExercises: StateFlow<List<Exercise>> = workoutRepo.allLibraryExercisesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = workoutRepo.defaultExercises
    )

    // Custom Plans Created by User
    val customWorkoutPlans: StateFlow<List<com.example.data.model.CustomWorkoutPlan>> = workoutRepo.customWorkoutPlansFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All Split Templates Flow (User Custom Plans only)
    val allSplitTemplates: StateFlow<List<WorkoutSplitTemplate>> = workoutRepo.allSplitTemplatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Progress Photos (Formchecks) Flow
    val progressPhotos: StateFlow<List<ProgressPhoto>> = coachRepo.progressPhotosFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _performanceReport = MutableStateFlow<com.example.data.model.PerformanceReport?>(coachRepo.getSavedPerformanceReport())
    val performanceReport: StateFlow<com.example.data.model.PerformanceReport?> = _performanceReport.asStateFlow()

    private val _isAnalyzingPerformance = MutableStateFlow(false)
    val isAnalyzingPerformance: StateFlow<Boolean> = _isAnalyzingPerformance.asStateFlow()

    // Current Date navigation
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val currentDateString: StateFlow<String> = MutableStateFlow(LocalDate.now().toString())

    val foodsForSelectedDate: StateFlow<List<FoodItem>> = _selectedDate.flatMapLatest { date ->
        nutritionRepo.getFoodsForDateFlow(date.toString())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's Foods Flow strictly for Current Day Context
    val todayFoods: StateFlow<List<FoodItem>> = nutritionRepo.getFoodsForDateFlow(LocalDate.now().toString()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic Daily Nutrition Summary combining User Profile, Today's Eaten Foods, and Workout Calories Burned
    val todayNutritionSummary: StateFlow<DailyNutritionSummary> = combine(
        userProfile,
        todayFoods,
        workoutSessions
    ) { profile, foods, workouts ->
        val prof = profile ?: UserProfile()
        val baseTargetKcal = prof.dailyCalorieTarget
        val todayDateStr = LocalDate.now().toString()
        val todayWorkouts = workouts.filter { it.dateString == todayDateStr && it.isCompleted }
        val workoutBurn = todayWorkouts.sumOf { it.caloriesBurned }
        val effectiveTargetKcal = baseTargetKcal + workoutBurn

        val eatenKcal = foods.sumOf { it.bufferedCalories }
        val remainingKcal = (effectiveTargetKcal - eatenKcal).coerceAtLeast(0)

        val targetProt = prof.proteinTargetGrams
        val eatenProt = foods.sumOf { it.proteinGrams.toDouble() }.toFloat()
        val remainingProt = (targetProt - eatenProt).coerceAtLeast(0f)

        val targetCarb = prof.carbsTargetGrams
        val eatenCarb = foods.sumOf { it.carbsGrams.toDouble() }.toFloat()
        val remainingCarb = (targetCarb - eatenCarb).coerceAtLeast(0f)

        val targetF = prof.fatTargetGrams
        val eatenF = foods.sumOf { it.fatGrams.toDouble() }.toFloat()
        val remainingF = (targetF - eatenF).coerceAtLeast(0f)

        DailyNutritionSummary(
            baseCalorieTarget = baseTargetKcal,
            workoutCaloriesBurned = workoutBurn,
            targetCalories = effectiveTargetKcal,
            eatenCalories = eatenKcal,
            remainingCalories = remainingKcal,
            targetProtein = targetProt,
            eatenProtein = eatenProt,
            remainingProtein = remainingProt,
            targetCarbs = targetCarb,
            eatenCarbs = eatenCarb,
            remainingCarbs = remainingCarb,
            targetFat = targetF,
            eatenFat = eatenF,
            remainingFat = remainingF
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyNutritionSummary()
    )

    // Dynamic Nutrition Summary for the currently selected Date Navigator
    val selectedDateNutritionSummary: StateFlow<DailyNutritionSummary> = combine(
        userProfile,
        foodsForSelectedDate,
        workoutSessions,
        _selectedDate
    ) { profile, foods, workouts, date ->
        val prof = profile ?: UserProfile()
        val baseTargetKcal = prof.dailyCalorieTarget
        val dateStr = date.toString()
        val dateWorkouts = workouts.filter { it.dateString == dateStr && it.isCompleted }
        val workoutBurn = dateWorkouts.sumOf { it.caloriesBurned }
        val effectiveTargetKcal = baseTargetKcal + workoutBurn

        val eatenKcal = foods.sumOf { it.bufferedCalories }
        val remainingKcal = (effectiveTargetKcal - eatenKcal).coerceAtLeast(0)

        val targetProt = prof.proteinTargetGrams
        val eatenProt = foods.sumOf { it.proteinGrams.toDouble() }.toFloat()
        val remainingProt = (targetProt - eatenProt).coerceAtLeast(0f)

        val targetCarb = prof.carbsTargetGrams
        val eatenCarb = foods.sumOf { it.carbsGrams.toDouble() }.toFloat()
        val remainingCarb = (targetCarb - eatenCarb).coerceAtLeast(0f)

        val targetF = prof.fatTargetGrams
        val eatenF = foods.sumOf { it.fatGrams.toDouble() }.toFloat()
        val remainingF = (targetF - eatenF).coerceAtLeast(0f)

        DailyNutritionSummary(
            baseCalorieTarget = baseTargetKcal,
            workoutCaloriesBurned = workoutBurn,
            targetCalories = effectiveTargetKcal,
            eatenCalories = eatenKcal,
            remainingCalories = remainingKcal,
            targetProtein = targetProt,
            eatenProtein = eatenProt,
            remainingProtein = remainingProt,
            targetCarbs = targetCarb,
            eatenCarbs = eatenCarb,
            remainingCarbs = remainingCarb,
            targetFat = targetF,
            eatenFat = eatenF,
            remainingFat = remainingF
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyNutritionSummary()
    )

    // Rest Timer State
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    private var timerJob: Job? = null

    // Active Workout State
    private val _activeWorkout = MutableStateFlow<ActiveWorkoutState?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutState?> = _activeWorkout.asStateFlow()

    // Vision / Photo Analysis State
    private val _photoAnalysisResult = MutableStateFlow<FoodPhotoAnalysisResult?>(null)
    val photoAnalysisResult: StateFlow<FoodPhotoAnalysisResult?> = _photoAnalysisResult.asStateFlow()

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto: StateFlow<Boolean> = _isAnalyzingPhoto.asStateFlow()

    // Barcode Lookup State
    private val _barcodeLookupResult = MutableStateFlow<BarcodeLookupResult?>(null)
    val barcodeLookupResult: StateFlow<BarcodeLookupResult?> = _barcodeLookupResult.asStateFlow()

    private val _isLookingUpBarcode = MutableStateFlow(false)
    val isLookingUpBarcode: StateFlow<Boolean> = _isLookingUpBarcode.asStateFlow()

    // Chat with Coach
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "coach",
                text = "Hallo! Ich bin dein Axon AI Coach. Lade jederzeit ein Video oder Foto von deinen Übungen (z.B. Squat, Bench, Muscle-Up, Handstand) hoch für eine sofortige KI-Formanalyse & Schwachstellen-Korrektur!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isCoachReplying = MutableStateFlow(false)
    val isCoachReplying: StateFlow<Boolean> = _isCoachReplying.asStateFlow()

    // Calisthenics Form Analysis State
    private val _isAnalyzingCalisthenics = MutableStateFlow(false)
    val isAnalyzingCalisthenics: StateFlow<Boolean> = _isAnalyzingCalisthenics.asStateFlow()

    private val _latestCalisthenicsResult = MutableStateFlow<CalisthenicsAnalysisResult?>(null)
    val latestCalisthenicsResult: StateFlow<CalisthenicsAnalysisResult?> = _latestCalisthenicsResult.asStateFlow()

    private val _calisthenicsHistory = MutableStateFlow<List<CalisthenicsAnalysisResult>>(emptyList())
    val calisthenicsHistory: StateFlow<List<CalisthenicsAnalysisResult>> = _calisthenicsHistory.asStateFlow()

    // Auth & UI notification banner
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun showStatusMessage(msg: String) {
        _statusMessage.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_statusMessage.value == msg) {
                _statusMessage.value = null
            }
        }
    }

    // Date Navigation
    fun changeDateOffset(days: Int) {
        _selectedDate.value = _selectedDate.value.plusDays(days.toLong())
    }

    fun setDateToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun setDateToYesterday() {
        _selectedDate.value = LocalDate.now().minusDays(1)
    }

    fun getDateDisplayLabel(date: LocalDate): String {
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

    // Onboarding & Profile
    fun completeOnboarding(
        name: String,
        email: String,
        heightCm: Int,
        currentWeightKg: Float,
        targetWeightKg: Float,
        weeklyWeightGainKg: Float,
        age: Int,
        gender: String,
        goal: String,
        split: String = "Push / Pull / Legs",
        profileImageUri: String? = null
    ) {
        viewModelScope.launch {
            val profile = coachRepo.completeOnboarding(
                name = name,
                email = email,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                weeklyWeightGainKg = weeklyWeightGainKg,
                age = age,
                gender = gender,
                goal = goal,
                split = split,
                profileImageUri = profileImageUri
            )
            showStatusMessage("Onboarding abgeschlossen! Kalorienziel: ${profile.dailyCalorieTarget} kcal")
        }
    }

    fun updateProfileImage(imageUri: String?) {
        viewModelScope.launch {
            coachRepo.updateProfileImage(imageUri)
            showStatusMessage(if (imageUri != null) "Profilbild aktualisiert!" else "Profilbild entfernt")
        }
    }

    fun calculateLifesumProjection(
        heightCm: Int,
        currentWeightKg: Float,
        targetWeightKg: Float,
        weeklyGainKg: Float,
        age: Int,
        gender: String,
        goal: String = "Masseaufbau"
    ): LifesumProjectionResult {
        val s = if (gender.lowercase().contains("weib") || gender.lowercase() == "female") -161 else 5
        val bmr = (10.0 * currentWeightKg) + (6.25 * heightCm) - (5.0 * age) + s
        val tdee = (bmr * 1.55).toInt()
        
        val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) || goal.contains("Definition", ignoreCase = true) || goal.contains("Cut", ignoreCase = true)
        val isMaintain = goal.contains("Halten", ignoreCase = true) || goal.contains("Recomp", ignoreCase = true)

        val dailySurplus = when {
            isFatLoss -> -((weeklyGainKg * 1100f).toInt().coerceIn(200, 1000))
            isMaintain -> 0
            else -> (weeklyGainKg * 1100f).toInt().coerceIn(150, 1000)
        }
        val targetKcal = (tdee + dailySurplus).coerceAtLeast(1500)

        val proteinG = (currentWeightKg * 2.2f).toInt()
        val fatG = (currentWeightKg * 1.0f).toInt()
        val remainingKcal = (targetKcal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(400)
        val carbsG = remainingKcal / 4

        val totalGain = kotlin.math.abs(targetWeightKg - currentWeightKg).coerceAtLeast(0.1f)
        val weeks = (totalGain / weeklyGainKg.coerceAtLeast(0.05f)).toInt().coerceAtLeast(1)
        val targetDate = LocalDate.now().plusWeeks(weeks.toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

        return LifesumProjectionResult(
            tdeeKcal = tdee,
            dailySurplusKcal = dailySurplus,
            targetKcal = targetKcal,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG,
            totalGainKg = totalGain,
            estimatedWeeks = weeks,
            estimatedTargetDate = targetDate.format(dateFormatter)
        )
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            coachRepo.saveProfile(profile)
            showStatusMessage("Profil & Kalorienziel aktualisiert")
        }
    }

    suspend fun checkEmailAvailable(email: String): Result<Boolean> {
        return coachRepo.checkEmailAvailable(email)
    }

    suspend fun validateLoginCredentials(email: String, password: String): Result<String> {
        return coachRepo.validateLoginCredentials(email, password)
    }

    suspend fun generateAndSendVerificationCode(email: String, name: String = "Athlet", isRegistration: Boolean = false): String {
        return coachRepo.generateAndSendVerificationCode(email, name, isRegistration)
    }

    suspend fun sendPasswordResetCode(email: String): Result<String> {
        return coachRepo.sendPasswordResetCode(email)
    }

    fun verifyPasswordResetCode(email: String, code: String): Boolean {
        return coachRepo.verifyPasswordResetCode(email, code)
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Boolean> {
        val res = coachRepo.resetPassword(email, code, newPassword)
        if (res.isSuccess) {
            showStatusMessage("Passwort erfolgreich geändert! Du kannst dich jetzt einloggen.")
        }
        return res
    }

    fun generateVerificationCode(email: String): String {
        return coachRepo.generateVerificationCode(email)
    }

    fun verifyOtpCode(email: String, code: String): Boolean {
        return coachRepo.verifyOtpCode(email, code)
    }

    suspend fun register(name: String, email: String, password: String): Result<UserProfile> {
        val res = coachRepo.registerUser(name, email, password)
        if (res.isSuccess) {
            val prof = res.getOrNull()
            showStatusMessage("Registrierung erfolgreich! Willkommen, ${prof?.name ?: name}.")
        }
        return res
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val res = coachRepo.loginUser(email, password)
        if (res.isSuccess) {
            val prof = res.getOrNull()
            showStatusMessage("Erfolgreich angemeldet als ${prof?.name ?: email}")
        }
        return res
    }

    fun loginOrRestore(email: String, name: String = "") {
        viewModelScope.launch {
            val profile = coachRepo.loginOrRegister(email, name)
            showStatusMessage("Erfolgreich angemeldet als ${profile.name} (${profile.email})")
        }
    }

    fun isUserLoggedIn(): Boolean {
        return coachRepo.isLoggedIn()
    }

    fun logout() {
        viewModelScope.launch {
            coachRepo.logoutAndClear()
            showStatusMessage("Abgemeldet.")
        }
    }

    // Nutrition Logging
    fun addFoodItem(
        name: String,
        portion: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        mealType: MealType,
        isAiEstimated: Boolean = false,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.toString()
            nutritionRepo.addFoodItem(
                name = name,
                portionDescription = portion,
                rawCalories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                mealType = mealType,
                dateString = dateStr,
                isAiEstimated = isAiEstimated,
                barcode = barcode
            )
            val goal = userProfile.value?.primaryGoal ?: "Masseaufbau"
            val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) || goal.contains("Definition", ignoreCase = true) || goal.contains("Cut", ignoreCase = true) || goal.contains("Abnahme", ignoreCase = true)
            val isMaintain = goal.contains("Halten", ignoreCase = true) || goal.contains("Recomp", ignoreCase = true) || goal.contains("Erhalt", ignoreCase = true) || goal.contains("Beibehalten", ignoreCase = true)

            val bufferNote = if (isAiEstimated) {
                when {
                    isFatLoss -> " (+8% Puffer)"
                    isMaintain -> ""
                    else -> " (-8% Puffer)"
                }
            } else ""
            showStatusMessage("Mahlzeit hinzugefügt$bufferNote")
        }
    }

    fun deleteFoodItem(id: String) {
        viewModelScope.launch {
            nutritionRepo.deleteFoodItem(id)
            showStatusMessage("Eintrag gelöscht")
        }
    }

    fun getCommonBulkPresets() = nutritionRepo.getCommonBulkFoods()

    // Free-Text Food Search (Open Food Facts + Fuzzy Local Search)
    private val _foodSearchResults = MutableStateFlow<List<FoodSearchResultItem>>(emptyList())
    val foodSearchResults: StateFlow<List<FoodSearchResultItem>> = _foodSearchResults.asStateFlow()

    private val _isSearchingFoods = MutableStateFlow(false)
    val isSearchingFoods: StateFlow<Boolean> = _isSearchingFoods.asStateFlow()

    private var foodSearchJob: Job? = null

    fun searchFoods(query: String) {
        foodSearchJob?.cancel()
        foodSearchJob = viewModelScope.launch {
            _isSearchingFoods.value = true
            try {
                val results = nutritionRepo.searchFoods(query)
                _foodSearchResults.value = results
            } catch (_: Exception) {
                _foodSearchResults.value = emptyList()
            } finally {
                _isSearchingFoods.value = false
            }
        }
    }

    // Barcode Lookup
    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            _isLookingUpBarcode.value = true
            _barcodeLookupResult.value = null
            val result = nutritionRepo.lookupBarcode(code)
            _barcodeLookupResult.value = result
            _isLookingUpBarcode.value = false
            if (result != null) {
                showStatusMessage("Produkt gefunden: ${result.productName}")
            } else {
                showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
            }
        }
    }

    fun clearBarcodeResult() {
        _barcodeLookupResult.value = null
    }

    fun analyzeBarcodeFromPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLookingUpBarcode.value = true
            _barcodeLookupResult.value = null
            try {
                // 1. Process with ML Kit Barcode Scanner
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val detectedEan = barcodes.firstOrNull()?.rawValue
                        if (!detectedEan.isNullOrBlank()) {
                            viewModelScope.launch {
                                val directLookup = nutritionRepo.lookupBarcode(detectedEan)
                                _isLookingUpBarcode.value = false
                                if (directLookup != null) {
                                    _barcodeLookupResult.value = directLookup
                                    showStatusMessage("Barcode $detectedEan erkannt: ${directLookup.productName}")
                                } else {
                                    showStatusMessage("Produkt ($detectedEan) nicht in der Datenbank gefunden, bitte manuell eintragen")
                                }
                            }
                        } else {
                            // 2. Fallback: try AI Vision Barcode reading
                            viewModelScope.launch {
                                try {
                                    val photoResult = geminiRepo.analyzeBarcodePhoto(bitmap)
                                    if (photoResult.barcode.isNotBlank()) {
                                        val directLookup = nutritionRepo.lookupBarcode(photoResult.barcode)
                                        if (directLookup != null) {
                                            _barcodeLookupResult.value = directLookup
                                            showStatusMessage("Barcode erkannt: ${directLookup.productName}")
                                            _isLookingUpBarcode.value = false
                                            return@launch
                                        }
                                    }
                                    if (photoResult.productName.isNotBlank() && photoResult.caloriesPer100g > 0) {
                                        _barcodeLookupResult.value = BarcodeLookupResult(
                                            productName = photoResult.productName,
                                            brand = photoResult.brand,
                                            caloriesPer100g = photoResult.caloriesPer100g,
                                            proteinPer100g = photoResult.proteinPer100g,
                                            carbsPer100g = photoResult.carbsPer100g,
                                            fatPer100g = photoResult.fatPer100g,
                                            servingSizeDescription = photoResult.servingName,
                                            pieceWeightGrams = photoResult.servingGrams,
                                            pieceUnitName = photoResult.servingName,
                                            hasPieceServing = photoResult.servingGrams > 0f && photoResult.servingGrams != 100f
                                        )
                                        showStatusMessage("Nährwerttabelle erkannt: ${photoResult.productName}")
                                    } else {
                                        showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                                    }
                                } catch (_: Exception) {
                                    showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                                } finally {
                                    _isLookingUpBarcode.value = false
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Fallback to Gemini Vision Barcode reading on scanner failure
                        viewModelScope.launch {
                            try {
                                val photoResult = geminiRepo.analyzeBarcodePhoto(bitmap)
                                if (photoResult.barcode.isNotBlank()) {
                                    val directLookup = nutritionRepo.lookupBarcode(photoResult.barcode)
                                    if (directLookup != null) {
                                        _barcodeLookupResult.value = directLookup
                                        showStatusMessage("Barcode erkannt: ${directLookup.productName}")
                                        _isLookingUpBarcode.value = false
                                        return@launch
                                    }
                                }
                                if (photoResult.productName.isNotBlank() && photoResult.caloriesPer100g > 0) {
                                    _barcodeLookupResult.value = BarcodeLookupResult(
                                        productName = photoResult.productName,
                                        brand = photoResult.brand,
                                        caloriesPer100g = photoResult.caloriesPer100g,
                                        proteinPer100g = photoResult.proteinPer100g,
                                        carbsPer100g = photoResult.carbsPer100g,
                                        fatPer100g = photoResult.fatPer100g,
                                        servingSizeDescription = photoResult.servingName,
                                        pieceWeightGrams = photoResult.servingGrams,
                                        pieceUnitName = photoResult.servingName,
                                        hasPieceServing = photoResult.servingGrams > 0f && photoResult.servingGrams != 100f
                                    )
                                    showStatusMessage("Nährwerttabelle erkannt: ${photoResult.productName}")
                                } else {
                                    showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                                }
                            } catch (_: Exception) {
                                showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                            } finally {
                                _isLookingUpBarcode.value = false
                            }
                        }
                    }
            } catch (_: Exception) {
                // Fallback to Gemini Vision Barcode reading on initial exception
                viewModelScope.launch {
                    try {
                        val photoResult = geminiRepo.analyzeBarcodePhoto(bitmap)
                        if (photoResult.barcode.isNotBlank()) {
                            val directLookup = nutritionRepo.lookupBarcode(photoResult.barcode)
                            if (directLookup != null) {
                                _barcodeLookupResult.value = directLookup
                                showStatusMessage("Barcode erkannt: ${directLookup.productName}")
                                _isLookingUpBarcode.value = false
                                return@launch
                            }
                        }
                        if (photoResult.productName.isNotBlank() && photoResult.caloriesPer100g > 0) {
                            _barcodeLookupResult.value = BarcodeLookupResult(
                                productName = photoResult.productName,
                                brand = photoResult.brand,
                                caloriesPer100g = photoResult.caloriesPer100g,
                                proteinPer100g = photoResult.proteinPer100g,
                                carbsPer100g = photoResult.carbsPer100g,
                                fatPer100g = photoResult.fatPer100g,
                                servingSizeDescription = photoResult.servingName,
                                pieceWeightGrams = photoResult.servingGrams,
                                pieceUnitName = photoResult.servingName,
                                hasPieceServing = photoResult.servingGrams > 0f && photoResult.servingGrams != 100f
                            )
                            showStatusMessage("Nährwerttabelle erkannt: ${photoResult.productName}")
                        } else {
                            showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                        }
                    } catch (_: Exception) {
                        showStatusMessage("Produkt nicht in der Datenbank gefunden, bitte manuell eintragen")
                    } finally {
                        _isLookingUpBarcode.value = false
                    }
                }
            }
        }
    }

    // Gemini Vision Photo Analysis
    fun analyzeFoodPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            _photoAnalysisResult.value = null
            try {
                // Save and upload food scan image to Firebase Storage
                val app = getApplication<android.app.Application>()
                val localSavedPath = com.example.util.ImageUtils.processAndSaveBitmapToAppStorage(app, bitmap, "food_scan")
                if (localSavedPath.isNotBlank()) {
                    launch(Dispatchers.IO) {
                        com.example.util.FirebaseStorageManager.uploadImage(app, localSavedPath, "food_scans")
                    }
                }

                val result = geminiRepo.analyzeFoodPhoto(bitmap)
                _photoAnalysisResult.value = result
                if (!result.isFood) {
                    showStatusMessage(result.errorMessage ?: "Kein Essen erkannt")
                }
            } catch (e: Exception) {
                val errMsg = "API-Fehler: ${e.localizedMessage ?: "Verbindung fehlgeschlagen"}"
                _photoAnalysisResult.value = FoodPhotoAnalysisResult(
                    isFood = false,
                    dishName = "",
                    portion = "",
                    rawCalories = 0,
                    bufferedCalories = 0,
                    proteinGrams = 0f,
                    carbsGrams = 0f,
                    fatGrams = 0f,
                    summary = errMsg,
                    errorMessage = errMsg
                )
                showStatusMessage(errMsg)
            } finally {
                _isAnalyzingPhoto.value = false
            }
        }
    }

    fun clearPhotoAnalysis() {
        _photoAnalysisResult.value = null
    }

    // Workout Management
    fun createCustomExercise(
        name: String,
        category: String,
        defaultSets: Int = 3,
        targetReps: String = "8-12",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val ex = workoutRepo.saveCustomExercise(
                name = name,
                category = category,
                defaultSets = defaultSets,
                targetReps = targetReps,
                notes = notes
            )
            showStatusMessage("Übung '${ex.name}' im Übungsspeicher gesichert!")
        }
    }

    fun deleteCustomExercise(id: String) {
        viewModelScope.launch {
            workoutRepo.deleteCustomExercise(id)
            showStatusMessage("Übung aus Speicher entfernt")
        }
    }

    fun saveCustomWorkoutPlan(plan: com.example.data.model.CustomWorkoutPlan) {
        viewModelScope.launch {
            workoutRepo.saveCustomPlan(plan)
            showStatusMessage("Trainingsplan '${plan.name}' erfolgreich gespeichert!")
        }
    }

    fun deleteCustomWorkoutPlan(id: String) {
        viewModelScope.launch {
            workoutRepo.deleteCustomPlan(id)
            showStatusMessage("Trainingsplan gelöscht")
        }
    }

    fun startWorkoutSession(template: WorkoutSplitTemplate) {
        viewModelScope.launch {
            val isDeload = userProfile.value?.isDeloadActive == true
            val exerciseStates = template.exercises.map { ex ->
                val lastLog = workoutRepo.getLastLogForExercise(ex.name)
                val smartProg = workoutRepo.calculateSmartProgression(
                    exercise = ex,
                    lastLog = lastLog,
                    isDeload = isDeload
                )
                val effectiveBaselineWeight = smartProg.baselineWeightKg
                val effectiveBaselineReps = smartProg.baselineReps
                val suggestedKg = smartProg.suggestedWeightKg
                val suggestedReps = smartProg.suggestedReps

                val updatedExercise = ex.copy(
                    lastWeightKg = effectiveBaselineWeight,
                    lastReps = effectiveBaselineReps,
                    suggestedWeightKg = suggestedKg
                )

                val sets = if (lastLog != null && lastLog.sets.isNotEmpty()) {
                    lastLog.sets.mapIndexed { idx, prevSet ->
                        val weight = if (prevSet.weightKg > 0f) prevSet.weightKg else suggestedKg
                        val reps = if (prevSet.reps > 0) prevSet.reps else suggestedReps
                        ExerciseSet(
                            setNumber = idx + 1,
                            weightKg = weight,
                            reps = reps,
                            isCompleted = false,
                            isWarmup = prevSet.isWarmup,
                            setLabel = prevSet.setLabel.ifBlank { if (prevSet.isWarmup) "W" else "${idx + 1}" }
                        )
                    }
                } else {
                    (1..ex.defaultSets.coerceAtLeast(1)).map { setNum ->
                        val w = if (ex.suggestedWeightKg > 0f) ex.suggestedWeightKg else suggestedKg
                        ExerciseSet(
                            setNumber = setNum,
                            weightKg = w,
                            reps = suggestedReps,
                            isCompleted = false,
                            setLabel = "$setNum"
                        )
                    }
                }

                ActiveExerciseState(
                    exercise = updatedExercise,
                    suggestedWeightKg = suggestedKg,
                    isDeload = isDeload,
                    sets = sets
                )
            }

            _activeWorkout.value = ActiveWorkoutState(
                template = template,
                exercises = exerciseStates,
                startTimeMillis = System.currentTimeMillis()
            )
        }
    }

    fun toggleSetCompletion(exerciseIndex: Int, setIndex: Int, weightKg: Float, reps: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex in current.exercises.indices) {
            val ex = current.exercises[exerciseIndex]
            if (setIndex in ex.sets.indices) {
                val currentSet = ex.sets[setIndex]
                val newCompleted = !currentSet.isCompleted
                val updatedSet = currentSet.copy(
                    weightKg = weightKg,
                    reps = reps,
                    isCompleted = newCompleted
                )
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) updatedSet else s
                }
                val updatedExercises = current.exercises.mapIndexed { idx, item ->
                    if (idx == exerciseIndex) item.copy(sets = updatedSets) else item
                }
                _activeWorkout.value = current.copy(exercises = updatedExercises)

                if (newCompleted) {
                    // Trigger automated rest timer (90 seconds default)
                    startRestTimer(90)
                    showStatusMessage("Satz ${setIndex + 1} abgeschlossen! Pausen-Timer läuft (90s)")
                } else {
                    showStatusMessage("Satz ${setIndex + 1} wieder zur Bearbeitung geöffnet")
                }
            }
        }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, weightKg: Float, reps: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex in current.exercises.indices) {
            val ex = current.exercises[exerciseIndex]
            if (setIndex in ex.sets.indices) {
                val updatedSet = ex.sets[setIndex].copy(
                    weightKg = weightKg,
                    reps = reps,
                    isCompleted = true
                )
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) updatedSet else s
                }
                val updatedExercises = current.exercises.mapIndexed { idx, item ->
                    if (idx == exerciseIndex) item.copy(sets = updatedSets) else item
                }
                _activeWorkout.value = current.copy(exercises = updatedExercises)

                // Trigger automated rest timer (90 seconds default)
                startRestTimer(90)
                showStatusMessage("Satz ${setIndex + 1} abgeschlossen! Pausen-Timer läuft (90s)")
            }
        }
    }

    fun addSetToExercise(exerciseIndex: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex in current.exercises.indices) {
            val ex = current.exercises[exerciseIndex]
            val lastSet = ex.sets.lastOrNull()
            val newSet = ExerciseSet(
                setNumber = ex.sets.size + 1,
                weightKg = lastSet?.weightKg ?: ex.suggestedWeightKg,
                reps = lastSet?.reps ?: 8,
                isCompleted = false
            )
            val updatedExercises = current.exercises.mapIndexed { idx, item ->
                if (idx == exerciseIndex) {
                    item.copy(sets = item.sets + newSet)
                } else {
                    item
                }
            }
            _activeWorkout.value = current.copy(exercises = updatedExercises)
            showStatusMessage("Satz ${ex.sets.size + 1} hinzugefügt")
        }
    }

    fun deleteSetFromExercise(exerciseIndex: Int, setIndex: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex in current.exercises.indices) {
            val ex = current.exercises[exerciseIndex]
            if (setIndex in ex.sets.indices) {
                val remainingSets = ex.sets.filterIndexed { idx, _ -> idx != setIndex }
                    .mapIndexed { newIdx, s -> s.copy(setNumber = newIdx + 1) }
                val updatedExercises = current.exercises.mapIndexed { idx, item ->
                    if (idx == exerciseIndex) {
                        item.copy(sets = remainingSets)
                    } else {
                        item
                    }
                }
                _activeWorkout.value = current.copy(exercises = updatedExercises)
                showStatusMessage("Satz entfernt")
            }
        }
    }

    private val _completedWorkoutSummary = MutableStateFlow<WorkoutCalorieResultSummary?>(null)
    val completedWorkoutSummary: StateFlow<WorkoutCalorieResultSummary?> = _completedWorkoutSummary.asStateFlow()

    fun updateSetValues(exerciseIndex: Int, setIndex: Int, weightKg: Float, reps: Int) {
        val current = _activeWorkout.value ?: return
        if (exerciseIndex in current.exercises.indices) {
            val ex = current.exercises[exerciseIndex]
            if (setIndex in ex.sets.indices) {
                val currentSet = ex.sets[setIndex]
                val updatedSet = currentSet.copy(
                    weightKg = weightKg,
                    reps = reps
                )
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) updatedSet else s
                }
                val updatedExercises = current.exercises.mapIndexed { idx, item ->
                    if (idx == exerciseIndex) item.copy(sets = updatedSets) else item
                }
                _activeWorkout.value = current.copy(exercises = updatedExercises)
            }
        }
    }

    fun dismissWorkoutSummary() {
        _completedWorkoutSummary.value = null
    }

    fun finishWorkoutSession() {
        val current = _activeWorkout.value ?: return
        viewModelScope.launch {
            val durationMinutes = ((System.currentTimeMillis() - current.startTimeMillis) / 60000).toInt().coerceAtLeast(1)
            val logs = current.exercises.map { exState ->
                val updatedSets = exState.sets.map { set ->
                    if (!set.isCompleted && set.weightKg > 0f && set.reps > 0) {
                        set.copy(isCompleted = true)
                    } else {
                        set
                    }
                }
                WorkoutLogEntry(
                    workoutSessionId = "",
                    exerciseName = exState.exercise.name,
                    exerciseCategory = exState.exercise.category,
                    sets = updatedSets
                )
            }
            val profile = userProfile.value ?: UserProfile()
            val calorieResult = workoutRepo.calculateWorkoutCalories(
                userProfile = profile,
                durationMinutes = durationMinutes,
                logs = logs
            )

            val dateStr = LocalDate.now().toString()
            workoutRepo.saveWorkoutSession(
                splitName = current.template.title,
                dateString = dateStr,
                durationMinutes = durationMinutes,
                logs = logs,
                caloriesBurned = calorieResult.totalCaloriesBurned
            )

            // 1. AUTOMATISCHES SPEICHERN NEUER GEWICHTE IN DER ROUTINE
            if (!current.template.planId.isNullOrBlank()) {
                workoutRepo.updatePlanExerciseDefaults(current.template.planId, logs)
            }
            workoutRepo.updateAllMatchingExercisesInPlans(logs)

            _completedWorkoutSummary.value = WorkoutCalorieResultSummary(
                splitName = current.template.title,
                dateString = dateStr,
                result = calorieResult
            )

            _activeWorkout.value = null
            cancelRestTimer()
            showStatusMessage("Workout beendet! 🔥 +${calorieResult.totalCaloriesBurned} kcal • Gewichte in Routine aktualisiert!")
        }
    }

    fun logRestDay() {
        viewModelScope.launch {
            workoutRepo.saveRestDaySession()
            showStatusMessage("Ruhetag eingetragen! Nutze den Tag für Muskelregeneration.")
        }
    }

    fun cancelActiveWorkout() {
        _activeWorkout.value = null
        cancelRestTimer()
    }

    // Rest Timer
    fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        _restTimerSeconds.value = seconds
        _isRestTimerActive.value = true

        timerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
            _isRestTimerActive.value = false
            vibrateDevice()
            showStatusMessage("Pause beendet! Bereit für den nächsten Satz!")
        }
    }

    fun addRestTimerSeconds(seconds: Int) {
        _restTimerSeconds.value += seconds
    }

    fun cancelRestTimer() {
        timerJob?.cancel()
        _isRestTimerActive.value = false
        _restTimerSeconds.value = 0
    }

    private fun vibrateDevice() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (_: Exception) {
            // Ignored on emulators
        }
    }

    // Monday Check-In Popup State & Monday Check
    val isTodayMonday: Boolean
        get() = LocalDate.now().dayOfWeek == java.time.DayOfWeek.MONDAY

    private val _showMondayCheckInPopup = MutableStateFlow(false)
    val showMondayCheckInPopup: StateFlow<Boolean> = _showMondayCheckInPopup.asStateFlow()

    init {
        checkAndTriggerMondayPopup()
    }

    fun saveProgressPhoto(imageUri: String, weightKg: Float? = null, note: String = "Monatlicher Formcheck") {
        viewModelScope.launch {
            val currentWeight = weightKg ?: userProfile.value?.currentWeightKg ?: 80f
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN))
            val photo = ProgressPhoto(
                imageUri = imageUri,
                dateString = dateStr,
                weightKg = currentWeight,
                note = note.ifBlank { "Monatlicher Formcheck" },
                timestamp = System.currentTimeMillis()
            )
            coachRepo.saveProgressPhoto(photo)
            val profile = userProfile.value ?: coachRepo.getProfile()
            if (profile != null) {
                coachRepo.saveProfile(profile.copy(lastProgressPhotoEpoch = System.currentTimeMillis()))
            }
            showStatusMessage("Fortschritts-Foto erfolgreich gesichert!")
        }
    }

    fun deleteProgressPhoto(id: String) {
        viewModelScope.launch {
            coachRepo.deleteProgressPhoto(id)
            showStatusMessage("Foto entfernt.")
        }
    }

    fun checkAndTriggerMondayPopup() {
        if (isTodayMonday) {
            viewModelScope.launch {
                val profile = coachRepo.getProfile()
                if (profile != null && profile.isOnboarded) {
                    val lastCheckinDays = (System.currentTimeMillis() - profile.lastMondayCheckinEpoch) / (1000 * 60 * 60 * 24)
                    // If not checked in today (more than 12 hours since last checkin)
                    if (lastCheckinDays >= 1 || profile.lastMondayCheckinEpoch == 0L) {
                        _showMondayCheckInPopup.value = true
                    }
                }
            }
        }
    }

    fun dismissMondayPopup() {
        _showMondayCheckInPopup.value = false
    }

    fun getNextMondayDateFormatted(): String {
        var date = LocalDate.now()
        if (date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            date = date.plusWeeks(1)
        } else {
            while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                date = date.plusDays(1)
            }
        }
        val formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
        return date.format(formatter)
    }

    // Monday Check-In & Deload
    fun submitMondayCheckIn(
        weightKg: Float,
        feelingScore: Int,
        note: String,
        heightCm: Int? = null
    ) {
        viewModelScope.launch {
            val updated = coachRepo.performMondayCheckin(weightKg, feelingScore, note, heightCm)
            _showMondayCheckInPopup.value = false
            val deloadMsg = if (updated.isDeloadActive) " Woche 4 Deload aktiviert (-20% Gewichte)." else ""
            showStatusMessage("Montags-Check-in gespeichert! Kalorienziel: ${updated.dailyCalorieTarget} kcal.$deloadMsg")
        }
    }

    fun updateGoalSettings(newTargetWeightKg: Float, newWeeklyGainKg: Float) {
        viewModelScope.launch {
            val updated = coachRepo.updateGoalSettings(newTargetWeightKg, newWeeklyGainKg)
            showStatusMessage("Ziel aktualisiert: ${updated.targetWeightKg} kg (+${updated.weeklyWeightGainKg} kg/Wo) -> ${updated.dailyCalorieTarget} kcal")
        }
    }

    fun toggleDeload(active: Boolean) {
        viewModelScope.launch {
            coachRepo.setDeloadState(active)
            showStatusMessage(if (active) "Deload-Woche aktiviert (-20% Gewichte)" else "Normaler Bulk-Modus aktiv")
        }
    }

    // Coach Chat
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = text.trim())
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isCoachReplying.value = true
            val profile = userProfile.value
            val summary = todayNutritionSummary.value
            val todayDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
            val context = """
                Datum: $todayDateStr
                Athlet: ${profile?.name ?: "Athlet"}
                Gewicht: ${profile?.currentWeightKg ?: 80} kg
                Trainingswoche: ${profile?.currentTrainingWeek ?: 1} (Deload: ${if (profile?.isDeloadActive == true) "JA (-20% Gewichte)" else "NEIN"})
                Split: ${profile?.trainingSplit ?: "Push / Pull / Legs"}
                
                Nutzer-Status Heute:
                - Ziel: ${summary.targetCalories} kcal | Ist: ${summary.eatenCalories} kcal | FEHLT NOCH: ${summary.remainingCalories} kcal
                - Eiweiß Ziel: ${summary.targetProtein}g | Ist: ${summary.eatenProtein.toInt()}g | FEHLT NOCH: ${summary.remainingProtein.toInt()}g
                - Kohlenhydrate Ziel: ${summary.targetCarbs}g | Ist: ${summary.eatenCarbs.toInt()}g | FEHLT NOCH: ${summary.remainingCarbs.toInt()}g
                - Fett Ziel: ${summary.targetFat}g | Ist: ${summary.eatenFat.toInt()}g | FEHLT NOCH: ${summary.remainingFat.toInt()}g
            """.trimIndent()

            val reply = geminiRepo.getCoachResponse(text, context)
            val coachMsg = ChatMessage(sender = "coach", text = reply)
            _chatMessages.value = _chatMessages.value + coachMsg
            _isCoachReplying.value = false
        }
    }

    fun clearLatestCalisthenicsResult() {
        _latestCalisthenicsResult.value = null
    }

    fun analyzeCalisthenicsVideoOrPhoto(
        skillName: String,
        mediaUri: android.net.Uri?,
        directBitmap: Bitmap?,
        isVideo: Boolean,
        userNotes: String = ""
    ) {
        val safeSkill = skillName.ifBlank { "Calisthenics Skill" }
        viewModelScope.launch {
            _isAnalyzingCalisthenics.value = true
            _latestCalisthenicsResult.value = null

            // 1. Log User Request in Chat
            val mediaLabel = if (isVideo) "📹 [Video-Upload]" else "📸 [Foto-Upload]"
            val notesSuffix = if (userNotes.isNotBlank()) "\nNotiz: $userNotes" else ""
            val userMsg = ChatMessage(
                sender = "user",
                text = "$mediaLabel Calisthenics Form-Analyse: **$safeSkill**$notesSuffix"
            )
            _chatMessages.value = _chatMessages.value + userMsg

            // 2. Extract Bitmaps
            val context = getApplication<Application>()
            val bitmaps = mutableListOf<Bitmap>()
            if (directBitmap != null) {
                bitmaps.add(directBitmap)
            } else if (mediaUri != null) {
                try {
                    if (isVideo) {
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, mediaUri)
                            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val durationUs = (durationStr?.toLongOrNull() ?: 3000L) * 1000L

                            val times = listOf(
                                (durationUs * 0.15).toLong(),
                                (durationUs * 0.45).toLong(),
                                (durationUs * 0.75).toLong()
                            )
                            for (t in times) {
                                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                    retriever.getScaledFrameAtTime(t, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 720, 720)
                                } else {
                                    retriever.getFrameAtTime(t, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                }
                                if (frame != null) bitmaps.add(frame)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BulkCoachViewModel", "Error reading video frames: ${e.message}")
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    } else {
                        val inputStream = context.contentResolver.openInputStream(mediaUri)
                        val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bmp != null) bitmaps.add(bmp)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BulkCoachViewModel", "Error loading media: ${e.message}")
                }
            }

            // 3. Perform Gemini Vision Analysis
            val analysisResult = geminiRepo.analyzeCalisthenicsSkill(
                skillName = safeSkill,
                bitmaps = bitmaps,
                isVideo = isVideo,
                userNotes = userNotes
            ).copy(
                mediaUri = mediaUri?.toString(),
                isVideo = isVideo
            )

            _latestCalisthenicsResult.value = analysisResult
            _calisthenicsHistory.value = listOf(analysisResult) + _calisthenicsHistory.value

            // 4. Format structured Chat Message response
            val weakPointsFormatted = analysisResult.weakPoints.joinToString("\n") { "• $it" }
            val weakMusclesFormatted = analysisResult.weakMuscles.joinToString("\n") { "• $it" }
            val drillsFormatted = analysisResult.recommendedDrills.joinToString("\n\n") { drill ->
                "• **${drill.name}**\n  ⏱️ ${drill.setsAndReps} | 🎯 ${drill.targetMuscles}${if (drill.tip.isNotBlank()) "\n  💡 ${drill.tip}" else ""}"
            }

            val coachChatText = """
                🎯 **Auswertung & Form-Check: ${analysisResult.skillName}**
                **Bewertung:** ${analysisResult.gradeText}
                ${analysisResult.formCheckSummary}

                ⚠️ **Gefundene Schwachstellen:**
                $weakPointsFormatted

                💪 **Geschwächte Muskelgruppen:**
                $weakMusclesFormatted

                🏋️‍♂️ **Empfohlene Hilfsübungen:**
                $drillsFormatted
            """.trimIndent()

            val coachMsg = ChatMessage(
                sender = "coach",
                text = coachChatText,
                calisthenicsAnalysis = analysisResult
            )
            _chatMessages.value = _chatMessages.value + coachMsg

            _isAnalyzingCalisthenics.value = false
            showStatusMessage("Calisthenics Form-Analyse abgeschlossen: ${analysisResult.gradeText}")
        }
    }

    fun addCalisthenicsDrillToExercises(drill: CalisthenicsAssistanceDrill) {
        viewModelScope.launch {
            val ex = workoutRepo.saveCustomExercise(
                name = drill.name,
                category = "Calisthenics / Skills",
                defaultSets = 3,
                targetReps = drill.setsAndReps,
                notes = "${drill.targetMuscles}. ${drill.tip}".trim()
            )
            showStatusMessage("Assistenzübung '${ex.name}' zum Übungsspeicher hinzugefügt!")
        }
    }

    // Local JSON Backup Export & Import
    fun exportBackup(targetUri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = coachRepo.exportBackupToJsonUri(targetUri)
            if (success) {
                showStatusMessage("Backup erfolgreich gespeichert!")
            } else {
                showStatusMessage("Fehler beim Exportieren des Backups.")
            }
            onResult(success)
        }
    }

    fun importBackup(sourceUri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = coachRepo.importBackupFromJsonUri(sourceUri)
            if (success) {
                coachRepo.getOrRestoreProfile()
                showStatusMessage("Daten erfolgreich wiederhergestellt!")
            } else {
                showStatusMessage("Fehler beim Importieren des Backups. Ungültiges Format?")
            }
            onResult(success)
        }
    }

    fun generatePerformanceReport() {
        viewModelScope.launch {
            _isAnalyzingPerformance.value = true
            showStatusMessage("KI analysiert deine Performance-Daten...")
            try {
                val report = coachRepo.generatePerformanceReport()
                _performanceReport.value = report
                showStatusMessage("Performance-Analyse erfolgreich generiert!")
            } catch (e: Exception) {
                e.printStackTrace()
                showStatusMessage("Analyse fehlgeschlagen: ${e.message}")
            } finally {
                _isAnalyzingPerformance.value = false
            }
        }
    }
}


data class DailyNutritionSummary(
    val baseCalorieTarget: Int = 0,
    val workoutCaloriesBurned: Int = 0,
    val targetCalories: Int = 0,
    val eatenCalories: Int = 0,
    val remainingCalories: Int = 0,
    val targetProtein: Int = 0,
    val eatenProtein: Float = 0f,
    val remainingProtein: Float = 0f,
    val targetCarbs: Int = 0,
    val eatenCarbs: Float = 0f,
    val remainingCarbs: Float = 0f,
    val targetFat: Int = 0,
    val eatenFat: Float = 0f,
    val remainingFat: Float = 0f
)

data class ActiveWorkoutState(
    val template: WorkoutSplitTemplate,
    val exercises: List<ActiveExerciseState>,
    val startTimeMillis: Long
)

data class ActiveExerciseState(
    val exercise: Exercise,
    val suggestedWeightKg: Float,
    val isDeload: Boolean,
    val sets: List<ExerciseSet>
)

data class LifesumProjectionResult(
    val tdeeKcal: Int,
    val dailySurplusKcal: Int,
    val targetKcal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val totalGainKg: Float,
    val estimatedWeeks: Int,
    val estimatedTargetDate: String
)

package com.example.data.model

data class UserProfile(
    val id: String = "primary_user",
    val email: String = "",
    val name: String = "",
    val profileImageUri: String? = null,
    val heightCm: Int = 180,
    val currentWeightKg: Float = 75.0f,
    val startWeightKg: Float = 75.0f,
    val targetWeightKg: Float = 85.0f,
    val weeklyWeightGainKg: Float = 0.5f,
    val age: Int = 24,
    val gender: String = "männlich", // männlich / weiblich
    val primaryGoal: String = "Masseaufbau", // Masseaufbau, Lean Bulk, Kraftaufbau
    val activityLevel: String = "Moderat (3-5x Training/Woche)",
    val dailyCalorieTarget: Int = 3200,
    val proteinTargetGrams: Int = 160,
    val carbsTargetGrams: Int = 400,
    val fatTargetGrams: Int = 85,
    val isOnboarded: Boolean = false,
    val currentTrainingWeek: Int = 1,
    val isDeloadActive: Boolean = false,
    val trainingSplit: String = "Push / Pull / Legs",
    val bufferPercentage: Int = 8, // 8% Bulking-Puffer
    val lastMondayCheckinEpoch: Long = 0L,
    val lastProgressPhotoEpoch: Long = 0L,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

data class ProgressPhoto(
    val id: String = java.util.UUID.randomUUID().toString(),
    val imageUri: String,
    val dateString: String,
    val weightKg: Float,
    val note: String = "Monatlicher Formcheck",
    val timestamp: Long = System.currentTimeMillis()
)

data class MacroNutrients(
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float
)

enum class MealType(val displayName: String) {
    BREAKFAST("Frühstück"),
    LUNCH("Mittagessen"),
    DINNER("Abendessen"),
    SNACK_SHAKE("Snacks & Bulk-Shakes")
}

data class FoodItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val portionDescription: String = "1 Portion",
    val rawCalories: Int,
    val bufferedCalories: Int, // after 8% safety buffer deduction
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val mealType: MealType = MealType.LUNCH,
    val dateString: String, // YYYY-MM-DD
    val isAiEstimated: Boolean = false,
    val barcode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class Exercise(
    val id: String,
    val name: String,
    val category: String, // Brust, Rücken, Beine, Schultern, Arme, Core
    val defaultSets: Int = 3,
    val targetReps: String = "8-12",
    val lastWeightKg: Float = 0f,
    val lastReps: Int = 0,
    val suggestedWeightKg: Float = 0f,
    val notes: String = ""
)

data class ExerciseSet(
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val isCompleted: Boolean = false,
    val rpe: Int = 8,
    val isWarmup: Boolean = false,
    val setLabel: String = ""
)

data class WorkoutSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val splitName: String, // e.g. "Push A: Brust & Schultern"
    val dateString: String,
    val durationMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val totalVolumeKg: Float = 0f,
    val caloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkoutCalorieResult(
    val totalCaloriesBurned: Int,
    val durationMinutes: Int,
    val totalVolumeKg: Float,
    val completedSetsCount: Int,
    val totalExercisesCount: Int,
    val baseMetCalories: Int,
    val volumeLoadCalories: Int,
    val intensityBonusCalories: Int,
    val compoundExercisesSummary: String
)

data class WorkoutCalorieResultSummary(
    val splitName: String,
    val dateString: String,
    val result: WorkoutCalorieResult
)

data class WorkoutLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val workoutSessionId: String,
    val exerciseName: String,
    val exerciseCategory: String,
    val sets: List<ExerciseSet>,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomWorkoutDay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<Exercise> = emptyList()
)

data class CustomWorkoutPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "Individueller Trainingsplan",
    val days: List<CustomWorkoutDay> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class WeightCheckIn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateString: String,
    val weightKg: Float,
    val feelingScore: Int, // 1-10
    val waistCircumferenceCm: Float? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "coach"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActionPrompt: Boolean = false,
    val calisthenicsAnalysis: CalisthenicsAnalysisResult? = null
)

data class CalisthenicsAssistanceDrill(
    val name: String,
    val setsAndReps: String, // e.g. "4 Sätze x 5 Wdh"
    val targetMuscles: String, // e.g. "Oberer Rücken & Latissimus"
    val tip: String = ""
)

data class CalisthenicsAnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val skillName: String = "Calisthenics Skill",
    val scorePercentage: Int = 75,
    val gradeText: String = "75% Sauberkeit",
    val formCheckSummary: String, // 🎯 Auswertung & Form-Check
    val weakPoints: List<String> = emptyList(), // ⚠️ Gefundene Schwachstellen
    val weakMuscles: List<String> = emptyList(), // 💪 Geschwächte Muskelgruppen
    val recommendedDrills: List<CalisthenicsAssistanceDrill> = emptyList(), // 🏋️‍♂️ Empfohlene Hilfsübungen
    val mediaUri: String? = null,
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class PerformanceReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val generatedAtEpoch: Long = System.currentTimeMillis(),
    val workoutAnalysis: String = "",
    val visualAndWeightAnalysis: String = "",
    val muscleAndVTaperAnalysis: String = "",
    val nutritionAnalysis: String = "",
    val actionPlan: List<String> = emptyList(),
    val photoUrisUsed: List<String> = emptyList(),
    val hasEnoughPhotos: Boolean = true
)

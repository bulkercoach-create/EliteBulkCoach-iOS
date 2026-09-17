package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.ExerciseSet
import com.example.data.model.MealType
import org.json.JSONArray
import org.json.JSONObject

class RoomConverters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = runCatching { MealType.valueOf(value) }.getOrDefault(MealType.LUNCH)

    @TypeConverter
    fun fromExerciseSetList(list: List<ExerciseSet>): String {
        val array = JSONArray()
        list.forEach { set ->
            val obj = JSONObject()
            obj.put("setNumber", set.setNumber)
            obj.put("weightKg", set.weightKg.toDouble())
            obj.put("reps", set.reps)
            obj.put("isCompleted", set.isCompleted)
            obj.put("rpe", set.rpe)
            obj.put("isWarmup", set.isWarmup)
            obj.put("setLabel", set.setLabel)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toExerciseSetList(json: String): List<ExerciseSet> {
        val list = mutableListOf<ExerciseSet>()
        runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ExerciseSet(
                        setNumber = obj.optInt("setNumber", i + 1),
                        weightKg = obj.optDouble("weightKg", 0.0).toFloat(),
                        reps = obj.optInt("reps", 0),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        rpe = obj.optInt("rpe", 8),
                        isWarmup = obj.optBoolean("isWarmup", false),
                        setLabel = obj.optString("setLabel", "")
                    )
                )
            }
        }
        return list
    }
}

@Entity(
    tableName = "user_accounts",
    indices = [androidx.room.Index(value = ["email"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "primary_user",
    val email: String,
    val name: String,
    val profileImageUri: String? = null,
    val heightCm: Int,
    val currentWeightKg: Float,
    val startWeightKg: Float,
    val targetWeightKg: Float,
    val weeklyWeightGainKg: Float = 0.5f,
    val age: Int,
    val gender: String,
    val primaryGoal: String,
    val activityLevel: String,
    val dailyCalorieTarget: Int,
    val proteinTargetGrams: Int,
    val carbsTargetGrams: Int,
    val fatTargetGrams: Int,
    val isOnboarded: Boolean,
    val currentTrainingWeek: Int,
    val isDeloadActive: Boolean,
    val trainingSplit: String,
    val bufferPercentage: Int,
    val lastMondayCheckinEpoch: Long,
    val lastProgressPhotoEpoch: Long = 0L,
    val createdAtEpoch: Long
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val portionDescription: String,
    val rawCalories: Int,
    val bufferedCalories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val mealType: MealType,
    val dateString: String,
    val isAiEstimated: Boolean,
    val barcode: String?,
    val timestamp: Long
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val splitName: String,
    val dateString: String,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val totalVolumeKg: Float,
    val caloriesBurned: Int = 0,
    val timestamp: Long
)

@Entity(tableName = "workout_logs")
data class WorkoutLogEntryEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseName: String,
    val exerciseCategory: String,
    val sets: List<ExerciseSet>,
    val timestamp: Long
)

@Entity(tableName = "weight_checkins")
data class WeightCheckInEntity(
    @PrimaryKey val id: String,
    val dateString: String,
    val weightKg: Float,
    val feelingScore: Int,
    val waistCircumferenceCm: Float?,
    val note: String,
    val timestamp: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isActionPrompt: Boolean
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val defaultSets: Int = 3,
    val targetReps: String = "8-12",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_workout_plans")
data class CustomWorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "Individueller Trainingsplan",
    val daysJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_photos")
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val imageUri: String,
    val dateString: String,
    val weightKg: Float,
    val note: String = "Monatlicher Formcheck",
    val timestamp: Long = System.currentTimeMillis()
)


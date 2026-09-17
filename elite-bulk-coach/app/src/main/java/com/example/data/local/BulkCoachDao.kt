package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BulkCoachDao {

    // User Accounts
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccount(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllUserAccounts(): List<UserAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(account: UserAccountEntity)

    @Update
    suspend fun updateUserAccount(account: UserAccountEntity)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 'primary_user' LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'primary_user' LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()

    // Food Items
    @Query("SELECT * FROM food_items WHERE dateString = :date ORDER BY timestamp ASC")
    fun getFoodsForDateFlow(date: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE dateString = :date ORDER BY timestamp ASC")
    suspend fun getFoodsForDate(date: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE dateString BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getFoodsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY timestamp DESC")
    fun getAllFoodsFlow(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY timestamp DESC")
    suspend fun getAllFoods(): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItemEntity)

    @Update
    suspend fun updateFood(food: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFood(id: String)

    @Query("DELETE FROM food_items WHERE dateString = :date")
    suspend fun deleteFoodsForDate(date: String)

    @Query("DELETE FROM food_items")
    suspend fun clearAllFoods()

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllWorkoutSessionsFlow(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllWorkoutSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE dateString = :date LIMIT 1")
    suspend fun getWorkoutSessionForDate(date: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_logs WHERE workoutSessionId = :sessionId")
    fun getWorkoutLogsForSessionFlow(sessionId: String): Flow<List<WorkoutLogEntryEntity>>

    @Query("SELECT * FROM workout_logs WHERE LOWER(TRIM(exerciseName)) = LOWER(TRIM(:exerciseName)) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastWorkoutLogForExercise(exerciseName: String): WorkoutLogEntryEntity?

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkoutLogsFlow(): Flow<List<WorkoutLogEntryEntity>>

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntryEntity)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearWorkouts()

    // Weight Check-ins
    @Query("SELECT * FROM weight_checkins ORDER BY timestamp DESC")
    fun getAllCheckInsFlow(): Flow<List<WeightCheckInEntity>>

    @Query("SELECT * FROM weight_checkins ORDER BY timestamp DESC")
    suspend fun getAllCheckIns(): List<WeightCheckInEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: WeightCheckInEntity)

    @Query("DELETE FROM weight_checkins")
    suspend fun clearCheckIns()

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()

    // Custom Exercises
    @Query("SELECT * FROM custom_exercises ORDER BY name ASC")
    fun getAllCustomExercisesFlow(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises ORDER BY name ASC")
    suspend fun getAllCustomExercises(): List<CustomExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomExercise(exercise: CustomExerciseEntity)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun deleteCustomExercise(id: String)

    // Custom Workout Plans
    @Query("SELECT * FROM custom_workout_plans ORDER BY timestamp DESC")
    fun getAllCustomPlansFlow(): Flow<List<CustomWorkoutPlanEntity>>

    @Query("SELECT * FROM custom_workout_plans ORDER BY timestamp DESC")
    suspend fun getAllCustomPlans(): List<CustomWorkoutPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPlan(plan: CustomWorkoutPlanEntity)

    @Query("DELETE FROM custom_workout_plans WHERE id = :id")
    suspend fun deleteCustomPlan(id: String)

    @Query("DELETE FROM custom_workout_plans")
    suspend fun clearCustomPlans()

    // Progress Photos (Formchecks)
    @Query("SELECT * FROM progress_photos ORDER BY timestamp DESC")
    fun getAllProgressPhotosFlow(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photos ORDER BY timestamp DESC")
    suspend fun getAllProgressPhotos(): List<ProgressPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(photo: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun deleteProgressPhoto(id: String)
}


package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.CustomWorkoutPlanEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.ProgressPhotoEntity
import com.example.data.local.RoomConverters
import com.example.data.local.UserAccountEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightCheckInEntity
import com.example.data.local.WorkoutLogEntryEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.model.PerformanceReport
import com.example.data.model.ProgressPhoto
import com.example.data.model.UserProfile
import com.example.data.model.WeightCheckIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CoachRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.coachDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("bulk_coach_prefs", Context.MODE_PRIVATE)

    val userProfileFlow: Flow<UserProfile?> = dao.getUserProfileFlow().map { entity ->
        entity?.toModel()
    }

    val checkInsFlow: Flow<List<WeightCheckIn>> = dao.getAllCheckInsFlow().map { list ->
        list.map { it.toModel() }
    }

    val progressPhotosFlow: Flow<List<ProgressPhoto>> = dao.getAllProgressPhotosFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun saveProgressPhoto(photo: ProgressPhoto) = withContext(Dispatchers.IO) {
        val uploadedUri = if (photo.imageUri.isNotBlank()) {
            com.example.util.FirebaseStorageManager.uploadImage(context, photo.imageUri, "progress_photos")
        } else photo.imageUri
        dao.insertProgressPhoto(photo.copy(imageUri = uploadedUri).toEntity())
        saveFullCloudVaultBackup()
    }

    suspend fun deleteProgressPhoto(id: String) = withContext(Dispatchers.IO) {
        dao.deleteProgressPhoto(id)
        saveFullCloudVaultBackup()
    }

    suspend fun getProfile(): UserProfile? = withContext(Dispatchers.IO) {
        getOrRestoreProfile()
    }

    suspend fun getOrRestoreProfile(): UserProfile? = withContext(Dispatchers.IO) {
        var profile = dao.getUserProfile()?.toModel()
        if (profile == null) {
            val restored = restoreFullCloudVaultBackup(null)
            if (restored) {
                profile = dao.getUserProfile()?.toModel()
            }
        }
        profile
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateProfile(profile.toEntity())
        saveCloudSnapshot(profile)
        saveFullCloudVaultBackup(profile.email)
    }

    suspend fun completeOnboarding(
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
    ): UserProfile = withContext(Dispatchers.IO) {
        // Calculate Bulking Calorie Target based on Mifflin-St Jeor Equation
        // BMR: 10 * weight (kg) + 6.25 * height (cm) - 5 * age + (5 for male, -161 for female)
        val s = if (gender.lowercase().contains("weib") || gender.lowercase() == "female") -161 else 5
        val bmr = (10.0 * currentWeightKg) + (6.25 * heightCm) - (5.0 * age) + s
        val tdee = bmr * 1.55 // Active bulk training factor
        
        val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) || goal.contains("Definition", ignoreCase = true) || goal.contains("Cut", ignoreCase = true)
        val isMaintain = goal.contains("Halten", ignoreCase = true) || goal.contains("Recomp", ignoreCase = true)

        val targetKcal = when {
            isFatLoss -> {
                val deficit = (weeklyWeightGainKg * 1100f).toInt().coerceIn(200, 1000)
                (tdee - deficit).toInt().coerceAtLeast(1500)
            }
            isMaintain -> tdee.toInt()
            else -> {
                val calculatedSurplus = (weeklyWeightGainKg * 1100f).toInt().coerceIn(150, 1000)
                (tdee + calculatedSurplus).toInt()
            }
        }

        // Macros: Protein = 2.2g/kg, Fat = 1.0g/kg, Remainder = Carbs
        val proteinG = (currentWeightKg * 2.2f).toInt()
        val fatG = (currentWeightKg * 1.0f).toInt()
        val proteinKcal = proteinG * 4
        val fatKcal = fatG * 9
        val remainingKcal = (targetKcal - proteinKcal - fatKcal).coerceAtLeast(400)
        val carbsG = (remainingKcal / 4)

        val uploadedAvatar = if (!profileImageUri.isNullOrBlank()) {
            com.example.util.FirebaseStorageManager.uploadImage(context, profileImageUri, "profile_avatars")
        } else profileImageUri

        val newProfile = UserProfile(
            id = "primary_user",
            email = email.trim(),
            name = name.trim(),
            profileImageUri = uploadedAvatar,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            startWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            weeklyWeightGainKg = weeklyWeightGainKg,
            age = age,
            gender = gender,
            primaryGoal = goal,
            activityLevel = "3-5x Krafttraining/Woche",
            dailyCalorieTarget = targetKcal,
            proteinTargetGrams = proteinG,
            carbsTargetGrams = carbsG,
            fatTargetGrams = fatG,
            isOnboarded = true,
            currentTrainingWeek = 1,
            isDeloadActive = false,
            trainingSplit = split,
            bufferPercentage = 8,
            lastMondayCheckinEpoch = System.currentTimeMillis()
        )

        dao.insertOrUpdateProfile(newProfile.toEntity())
        saveCloudSnapshot(newProfile)
        prefs.edit().putBoolean("is_logged_in", true).putString("active_user_email", email.trim().lowercase()).apply()
        saveFullCloudVaultBackup(newProfile.email)
        newProfile
    }

    suspend fun updateProfileImage(imageUri: String?) = withContext(Dispatchers.IO) {
        val current = dao.getUserProfile()?.toModel() ?: return@withContext
        val uploadedAvatar = if (!imageUri.isNullOrBlank()) {
            com.example.util.FirebaseStorageManager.uploadImage(context, imageUri, "profile_avatars")
        } else imageUri
        val updated = current.copy(profileImageUri = uploadedAvatar)
        dao.insertOrUpdateProfile(updated.toEntity())
        saveCloudSnapshot(updated)
        saveFullCloudVaultBackup(updated.email)
    }

    suspend fun performMondayCheckin(
        newWeightKg: Float,
        feelingScore: Int,
        note: String,
        newHeightCm: Int? = null
    ): UserProfile = withContext(Dispatchers.IO) {
        val currentProfile = dao.getUserProfile()?.toModel() ?: UserProfile()
        val checkInEntity = WeightCheckInEntity(
            id = java.util.UUID.randomUUID().toString(),
            dateString = java.time.LocalDate.now().toString(),
            weightKg = newWeightKg,
            feelingScore = feelingScore,
            waistCircumferenceCm = null,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        dao.insertCheckIn(checkInEntity)

        // Advance week or trigger Deload
        val nextWeek = currentProfile.currentTrainingWeek + 1
        val shouldDeload = nextWeek % 4 == 0 // Every 4th week is Deload

        // Recalculate target if weight stagnated (increase kcal) or gained too fast
        val weightDiff = newWeightKg - currentProfile.currentWeightKg
        var adjustedKcal = currentProfile.dailyCalorieTarget
        if (weightDiff < 0.15f) {
            // Plateau: Increase calories by 150 kcal for bulk
            adjustedKcal += 150
        } else if (weightDiff > 0.8f) {
            // Gaining too fast (fat gain): decrease slightly by 100 kcal
            adjustedKcal = (adjustedKcal - 100).coerceAtLeast(2400)
        }

        val updatedProfile = currentProfile.copy(
            currentWeightKg = newWeightKg,
            heightCm = newHeightCm ?: currentProfile.heightCm,
            currentTrainingWeek = if (nextWeek > 8) 1 else nextWeek,
            isDeloadActive = shouldDeload,
            dailyCalorieTarget = adjustedKcal,
            lastMondayCheckinEpoch = System.currentTimeMillis()
        )

        dao.insertOrUpdateProfile(updatedProfile.toEntity())
        saveCloudSnapshot(updatedProfile)
        saveFullCloudVaultBackup(updatedProfile.email)
        updatedProfile
    }

    suspend fun updateGoalSettings(
        newTargetWeightKg: Float,
        newWeeklyWeightGainKg: Float
    ): UserProfile = withContext(Dispatchers.IO) {
        val current = dao.getUserProfile()?.toModel() ?: UserProfile()
        val s = if (current.gender.lowercase().contains("weib") || current.gender.lowercase() == "female") -161 else 5
        val bmr = (10.0 * current.currentWeightKg) + (6.25 * current.heightCm) - (5.0 * current.age) + s
        val tdee = bmr * 1.55
        val calculatedSurplus = (newWeeklyWeightGainKg * 1100f).toInt().coerceIn(150, 1200)
        val targetKcal = (tdee + calculatedSurplus).toInt()

        val proteinG = (current.currentWeightKg * 2.2f).toInt()
        val fatG = (current.currentWeightKg * 1.0f).toInt()
        val remainingKcal = (targetKcal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(400)
        val carbsG = (remainingKcal / 4)

        val updated = current.copy(
            targetWeightKg = newTargetWeightKg,
            weeklyWeightGainKg = newWeeklyWeightGainKg,
            dailyCalorieTarget = targetKcal,
            proteinTargetGrams = proteinG,
            carbsTargetGrams = carbsG,
            fatTargetGrams = fatG
        )

        dao.insertOrUpdateProfile(updated.toEntity())
        saveCloudSnapshot(updated)
        saveFullCloudVaultBackup(updated.email)
        updated
    }

    suspend fun setDeloadState(active: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getUserProfile()?.toModel() ?: return@withContext
        val updated = current.copy(isDeloadActive = active)
        dao.insertOrUpdateProfile(updated.toEntity())
        saveCloudSnapshot(updated)
        saveFullCloudVaultBackup(updated.email)
    }

    suspend fun logoutAndClear() = withContext(Dispatchers.IO) {
        dao.clearProfile()
        dao.clearAllFoods()
        dao.clearWorkouts()
        dao.clearCheckIns()
        dao.clearChat()
        dao.clearCustomPlans()
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("active_user_email")
            .apply()
    }

    suspend fun syncUserCustomPlans(cleanEmail: String) = withContext(Dispatchers.IO) {
        dao.clearCustomPlans()
        val key = "cloud_plans_$cleanEmail"
        val plansJson = prefs.getString(key, null)
        if (plansJson != null && plansJson.isNotBlank()) {
            val plans = com.example.data.local.DatabaseSeeder.deserializePlans(plansJson)
            plans.forEach { dao.insertCustomPlan(it) }
        } else if (cleanEmail == "yamenkhalouf7@gmail.com") {
            // Seed Yamen default plans
            val yamenPlans = com.example.data.local.DatabaseSeeder.getYamenDefaultPlans()
            yamenPlans.forEach { dao.insertCustomPlan(it) }
            val serialized = com.example.data.local.DatabaseSeeder.serializePlans(yamenPlans)
            prefs.edit().putString(key, serialized).apply()
        }
    }

    private fun getPublicVaultDirectories(): List<java.io.File> {
        val dirs = mutableListOf<java.io.File>()
        runCatching {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) dirs.add(java.io.File(extDir, "EliteBulkCoachVault"))
        }
        runCatching {
            val docDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            if (docDir != null) dirs.add(java.io.File(docDir, "EliteBulkCoachVault"))
        }
        runCatching {
            val dlDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null) dirs.add(java.io.File(dlDir, "EliteBulkCoachVault"))
        }
        runCatching { dirs.add(java.io.File("/sdcard/Documents/EliteBulkCoachVault")) }
        runCatching { dirs.add(java.io.File("/sdcard/Download/EliteBulkCoachVault")) }
        runCatching { dirs.add(java.io.File("/sdcard/EliteBulkCoachVault")) }
        return dirs
    }

    private fun compressUriToBase64(imageUriStr: String): String? {
        if (imageUriStr.isBlank()) return null
        return try {
            val bitmap = com.example.util.ImageUtils.getRotatedBitmap(context, imageUriStr) ?: return null

            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                android.graphics.Bitmap.createScaledBitmap(bitmap, w, h, true)
            } else {
                bitmap
            }

            val baos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, baos)
            val compressedBytes = baos.toByteArray()
            android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveFullCloudVaultBackup(email: String? = null) = withContext(Dispatchers.IO) {
        val cleanEmail = email?.trim()?.lowercase() 
            ?: dao.getUserProfile()?.email?.trim()?.lowercase() 
            ?: prefs.getString("active_user_email", null) 
            ?: "primary_athlete"

        try {
            val profile = dao.getUserProfile()
            val checkIns = dao.getAllCheckIns()
            val photos = dao.getAllProgressPhotos()
            val foods = dao.getAllFoods()
            val sessions = dao.getAllWorkoutSessions()
            val logs = dao.getAllWorkoutLogs()
            val plans = dao.getAllCustomPlans()
            val account = dao.getUserAccount(cleanEmail)

            val checkInsArray = org.json.JSONArray()
            checkIns.forEach { c ->
                checkInsArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("dateString", c.dateString)
                    put("weightKg", c.weightKg.toDouble())
                    put("feelingScore", c.feelingScore)
                    put("waistCircumferenceCm", c.waistCircumferenceCm?.toDouble() ?: -1.0)
                    put("note", c.note)
                    put("timestamp", c.timestamp)
                })
            }

            val photosArray = org.json.JSONArray()
            photos.forEach { p ->
                val base64Data = compressUriToBase64(p.imageUri) ?: ""
                photosArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("imageUri", p.imageUri)
                    put("base64Data", base64Data)
                    put("dateString", p.dateString)
                    put("weightKg", p.weightKg.toDouble())
                    put("note", p.note)
                    put("timestamp", p.timestamp)
                })
            }

            val foodsArray = org.json.JSONArray()
            foods.forEach { f ->
                foodsArray.put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("portionDescription", f.portionDescription)
                    put("rawCalories", f.rawCalories)
                    put("bufferedCalories", f.bufferedCalories)
                    put("proteinGrams", f.proteinGrams.toDouble())
                    put("carbsGrams", f.carbsGrams.toDouble())
                    put("fatGrams", f.fatGrams.toDouble())
                    put("mealType", f.mealType.name)
                    put("dateString", f.dateString)
                    put("isAiEstimated", f.isAiEstimated)
                    put("barcode", f.barcode ?: "")
                    put("timestamp", f.timestamp)
                })
            }

            val sessionsArray = org.json.JSONArray()
            sessions.forEach { s ->
                sessionsArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("splitName", s.splitName)
                    put("dateString", s.dateString)
                    put("durationMinutes", s.durationMinutes)
                    put("isCompleted", s.isCompleted)
                    put("totalVolumeKg", s.totalVolumeKg.toDouble())
                    put("caloriesBurned", s.caloriesBurned)
                    put("timestamp", s.timestamp)
                })
            }

            val logsArray = org.json.JSONArray()
            logs.forEach { l ->
                logsArray.put(JSONObject().apply {
                    put("id", l.id)
                    put("workoutSessionId", l.workoutSessionId)
                    put("exerciseName", l.exerciseName)
                    put("exerciseCategory", l.exerciseCategory)
                    put("setsJson", RoomConverters().fromExerciseSetList(l.sets))
                    put("timestamp", l.timestamp)
                })
            }

            val plansArray = org.json.JSONArray()
            plans.forEach { pl ->
                plansArray.put(JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("description", pl.description)
                    put("daysJson", pl.daysJson)
                    put("timestamp", pl.timestamp)
                })
            }

            val masterJson = JSONObject().apply {
                put("email", cleanEmail)
                put("accountName", account?.name ?: profile?.name ?: "Bulk Athlet")
                put("passwordHash", account?.passwordHash ?: "")
                if (profile != null) {
                    put("profile", JSONObject().apply {
                        put("name", profile.name)
                        put("email", profile.email)
                        put("profileImageUri", profile.profileImageUri ?: "")
                        put("heightCm", profile.heightCm)
                        put("weightKg", profile.currentWeightKg.toDouble())
                        put("startWeightKg", profile.startWeightKg.toDouble())
                        put("targetWeightKg", profile.targetWeightKg.toDouble())
                        put("weeklyWeightGainKg", profile.weeklyWeightGainKg.toDouble())
                        put("age", profile.age)
                        put("gender", profile.gender)
                        put("goal", profile.primaryGoal)
                        put("activityLevel", profile.activityLevel)
                        put("targetKcal", profile.dailyCalorieTarget)
                        put("proteinG", profile.proteinTargetGrams)
                        put("carbsG", profile.carbsTargetGrams)
                        put("fatG", profile.fatTargetGrams)
                        put("week", profile.currentTrainingWeek)
                        put("isDeload", profile.isDeloadActive)
                        put("split", profile.trainingSplit)
                        put("bufferPercentage", profile.bufferPercentage)
                        put("lastMondayCheckinEpoch", profile.lastMondayCheckinEpoch)
                        put("lastProgressPhotoEpoch", profile.lastProgressPhotoEpoch)
                        put("createdAtEpoch", profile.createdAtEpoch)
                        put("isOnboarded", profile.isOnboarded)
                    })
                }
                put("checkIns", checkInsArray)
                put("photos", photosArray)
                put("foods", foodsArray)
                put("sessions", sessionsArray)
                put("logs", logsArray)
                put("plans", plansArray)
                put("lastBackupEpoch", System.currentTimeMillis())
            }

            val jsonString = masterJson.toString()

            // 1. Save to SharedPreferences
            val vaultPrefs = context.getSharedPreferences("bulk_coach_cloud_vault", Context.MODE_PRIVATE)
            vaultPrefs.edit()
                .putString("master_vault_$cleanEmail", jsonString)
                .putString("latest_master_vault", jsonString)
                .apply()

            // 3. Save to internal filesDir
            runCatching {
                val vaultDir = java.io.File(context.filesDir, "cloud_vault")
                if (!vaultDir.exists()) vaultDir.mkdirs()
                java.io.File(vaultDir, "user_$cleanEmail.json").writeText(jsonString)
                java.io.File(vaultDir, "backup_master.json").writeText(jsonString)
            }

            // 4. Save to Public Storage Directories (SURVIVES APP UNINSTALL & DELETION!)
            getPublicVaultDirectories().forEach { dir ->
                runCatching {
                    if (!dir.exists()) dir.mkdirs()
                    java.io.File(dir, "user_$cleanEmail.json").writeText(jsonString)
                    java.io.File(dir, "backup_master.json").writeText(jsonString)
                }
            }

            // 5. Upload backup JSON directly to Firebase Storage bucket ('users/{cleanEmail}/backup.json')
            runCatching {
                com.example.util.FirebaseStorageManager.uploadJson(
                    context,
                    jsonString,
                    "users/$cleanEmail/backup.json"
                )
            }

            prefs.edit().putString("active_user_email", cleanEmail).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportBackupToJsonUri(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val email = dao.getUserProfile()?.email?.trim()?.lowercase()
                ?: prefs.getString("active_user_email", null)
                ?: "primary_athlete"
            
            // Build fresh json string
            saveFullCloudVaultBackup(email)
            
            val vaultPrefs = context.getSharedPreferences("bulk_coach_cloud_vault", Context.MODE_PRIVATE)
            val jsonString = vaultPrefs.getString("master_vault_$email", null)
                ?: vaultPrefs.getString("latest_master_vault", null)
                ?: return@withContext false

            context.contentResolver.openOutputStream(targetUri, "w")?.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importBackupFromJsonUri(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            if (jsonString.isNullOrBlank()) return@withContext false
            restoreMasterBackupFromJsonString(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFullCloudVaultBackup(cleanEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        val targetEmail = cleanEmail?.trim()?.lowercase()
            ?: prefs.getString("active_user_email", null)?.trim()?.lowercase()
            ?: dao.getUserProfile()?.email?.trim()?.lowercase()
        var jsonString: String? = null

        // 1. Try public storage directories (survives app deletion/uninstall!)
        if (jsonString.isNullOrBlank()) {
            getPublicVaultDirectories().forEach { dir ->
                if (jsonString.isNullOrBlank()) {
                    runCatching {
                        if (!targetEmail.isNullOrBlank()) {
                            val userFile = java.io.File(dir, "user_$targetEmail.json")
                            if (userFile.exists() && userFile.length() > 0) {
                                jsonString = userFile.readText()
                            }
                        }
                        if (cleanEmail.isNullOrBlank() && jsonString.isNullOrBlank()) {
                            val masterFile = java.io.File(dir, "backup_master.json")
                            if (masterFile.exists() && masterFile.length() > 0) {
                                jsonString = masterFile.readText()
                            }
                        }
                    }
                }
            }
        }

        // 2. Fallback to internal filesDir
        if (jsonString.isNullOrBlank()) {
            runCatching {
                val vaultDir = java.io.File(context.filesDir, "cloud_vault")
                if (!targetEmail.isNullOrBlank()) {
                    val backupFile = java.io.File(vaultDir, "user_$targetEmail.json")
                    if (backupFile.exists() && backupFile.length() > 0) jsonString = backupFile.readText()
                }
                if (cleanEmail.isNullOrBlank() && jsonString.isNullOrBlank()) {
                    val masterFile = java.io.File(vaultDir, "backup_master.json")
                    if (masterFile.exists() && masterFile.length() > 0) jsonString = masterFile.readText()
                }
            }
        }

        // 3. Fallback to SharedPreferences
        if (jsonString.isNullOrBlank()) {
            val vaultPrefs = context.getSharedPreferences("bulk_coach_cloud_vault", Context.MODE_PRIVATE)
            if (!targetEmail.isNullOrBlank()) {
                jsonString = vaultPrefs.getString("master_vault_$targetEmail", null)
            }
            if (cleanEmail.isNullOrBlank() && jsonString.isNullOrBlank()) {
                jsonString = vaultPrefs.getString("latest_master_vault", null)
            }
        }

        // 4. Fallback to Firebase Storage ('users/{targetEmail}/backup.json')
        if (jsonString.isNullOrBlank() && !targetEmail.isNullOrBlank()) {
            runCatching {
                jsonString = com.example.util.FirebaseStorageManager.downloadJson(
                    context,
                    "users/$targetEmail/backup.json"
                )
            }
        }

        var restored = false
        if (!jsonString.isNullOrBlank()) {
            val jsonObject = runCatching { JSONObject(jsonString!!) }.getOrNull()
            val backupEmail = jsonObject?.optString("email", "")?.trim()?.lowercase()
            val profileEmail = jsonObject?.optJSONObject("profile")?.optString("email", "")?.trim()?.lowercase()

            if (!cleanEmail.isNullOrBlank()) {
                if (backupEmail == targetEmail || profileEmail == targetEmail) {
                    restored = restoreMasterBackupFromJsonString(jsonString!!)
                } else {
                    android.util.Log.d("CoachRepository", "Vault backup email ($backupEmail / $profileEmail) does not match requested targetEmail ($targetEmail). Skipping restore.")
                    restored = false
                }
            } else {
                restored = restoreMasterBackupFromJsonString(jsonString!!)
            }
        }

        return@withContext restored
    }

    private fun parseExerciseSetsFromAny(obj: Any?): List<com.example.data.model.ExerciseSet> {
        val setsList = mutableListOf<com.example.data.model.ExerciseSet>()
        if (obj == null) return setsList

        if (obj is String) {
            return RoomConverters().toExerciseSetList(obj)
        }

        if (obj is List<*>) {
            obj.forEachIndexed { idx, item ->
                if (item is Map<*, *>) {
                    val setNumber = (item["setNumber"] as? Number)?.toInt()
                        ?: (item["set"] as? Number)?.toInt()
                        ?: (item["number"] as? Number)?.toInt()
                        ?: (idx + 1)
                    val weight = (item["weightKg"] as? Number)?.toFloat()
                        ?: (item["weight"] as? Number)?.toFloat()
                        ?: (item["weightKg"] as? String)?.toFloatOrNull()
                        ?: 0f
                    val reps = (item["reps"] as? Number)?.toInt()
                        ?: (item["reps"] as? String)?.toIntOrNull()
                        ?: 0
                    val isCompleted = (item["isCompleted"] as? Boolean)
                        ?: (item["completed"] as? Boolean)
                        ?: true
                    val rpe = (item["rpe"] as? Number)?.toInt() ?: 8
                    val isWarmup = (item["isWarmup"] as? Boolean) ?: false
                    val setLabel = item["setLabel"] as? String ?: ""

                    setsList.add(
                        com.example.data.model.ExerciseSet(
                            setNumber = setNumber,
                            weightKg = weight,
                            reps = reps,
                            isCompleted = isCompleted,
                            rpe = rpe,
                            isWarmup = isWarmup,
                            setLabel = setLabel
                        )
                    )
                }
            }
        }
        return setsList
    }

    suspend fun restoreMasterBackupFromJsonString(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val masterJson = JSONObject(jsonString)
            val restoredEmail = masterJson.optString("email", "primary_athlete")

            val accountName = masterJson.optString("accountName", "Bulk Athlet")
            val passwordHash = masterJson.optString("passwordHash", "")
            if (passwordHash.isNotBlank()) {
                dao.insertUserAccount(UserAccountEntity(email = restoredEmail, name = accountName, passwordHash = passwordHash))
                val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
                authPrefs.edit().putString("pwd_$restoredEmail", passwordHash).putString("name_$restoredEmail", accountName).apply()
            }

            if (masterJson.has("profile")) {
                val pJson = masterJson.getJSONObject("profile")
                val profile = UserProfileEntity(
                    id = "primary_user",
                    email = pJson.optString("email", restoredEmail),
                    name = pJson.optString("name", accountName),
                    profileImageUri = pJson.optString("profileImageUri").ifBlank { null },
                    heightCm = pJson.optInt("heightCm", 180),
                    currentWeightKg = pJson.optDouble("weightKg", 80.0).toFloat(),
                    startWeightKg = pJson.optDouble("startWeightKg", 80.0).toFloat(),
                    targetWeightKg = pJson.optDouble("targetWeightKg", 86.0).toFloat(),
                    weeklyWeightGainKg = pJson.optDouble("weeklyWeightGainKg", 0.5).toFloat(),
                    age = pJson.optInt("age", 25),
                    gender = pJson.optString("gender", "männlich"),
                    primaryGoal = pJson.optString("goal", "Masseaufbau"),
                    activityLevel = pJson.optString("activityLevel", "Moderat aktiv"),
                    dailyCalorieTarget = pJson.optInt("targetKcal", 3300),
                    proteinTargetGrams = pJson.optInt("proteinG", 170),
                    carbsTargetGrams = pJson.optInt("carbsG", 420),
                    fatTargetGrams = pJson.optInt("fatG", 85),
                    isOnboarded = pJson.optBoolean("isOnboarded", true),
                    currentTrainingWeek = pJson.optInt("week", 1),
                    isDeloadActive = pJson.optBoolean("isDeload", false),
                    trainingSplit = pJson.optString("split", "Push / Pull / Legs"),
                    bufferPercentage = pJson.optInt("bufferPercentage", 8),
                    lastMondayCheckinEpoch = pJson.optLong("lastMondayCheckinEpoch", 0L),
                    lastProgressPhotoEpoch = pJson.optLong("lastProgressPhotoEpoch", 0L),
                    createdAtEpoch = pJson.optLong("createdAtEpoch", System.currentTimeMillis())
                )
                dao.insertOrUpdateProfile(profile)
            }

            val checkInsArr = masterJson.optJSONArray("checkIns")
            if (checkInsArr != null) {
                for (i in 0 until checkInsArr.length()) {
                    val c = checkInsArr.getJSONObject(i)
                    val waist = c.optDouble("waistCircumferenceCm", -1.0)
                    dao.insertCheckIn(
                        WeightCheckInEntity(
                            id = c.optString("id", java.util.UUID.randomUUID().toString()),
                            dateString = c.optString("dateString"),
                            weightKg = c.optDouble("weightKg").toFloat(),
                            feelingScore = c.optInt("feelingScore"),
                            waistCircumferenceCm = if (waist > 0) waist.toFloat() else null,
                            note = c.optString("note"),
                            timestamp = c.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val photosArr = masterJson.optJSONArray("photos")
            if (photosArr != null) {
                for (i in 0 until photosArr.length()) {
                    val p = photosArr.getJSONObject(i)
                    val photoId = p.optString("id", java.util.UUID.randomUUID().toString())
                    var imageUri = p.optString("imageUri")
                    val base64Data = p.optString("base64Data")

                    if (base64Data.isNotBlank()) {
                        try {
                            val photosDir = java.io.File(context.filesDir, "restored_photos")
                            if (!photosDir.exists()) photosDir.mkdirs()
                            val photoFile = java.io.File(photosDir, "photo_$photoId.jpg")
                            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            photoFile.writeBytes(bytes)
                            imageUri = photoFile.toURI().toString()
                        } catch (_: Exception) {}
                    }

                    dao.insertProgressPhoto(
                        ProgressPhotoEntity(
                            id = photoId,
                            imageUri = imageUri,
                            dateString = p.optString("dateString"),
                            weightKg = p.optDouble("weightKg").toFloat(),
                            note = p.optString("note"),
                            timestamp = p.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val foodsArr = masterJson.optJSONArray("foods")
            if (foodsArr != null) {
                for (i in 0 until foodsArr.length()) {
                    val f = foodsArr.getJSONObject(i)
                    val mealTypeName = f.optString("mealType", "LUNCH")
                    val mealTypeEnum = runCatching { com.example.data.model.MealType.valueOf(mealTypeName) }.getOrDefault(com.example.data.model.MealType.LUNCH)
                    dao.insertFood(
                        FoodItemEntity(
                            id = f.optString("id", java.util.UUID.randomUUID().toString()),
                            name = f.optString("name"),
                            portionDescription = f.optString("portionDescription"),
                            rawCalories = f.optInt("rawCalories"),
                            bufferedCalories = f.optInt("bufferedCalories"),
                            proteinGrams = f.optDouble("proteinGrams").toFloat(),
                            carbsGrams = f.optDouble("carbsGrams").toFloat(),
                            fatGrams = f.optDouble("fatGrams").toFloat(),
                            mealType = mealTypeEnum,
                            dateString = f.optString("dateString"),
                            isAiEstimated = f.optBoolean("isAiEstimated"),
                            barcode = f.optString("barcode").ifBlank { null },
                            timestamp = f.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val sessionsArr = masterJson.optJSONArray("sessions")
            if (sessionsArr != null) {
                for (i in 0 until sessionsArr.length()) {
                    val s = sessionsArr.getJSONObject(i)
                    dao.insertWorkoutSession(
                        WorkoutSessionEntity(
                            id = s.optString("id"),
                            splitName = s.optString("splitName", "Workout"),
                            dateString = s.optString("dateString"),
                            durationMinutes = s.optInt("durationMinutes", 45),
                            isCompleted = s.optBoolean("isCompleted", true),
                            totalVolumeKg = s.optDouble("totalVolumeKg", 0.0).toFloat(),
                            caloriesBurned = s.optInt("caloriesBurned", 250),
                            timestamp = s.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val logsArr = masterJson.optJSONArray("logs")
            if (logsArr != null) {
                for (i in 0 until logsArr.length()) {
                    val l = logsArr.getJSONObject(i)
                    val setsList = RoomConverters().toExerciseSetList(l.optString("setsJson", "[]"))
                    dao.insertWorkoutLog(
                        WorkoutLogEntryEntity(
                            id = l.optString("id"),
                            workoutSessionId = l.optString("workoutSessionId"),
                            exerciseName = l.optString("exerciseName"),
                            exerciseCategory = l.optString("exerciseCategory", "Kraft"),
                            sets = setsList,
                            timestamp = l.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val plansArr = masterJson.optJSONArray("plans")
            if (plansArr != null) {
                for (i in 0 until plansArr.length()) {
                    val pl = plansArr.getJSONObject(i)
                    dao.insertCustomPlan(
                        CustomWorkoutPlanEntity(
                            id = pl.optString("id"),
                            name = pl.optString("name", "Custom Plan"),
                            description = pl.optString("description", "Individueller Trainingsplan"),
                            daysJson = pl.optString("daysJson", "[]"),
                            timestamp = pl.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("active_user_email", restoredEmail)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Cloud Synchronisation simulation / backup
    private fun saveCloudSnapshot(profile: UserProfile) {
        val json = JSONObject().apply {
            put("email", profile.email)
            put("name", profile.name)
            put("profileImageUri", profile.profileImageUri ?: "")
            put("heightCm", profile.heightCm)
            put("weightKg", profile.currentWeightKg.toDouble())
            put("startWeightKg", profile.startWeightKg.toDouble())
            put("targetWeightKg", profile.targetWeightKg.toDouble())
            put("weeklyWeightGainKg", profile.weeklyWeightGainKg.toDouble())
            put("age", profile.age)
            put("gender", profile.gender)
            put("goal", profile.primaryGoal)
            put("targetKcal", profile.dailyCalorieTarget)
            put("proteinG", profile.proteinTargetGrams)
            put("carbsG", profile.carbsTargetGrams)
            put("fatG", profile.fatTargetGrams)
            put("week", profile.currentTrainingWeek)
            put("isDeload", profile.isDeloadActive)
            put("split", profile.trainingSplit)
            put("isOnboarded", profile.isOnboarded)
            put("lastUpdated", System.currentTimeMillis())
        }
        prefs.edit()
            .putString("cloud_user_${profile.email.lowercase().trim()}", json.toString())
            .putString("last_synced_email", profile.email.lowercase().trim())
            .putString("active_user_email", profile.email.lowercase().trim())
            .putBoolean("is_logged_in", true)
            .apply()

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            com.example.util.FirebaseFirestoreManager.saveUserAccount(
                context = context,
                email = profile.email,
                name = profile.name,
                profile = profile
            )
            saveFullCloudVaultBackup(profile.email)
        }
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean("is_logged_in", false)
        val activeEmail = prefs.getString("active_user_email", null)
        return loggedIn && !activeEmail.isNullOrBlank()
    }

    suspend fun checkEmailAvailable(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        // Query Cloud Firestore to see if this email is registered
        val inFirestore = com.example.util.FirebaseFirestoreManager.checkUserExists(context, cleanEmail)
        val isMaster = cleanEmail == "yamenkhalouf7@gmail.com"

        if (inFirestore || isMaster) {
            return@withContext Result.failure(Exception("Es besteht bereits ein Account mit dieser E-Mail-Adresse."))
        }
        Result.success(true)
    }

    suspend fun validateLoginCredentials(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (dao.getUserAccount(cleanEmail) == null) {
            restoreFullCloudVaultBackup(cleanEmail)
        }
        val account = dao.getUserAccount(cleanEmail)
        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        val storedPass = account?.passwordHash ?: authPrefs.getString("pwd_$cleanEmail", null)

        if (storedPass == null || storedPass != password) {
            return@withContext Result.failure(Exception("E-Mail oder Passwort falsch"))
        }
        val userName = account?.name ?: authPrefs.getString("name_$cleanEmail", "Bulk Athlet") ?: "Bulk Athlet"
        Result.success(userName)
    }

    suspend fun generateAndSendVerificationCode(
        email: String,
        userName: String = "Athlet",
        isRegistration: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val code = (100000..999999).random().toString()
        val authPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        authPrefs.edit()
            .putString("otp_$cleanEmail", code)
            .putLong("otp_time_$cleanEmail", System.currentTimeMillis())
            .apply()

        // Send real email via SMTP asynchronously so UI never gets stuck
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.data.service.EmailService.sendVerificationCode(
                    recipientEmail = cleanEmail,
                    code = code,
                    isRegistration = isRegistration,
                    userName = userName
                )
            } catch (e: Exception) {
                android.util.Log.e("CoachRepository", "Email background dispatch failed: ${e.message}")
            }
        }
        code
    }

    fun generateVerificationCode(email: String): String {
        val cleanEmail = email.trim().lowercase()
        val code = (100000..999999).random().toString()
        val authPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        authPrefs.edit()
            .putString("otp_$cleanEmail", code)
            .putLong("otp_time_$cleanEmail", System.currentTimeMillis())
            .apply()
        return code
    }

    fun verifyOtpCode(email: String, inputCode: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val authPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        val storedCode = authPrefs.getString("otp_$cleanEmail", null)
        val storedTime = authPrefs.getLong("otp_time_$cleanEmail", 0L)
        val isNotExpired = (System.currentTimeMillis() - storedTime) < (15 * 60 * 1000) // 15 mins validity

        if (storedCode != null && storedCode.trim() == inputCode.trim() && isNotExpired) {
            // OTP consumed
            authPrefs.edit().remove("otp_$cleanEmail").apply()
            return true
        }
        return false
    }

    private suspend fun checkAccountExistsOnly(email: String): String? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        val account = dao.getUserAccount(cleanEmail)
        if (account != null) return@withContext account.name
        val prefPass = authPrefs.getString("pwd_$cleanEmail", null)
        if (prefPass != null) return@withContext authPrefs.getString("name_$cleanEmail", "Athlet") ?: "Athlet"
        
        // Check files or vault metadata WITHOUT restoring profile
        try {
            val vaultDir = java.io.File(context.filesDir, "cloud_vault")
            val backupFile = java.io.File(vaultDir, "user_$cleanEmail.json")
            if (backupFile.exists() && backupFile.length() > 0) {
                val json = JSONObject(backupFile.readText())
                return@withContext json.optString("accountName", "Athlet")
            }
        } catch (e: Exception) {
            // ignore
        }
        return@withContext null
    }

    suspend fun sendPasswordResetCode(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        
        val account = dao.getUserAccount(cleanEmail)
        val prefPass = authPrefs.getString("pwd_$cleanEmail", null)
        var userName = account?.name ?: authPrefs.getString("name_$cleanEmail", null)

        if (userName == null) {
            val checkedName = checkAccountExistsOnly(cleanEmail)
            if (checkedName == null) {
                return@withContext Result.failure(Exception("Kein Konto mit der E-Mail $cleanEmail gefunden."))
            }
            userName = checkedName
        }

        val code = (100000..999999).random().toString()
        val otpPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        otpPrefs.edit()
            .putString("otp_reset_$cleanEmail", code)
            .putLong("otp_reset_time_$cleanEmail", System.currentTimeMillis())
            .apply()

        // Send real email via SMTP and ensure it is dispatched (NO login, NO session mutation)
        val emailResult = com.example.data.service.EmailService.sendPasswordResetEmail(
            recipientEmail = cleanEmail,
            code = code,
            userName = userName
        )
        if (emailResult.isFailure) {
            android.util.Log.e("CoachRepository", "Password reset email dispatch failed: ${emailResult.exceptionOrNull()?.message}")
        }
        Result.success(code)
    }

    fun verifyPasswordResetCode(email: String, inputCode: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val otpPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        val storedCode = otpPrefs.getString("otp_reset_$cleanEmail", null)
        val storedTime = otpPrefs.getLong("otp_reset_time_$cleanEmail", 0L)
        val isNotExpired = (System.currentTimeMillis() - storedTime) < (15 * 60 * 1000) // 15 mins validity

        if (storedCode != null && storedCode.trim() == inputCode.trim() && isNotExpired) {
            return true
        }
        return false
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!verifyPasswordResetCode(cleanEmail, code)) {
            return@withContext Result.failure(Exception("Ungültiger oder abgelaufener Bestätigungscode."))
        }

        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        val existingAccount = dao.getUserAccount(cleanEmail)
        val accountName = existingAccount?.name ?: authPrefs.getString("name_$cleanEmail", "Athlet") ?: "Athlet"

        // Update Room account
        val updatedAccount = UserAccountEntity(
            email = cleanEmail,
            name = accountName,
            passwordHash = newPassword,
            createdAtEpoch = existingAccount?.createdAtEpoch ?: System.currentTimeMillis()
        )
        dao.insertUserAccount(updatedAccount)

        // Update SharedPreferences Auth backup
        authPrefs.edit()
            .putString("pwd_$cleanEmail", newPassword)
            .putString("name_$cleanEmail", accountName)
            .apply()

        // Remove used OTP
        val otpPrefs = context.getSharedPreferences("bulk_coach_otp_store", Context.MODE_PRIVATE)
        otpPrefs.edit()
            .remove("otp_reset_$cleanEmail")
            .remove("otp_reset_time_$cleanEmail")
            .apply()

        // Auto-save master snapshot with updated credentials
        dao.getUserProfile()?.toModel()?.let { currentProfile ->
            saveCloudSnapshot(currentProfile)
        }

        Result.success(true)
    }

    suspend fun registerUser(name: String, email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { "Bulk Athlet" }

        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        val inFirestore = com.example.util.FirebaseFirestoreManager.checkUserExists(context, cleanEmail)
        val isMaster = cleanEmail == "yamenkhalouf7@gmail.com"

        if (inFirestore || isMaster) {
            return@withContext Result.failure(Exception("Es besteht bereits ein Account mit dieser E-Mail-Adresse."))
        }

        // Fresh account registration: Clear any previous local session state
        dao.clearProfile()
        dao.clearAllFoods()
        dao.clearWorkouts()
        dao.clearCheckIns()
        dao.clearChat()
        dao.clearCustomPlans()

        // Create new account in Room and SharedPreferences backup
        val account = UserAccountEntity(
            email = cleanEmail,
            name = cleanName,
            passwordHash = password,
            createdAtEpoch = System.currentTimeMillis()
        )
        dao.insertUserAccount(account)

        authPrefs.edit()
            .putString("pwd_$cleanEmail", password)
            .putString("name_$cleanEmail", cleanName)
            .apply()

        // Create initial fresh un-onboarded profile for the newly registered user
        val initialProfile = UserProfile(
            id = "primary_user",
            email = cleanEmail,
            name = cleanName,
            profileImageUri = null,
            heightCm = 175,
            currentWeightKg = 70.0f,
            startWeightKg = 70.0f,
            targetWeightKg = 75.0f,
            weeklyWeightGainKg = 0.5f,
            age = 20,
            gender = "männlich",
            primaryGoal = "Masseaufbau",
            dailyCalorieTarget = 3000,
            proteinTargetGrams = 150,
            carbsTargetGrams = 400,
            fatTargetGrams = 75,
            isOnboarded = false,
            currentTrainingWeek = 1,
            isDeloadActive = false,
            trainingSplit = "Push / Pull / Legs"
        )
        dao.insertOrUpdateProfile(initialProfile.toEntity())

        // Save account & profile into Cloud Firestore collection "users"
        com.example.util.FirebaseFirestoreManager.saveUserAccount(
            context = context,
            email = cleanEmail,
            name = cleanName,
            passwordHash = password,
            profile = initialProfile
        )

        saveCloudSnapshot(initialProfile)
        syncUserCustomPlans(cleanEmail)
        saveFullCloudVaultBackup(cleanEmail)
        prefs.edit().putBoolean("is_logged_in", true).putString("active_user_email", cleanEmail).apply()

        Result.success(initialProfile)
    }

    suspend fun loginUser(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        // Restore full cloud vault data (photos, checkins, foods, workouts, custom plans, account) FIRST!
        restoreFullCloudVaultBackup(cleanEmail)

        val account = dao.getUserAccount(cleanEmail)
        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        val storedPass = account?.passwordHash ?: authPrefs.getString("pwd_$cleanEmail", null)

        if (storedPass == null || storedPass != password) {
            return@withContext Result.failure(Exception("E-Mail oder Passwort falsch"))
        }

        if (account == null) {
            val restoredName = authPrefs.getString("name_$cleanEmail", "Bulk Athlet") ?: "Bulk Athlet"
            dao.insertUserAccount(
                UserAccountEntity(
                    email = cleanEmail,
                    name = restoredName,
                    passwordHash = password
                )
            )
        }

        val profile = dao.getUserProfile()?.toModel() ?: run {
            val key = "cloud_user_$cleanEmail"
            val jsonStr = prefs.getString(key, null)
            if (jsonStr != null) {
                val json = JSONObject(jsonStr)
                UserProfile(
                    id = "primary_user",
                    email = json.optString("email", cleanEmail),
                    name = json.optString("name", account?.name ?: "Bulk Athlet"),
                    profileImageUri = json.optString("profileImageUri").ifBlank { null },
                    heightCm = json.optInt("heightCm", 175),
                    currentWeightKg = json.optDouble("weightKg", 70.0).toFloat(),
                    startWeightKg = json.optDouble("startWeightKg", 70.0).toFloat(),
                    targetWeightKg = json.optDouble("targetWeightKg", 75.0).toFloat(),
                    weeklyWeightGainKg = json.optDouble("weeklyWeightGainKg", 0.5).toFloat(),
                    age = json.optInt("age", 20),
                    gender = json.optString("gender", "männlich"),
                    primaryGoal = json.optString("goal", "Masseaufbau"),
                    dailyCalorieTarget = json.optInt("targetKcal", 3000),
                    proteinTargetGrams = json.optInt("proteinG", 150),
                    carbsTargetGrams = json.optInt("carbsG", 400),
                    fatTargetGrams = json.optInt("fatG", 75),
                    isOnboarded = json.optBoolean("isOnboarded", false),
                    currentTrainingWeek = json.optInt("week", 1),
                    isDeloadActive = json.optBoolean("isDeload", false),
                    trainingSplit = json.optString("split", "Push / Pull / Legs")
                )
            } else {
                UserProfile(
                    id = "primary_user",
                    email = cleanEmail,
                    name = account?.name ?: "Bulk Athlet",
                    profileImageUri = null,
                    heightCm = 175,
                    currentWeightKg = 70.0f,
                    startWeightKg = 70.0f,
                    targetWeightKg = 75.0f,
                    weeklyWeightGainKg = 0.5f,
                    age = 20,
                    gender = "männlich",
                    primaryGoal = "Masseaufbau",
                    dailyCalorieTarget = 3000,
                    proteinTargetGrams = 150,
                    carbsTargetGrams = 400,
                    fatTargetGrams = 75,
                    isOnboarded = false,
                    currentTrainingWeek = 1,
                    isDeloadActive = false,
                    trainingSplit = "Push / Pull / Legs"
                )
            }
        }

        dao.insertOrUpdateProfile(profile.toEntity())
        saveCloudSnapshot(profile)
        syncUserCustomPlans(cleanEmail)
        prefs.edit().putBoolean("is_logged_in", true).putString("active_user_email", cleanEmail).apply()

        Result.success(profile)
    }

    suspend fun loginOrRegister(email: String, name: String = ""): UserProfile = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank { "Bulk Athlet" }

        // Restore full cloud vault backup FIRST
        restoreFullCloudVaultBackup(cleanEmail)

        val account = dao.getUserAccount(cleanEmail)
        if (account == null) {
            dao.insertUserAccount(
                UserAccountEntity(
                    email = cleanEmail,
                    name = cleanName,
                    passwordHash = "Pass.123"
                )
            )
        }

        val profile = dao.getUserProfile()?.toModel() ?: run {
            val key = "cloud_user_$cleanEmail"
            val jsonStr = prefs.getString(key, null)
            if (jsonStr != null) {
                val json = JSONObject(jsonStr)
                UserProfile(
                    id = "primary_user",
                    email = json.optString("email", cleanEmail),
                    name = json.optString("name", cleanName),
                    profileImageUri = json.optString("profileImageUri").ifBlank { null },
                    heightCm = json.optInt("heightCm", 180),
                    currentWeightKg = json.optDouble("weightKg", 80.0).toFloat(),
                    startWeightKg = json.optDouble("startWeightKg", 75.0).toFloat(),
                    targetWeightKg = json.optDouble("targetWeightKg", json.optDouble("startWeightKg", 75.0) + 6.0).toFloat(),
                    weeklyWeightGainKg = json.optDouble("weeklyWeightGainKg", 0.5).toFloat(),
                    age = json.optInt("age", 25),
                    gender = json.optString("gender", "männlich"),
                    primaryGoal = json.optString("goal", "Masseaufbau"),
                    dailyCalorieTarget = json.optInt("targetKcal", 3300),
                    proteinTargetGrams = json.optInt("proteinG", 170),
                    carbsTargetGrams = json.optInt("carbsG", 420),
                    fatTargetGrams = json.optInt("fatG", 85),
                    isOnboarded = json.optBoolean("isOnboarded", true),
                    currentTrainingWeek = json.optInt("week", 1),
                    isDeloadActive = json.optBoolean("isDeload", false),
                    trainingSplit = json.optString("split", "Push / Pull / Legs")
                )
            } else {
                UserProfile(
                    id = "primary_user",
                    email = cleanEmail,
                    name = cleanName,
                    profileImageUri = null,
                    heightCm = 180,
                    currentWeightKg = 80.0f,
                    startWeightKg = 80.0f,
                    targetWeightKg = 86.0f,
                    weeklyWeightGainKg = 0.5f,
                    age = 24,
                    gender = "männlich",
                    primaryGoal = "Masseaufbau",
                    dailyCalorieTarget = 3300,
                    proteinTargetGrams = 176,
                    carbsTargetGrams = 420,
                    fatTargetGrams = 85,
                    isOnboarded = false,
                    currentTrainingWeek = 1,
                    isDeloadActive = false,
                    trainingSplit = "Push / Pull / Legs"
                )
            }
        }
        dao.insertOrUpdateProfile(profile.toEntity())
        saveCloudSnapshot(profile)
        syncUserCustomPlans(cleanEmail)
        prefs.edit().putBoolean("is_logged_in", true).putString("active_user_email", cleanEmail).apply()
        profile
    }

    suspend fun restoreCloudAccount(email: String): Boolean = withContext(Dispatchers.IO) {
        val key = "cloud_user_${email.lowercase().trim()}"
        val jsonStr = prefs.getString(key, null) ?: return@withContext false
        runCatching {
            val json = JSONObject(jsonStr)
            val profile = UserProfile(
                id = "primary_user",
                email = json.optString("email", email),
                name = json.optString("name", "Bulk Athlet"),
                profileImageUri = json.optString("profileImageUri").ifBlank { null },
                heightCm = json.optInt("heightCm", 180),
                currentWeightKg = json.optDouble("weightKg", 80.0).toFloat(),
                startWeightKg = json.optDouble("startWeightKg", 75.0).toFloat(),
                targetWeightKg = json.optDouble("targetWeightKg", json.optDouble("startWeightKg", 75.0) + 6.0).toFloat(),
                weeklyWeightGainKg = json.optDouble("weeklyWeightGainKg", 0.5).toFloat(),
                age = json.optInt("age", 25),
                gender = json.optString("gender", "männlich"),
                primaryGoal = json.optString("goal", "Masseaufbau"),
                dailyCalorieTarget = json.optInt("targetKcal", 3300),
                proteinTargetGrams = json.optInt("proteinG", 170),
                carbsTargetGrams = json.optInt("carbsG", 420),
                fatTargetGrams = json.optInt("fatG", 85),
                isOnboarded = json.optBoolean("isOnboarded", true),
                currentTrainingWeek = json.optInt("week", 1),
                isDeloadActive = json.optBoolean("isDeload", false),
                trainingSplit = json.optString("split", "Push / Pull / Legs")
            )
            dao.insertOrUpdateProfile(profile.toEntity())
            prefs.edit().putBoolean("is_logged_in", true).apply()
            true
        }.getOrDefault(false)
    }

    private fun UserProfileEntity.toModel(): UserProfile {
        return UserProfile(
            id = id,
            email = email,
            name = name,
            profileImageUri = profileImageUri,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            startWeightKg = startWeightKg,
            targetWeightKg = targetWeightKg,
            weeklyWeightGainKg = weeklyWeightGainKg,
            age = age,
            gender = gender,
            primaryGoal = primaryGoal,
            activityLevel = activityLevel,
            dailyCalorieTarget = dailyCalorieTarget,
            proteinTargetGrams = proteinTargetGrams,
            carbsTargetGrams = carbsTargetGrams,
            fatTargetGrams = fatTargetGrams,
            isOnboarded = isOnboarded,
            currentTrainingWeek = currentTrainingWeek,
            isDeloadActive = isDeloadActive,
            trainingSplit = trainingSplit,
            bufferPercentage = bufferPercentage,
            lastMondayCheckinEpoch = lastMondayCheckinEpoch,
            lastProgressPhotoEpoch = lastProgressPhotoEpoch,
            createdAtEpoch = createdAtEpoch
        )
    }

    private fun UserProfile.toEntity(): UserProfileEntity {
        return UserProfileEntity(
            id = id,
            email = email,
            name = name,
            profileImageUri = profileImageUri,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            startWeightKg = startWeightKg,
            targetWeightKg = targetWeightKg,
            weeklyWeightGainKg = weeklyWeightGainKg,
            age = age,
            gender = gender,
            primaryGoal = primaryGoal,
            activityLevel = activityLevel,
            dailyCalorieTarget = dailyCalorieTarget,
            proteinTargetGrams = proteinTargetGrams,
            carbsTargetGrams = carbsTargetGrams,
            fatTargetGrams = fatTargetGrams,
            isOnboarded = isOnboarded,
            currentTrainingWeek = currentTrainingWeek,
            isDeloadActive = isDeloadActive,
            trainingSplit = trainingSplit,
            bufferPercentage = bufferPercentage,
            lastMondayCheckinEpoch = lastMondayCheckinEpoch,
            lastProgressPhotoEpoch = lastProgressPhotoEpoch,
            createdAtEpoch = createdAtEpoch
        )
    }

    private fun WeightCheckInEntity.toModel(): WeightCheckIn {
        return WeightCheckIn(
            id = id,
            dateString = dateString,
            weightKg = weightKg,
            feelingScore = feelingScore,
            waistCircumferenceCm = waistCircumferenceCm,
            note = note,
            timestamp = timestamp
        )
    }

    private fun ProgressPhotoEntity.toModel(): ProgressPhoto {
        return ProgressPhoto(
            id = id,
            imageUri = imageUri,
            dateString = dateString,
            weightKg = weightKg,
            note = note,
            timestamp = timestamp
        )
    }

    private fun ProgressPhoto.toEntity(): ProgressPhotoEntity {
        return ProgressPhotoEntity(
            id = id,
            imageUri = imageUri,
            dateString = dateString,
            weightKg = weightKg,
            note = note,
            timestamp = timestamp
        )
    }

    fun getSavedPerformanceReport(): PerformanceReport? {
        val json = prefs.getString("saved_performance_report_json", null) ?: return null
        return runCatching {
            val obj = JSONObject(json)
            val actionPlanArray = obj.optJSONArray("actionPlan")
            val planList = mutableListOf<String>()
            if (actionPlanArray != null) {
                for (i in 0 until actionPlanArray.length()) {
                    planList.add(actionPlanArray.getString(i))
                }
            }
            val photoArray = obj.optJSONArray("photoUrisUsed")
            val photoList = mutableListOf<String>()
            if (photoArray != null) {
                for (i in 0 until photoArray.length()) {
                    photoList.add(photoArray.getString(i))
                }
            }
            PerformanceReport(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                generatedAtEpoch = obj.optLong("generatedAtEpoch", System.currentTimeMillis()),
                workoutAnalysis = obj.optString("workoutAnalysis"),
                visualAndWeightAnalysis = obj.optString("visualAndWeightAnalysis"),
                muscleAndVTaperAnalysis = obj.optString("muscleAndVTaperAnalysis"),
                nutritionAnalysis = obj.optString("nutritionAnalysis"),
                actionPlan = planList,
                photoUrisUsed = photoList,
                hasEnoughPhotos = obj.optBoolean("hasEnoughPhotos", true)
            )
        }.getOrNull()
    }

    fun savePerformanceReport(report: PerformanceReport) {
        val obj = JSONObject().apply {
            put("id", report.id)
            put("generatedAtEpoch", report.generatedAtEpoch)
            put("workoutAnalysis", report.workoutAnalysis)
            put("visualAndWeightAnalysis", report.visualAndWeightAnalysis)
            put("muscleAndVTaperAnalysis", report.muscleAndVTaperAnalysis)
            put("nutritionAnalysis", report.nutritionAnalysis)
            put("actionPlan", JSONArray(report.actionPlan))
            put("photoUrisUsed", JSONArray(report.photoUrisUsed))
            put("hasEnoughPhotos", report.hasEnoughPhotos)
        }
        prefs.edit().putString("saved_performance_report_json", obj.toString()).apply()
    }

    suspend fun generatePerformanceReport(): PerformanceReport = withContext(Dispatchers.IO) {
        val profile = getOrRestoreProfile() ?: UserProfile()

        val workoutRepo = WorkoutRepository(context)
        val nutritionRepo = NutritionRepository(context)
        val geminiRepo = GeminiRepository(context)

        val completedSessions = workoutRepo.getWorkoutSessions().filter { it.isCompleted }.take(2)
        val sessionsWithLogs = completedSessions.map { session ->
            val logs = workoutRepo.getLogsForSession(session.id)
            Pair(session, logs)
        }

        val checkIns = dao.getAllCheckIns().map { it.toModel() }.take(2)
        val photos = dao.getAllProgressPhotos().map { it.toModel() }
        val recentFoods = nutritionRepo.getFoodsForDate(java.time.LocalDate.now().toString())

        val report = geminiRepo.generatePerformanceReport(
            profile = profile,
            lastTwoWorkouts = sessionsWithLogs,
            lastTwoCheckIns = checkIns,
            photos = photos,
            recentFoodItems = recentFoods
        )

        savePerformanceReport(report)
        report
    }
}


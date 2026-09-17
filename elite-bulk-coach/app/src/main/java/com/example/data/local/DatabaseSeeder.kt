package com.example.data.local

import android.content.Context
import com.example.data.model.Exercise
import com.example.data.model.ExerciseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object DatabaseSeeder {

    fun getYamenPushExercises(): List<Exercise> = listOf(
        Exercise(
            id = "seed_ex_push_1",
            name = "Bankdrücken (Langhantel)",
            category = "Brust",
            defaultSets = 3,
            targetReps = "4-5",
            lastWeightKg = 15.0f,
            lastReps = 5,
            suggestedWeightKg = 17.5f,
            notes = "Satz W: 10kg x 4 | Satz 1: 15kg x 5 | Satz 2: 15kg x 5"
        ),
        Exercise(
            id = "seed_ex_push_2",
            name = "Schräge Brustpresse (Maschine)",
            category = "Brust",
            defaultSets = 3,
            targetReps = "9",
            lastWeightKg = 17.5f,
            lastReps = 9,
            suggestedWeightKg = 20.0f,
            notes = "Satz 1-3: 17.5kg x 9"
        ),
        Exercise(
            id = "seed_ex_push_3",
            name = "Schulterpresse sitzend (Maschine)",
            category = "Schultern",
            defaultSets = 3,
            targetReps = "5-7",
            lastWeightKg = 25.0f,
            lastReps = 7,
            suggestedWeightKg = 27.5f,
            notes = "Satz W: 20kg x 5 | Satz 1-2: 25kg x 7"
        ),
        Exercise(
            id = "seed_ex_push_4",
            name = "Fliegende (Maschine)",
            category = "Brust",
            defaultSets = 3,
            targetReps = "7",
            lastWeightKg = 61.0f,
            lastReps = 7,
            suggestedWeightKg = 63.5f,
            notes = "Satz 1-3: 61kg x 7"
        ),
        Exercise(
            id = "seed_ex_push_5",
            name = "Einarmiges Seitheben (Kabelzug)",
            category = "Schultern",
            defaultSets = 3,
            targetReps = "5-7",
            lastWeightKg = 5.0f,
            lastReps = 7,
            suggestedWeightKg = 7.5f,
            notes = "Satz W: 2.5kg x 5 | Satz 1-2: 5kg x 7"
        ),
        Exercise(
            id = "seed_ex_push_6",
            name = "Trizepsdrücken mit dem Seil",
            category = "Arme",
            defaultSets = 3,
            targetReps = "8",
            lastWeightKg = 17.5f,
            lastReps = 8,
            suggestedWeightKg = 20.0f,
            notes = "Satz 1-3: 17.5kg x 8"
        ),
        Exercise(
            id = "seed_ex_push_7",
            name = "Stirndrücken (Langhantel)",
            category = "Arme",
            defaultSets = 3,
            targetReps = "8",
            lastWeightKg = 2.5f,
            lastReps = 8,
            suggestedWeightKg = 5.0f,
            notes = "Satz 1-3: 2.5kg x 8"
        )
    )

    fun getYamenPullExercises(): List<Exercise> = listOf(
        Exercise(
            id = "seed_ex_pull_1",
            name = "Klimmzug (Gewichtet)",
            category = "Rücken",
            defaultSets = 3,
            targetReps = "5",
            lastWeightKg = 20.0f,
            lastReps = 5,
            suggestedWeightKg = 22.5f,
            notes = "Satz 1-3: 20kg x 5"
        ),
        Exercise(
            id = "seed_ex_pull_2",
            name = "Iso-laterales Rudern",
            category = "Rücken",
            defaultSets = 3,
            targetReps = "12",
            lastWeightKg = 40.0f,
            lastReps = 12,
            suggestedWeightKg = 42.5f,
            notes = "Satz 1-3: 40kg x 12"
        ),
        Exercise(
            id = "seed_ex_pull_3",
            name = "Latzug (Maschine)",
            category = "Rücken",
            defaultSets = 2,
            targetReps = "6",
            lastWeightKg = 40.0f,
            lastReps = 6,
            suggestedWeightKg = 42.5f,
            notes = "Satz 1-2: 40kg x 6"
        ),
        Exercise(
            id = "seed_ex_pull_4",
            name = "Face Pull",
            category = "Schultern",
            defaultSets = 3,
            targetReps = "7",
            lastWeightKg = 35.0f,
            lastReps = 7,
            suggestedWeightKg = 37.5f,
            notes = "Satz 1-3: 35kg x 7"
        ),
        Exercise(
            id = "seed_ex_pull_5",
            name = "Preacher Curl (Maschine)",
            category = "Arme",
            defaultSets = 3,
            targetReps = "5",
            lastWeightKg = 30.0f,
            lastReps = 5,
            suggestedWeightKg = 32.5f,
            notes = "Satz 1-3: 30kg x 5"
        ),
        Exercise(
            id = "seed_ex_pull_6",
            name = "Bizepscurl (Kurzhantel)",
            category = "Arme",
            defaultSets = 3,
            targetReps = "9",
            lastWeightKg = 12.5f,
            lastReps = 9,
            suggestedWeightKg = 15.0f,
            notes = "Satz 1-3: 12.5kg x 9"
        ),
        Exercise(
            id = "seed_ex_pull_7",
            name = "Hammer Curl (Kurzhantel)",
            category = "Arme",
            defaultSets = 3,
            targetReps = "9",
            lastWeightKg = 12.5f,
            lastReps = 9,
            suggestedWeightKg = 15.0f,
            notes = "Satz 1-3: 12.5kg x 9"
        )
    )

    fun getYamenLegExercises(): List<Exercise> = listOf(
        Exercise(
            id = "seed_ex_leg_1",
            name = "Hackenschmidt Squat (Maschine)",
            category = "Beine",
            defaultSets = 3,
            targetReps = "8",
            lastWeightKg = 15.0f,
            lastReps = 8,
            suggestedWeightKg = 20.0f,
            notes = "Satz 1-3: 15kg x 8"
        ),
        Exercise(
            id = "seed_ex_leg_2",
            name = "Beinpresse",
            category = "Beine",
            defaultSets = 3,
            targetReps = "6",
            lastWeightKg = 40.0f,
            lastReps = 6,
            suggestedWeightKg = 45.0f,
            notes = "Satz W: 20kg x 6 | Satz 1-2: 40kg x 6"
        ),
        Exercise(
            id = "seed_ex_leg_3",
            name = "Beinstrecken",
            category = "Beine",
            defaultSets = 3,
            targetReps = "5-7",
            lastWeightKg = 45.0f,
            lastReps = 7,
            suggestedWeightKg = 47.5f,
            notes = "Satz W: 25kg x 5 | Satz 1-2: 45kg x 7"
        ),
        Exercise(
            id = "seed_ex_leg_4",
            name = "Beinbeugen sitzend",
            category = "Beine",
            defaultSets = 3,
            targetReps = "5-9",
            lastWeightKg = 37.5f,
            lastReps = 9,
            suggestedWeightKg = 40.0f,
            notes = "Satz W: 25kg x 5 | Satz 1-2: 37.5kg x 9"
        ),
        Exercise(
            id = "seed_ex_leg_5",
            name = "Hyperextension",
            category = "Beine",
            defaultSets = 2,
            targetReps = "15",
            lastWeightKg = 0.0f,
            lastReps = 15,
            suggestedWeightKg = 5.0f,
            notes = "Satz 1-2: 0kg x 15"
        ),
        Exercise(
            id = "seed_ex_leg_6",
            name = "Wadenheben stehend (Maschine)",
            category = "Waden",
            defaultSets = 3,
            targetReps = "5-9",
            lastWeightKg = 105.0f,
            lastReps = 9,
            suggestedWeightKg = 110.0f,
            notes = "Satz W: 65kg x 5 | Satz 1-2: 105kg x 9"
        ),
        Exercise(
            id = "seed_ex_leg_7",
            name = "Crunch (Maschine)",
            category = "Bauch",
            defaultSets = 3,
            targetReps = "9",
            lastWeightKg = 40.0f,
            lastReps = 9,
            suggestedWeightKg = 42.5f,
            notes = "Satz 1: 35kg x 9 | Satz 2-3: 40kg x 9"
        )
    )

    fun buildPlanEntity(id: String, name: String, description: String, exercises: List<Exercise>): CustomWorkoutPlanEntity {
        val daysArray = JSONArray()
        val dayObj = JSONObject().apply {
            put("id", "day_${id}")
            put("name", name)
            val exArray = JSONArray()
            exercises.forEach { ex ->
                val exObj = JSONObject().apply {
                    put("id", ex.id)
                    put("name", ex.name)
                    put("category", ex.category)
                    put("defaultSets", ex.defaultSets)
                    put("targetReps", ex.targetReps)
                    put("lastWeightKg", ex.lastWeightKg.toDouble())
                    put("lastReps", ex.lastReps)
                    put("suggestedWeightKg", ex.suggestedWeightKg.toDouble())
                    put("notes", ex.notes)
                }
                exArray.put(exObj)
            }
            put("exercises", exArray)
        }
        daysArray.put(dayObj)

        return CustomWorkoutPlanEntity(
            id = id,
            name = name,
            description = description,
            daysJson = daysArray.toString(),
            timestamp = System.currentTimeMillis()
        )
    }

    fun getYamenDefaultPlans(): List<CustomWorkoutPlanEntity> {
        val pushPlan = buildPlanEntity("plan_push", "Routine 1: Push", "Brust, Schultern & Trizeps Hypertrophie", getYamenPushExercises())
        val pullPlan = buildPlanEntity("plan_pull", "Routine 2: Pull", "Rücken, Hintere Schulter & Bizeps", getYamenPullExercises())
        val legPlan = buildPlanEntity("plan_legs", "Routine 3: Leg", "Quads, Hamstrings, Waden & Core", getYamenLegExercises())
        return listOf(pushPlan, pullPlan, legPlan)
    }

    fun serializePlans(plans: List<CustomWorkoutPlanEntity>): String {
        val array = JSONArray()
        plans.forEach { plan ->
            val obj = JSONObject().apply {
                put("id", plan.id)
                put("name", plan.name)
                put("description", plan.description)
                put("daysJson", plan.daysJson)
                put("timestamp", plan.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializePlans(jsonStr: String): List<CustomWorkoutPlanEntity> {
        val list = mutableListOf<CustomWorkoutPlanEntity>()
        runCatching {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomWorkoutPlanEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", "Individueller Trainingsplan"),
                        daysJson = obj.getString("daysJson"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }
        return list
    }

    suspend fun seedDatabaseIfEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.coachDao()

        // 1. Seed Yamen account in user_accounts table if not present
        val yamenEmail = "yamenkhalouf7@gmail.com"
        val existingAccount = dao.getUserAccount(yamenEmail)
        if (existingAccount == null) {
            dao.insertUserAccount(
                UserAccountEntity(
                    email = yamenEmail,
                    name = "Yamen",
                    passwordHash = "Yamen.10/",
                    createdAtEpoch = System.currentTimeMillis()
                )
            )
        }

        // Store credentials in auth prefs as well
        val authPrefs = context.getSharedPreferences("bulk_coach_auth_accounts", Context.MODE_PRIVATE)
        authPrefs.edit()
            .putString("pwd_$yamenEmail", "Yamen.10/")
            .putString("name_$yamenEmail", "Yamen")
            .apply()

        // Store Yamen snapshot for when Yamen logs in
        val bulkPrefs = context.getSharedPreferences("bulk_coach_prefs", Context.MODE_PRIVATE)
        val yamenSnapshotJson = JSONObject().apply {
            put("email", yamenEmail)
            put("name", "Yamen")
            put("heightCm", 175)
            put("weightKg", 60.0)
            put("startWeightKg", 60.0)
            put("targetWeightKg", 70.0)
            put("weeklyWeightGainKg", 0.75)
            put("age", 16)
            put("gender", "männlich")
            put("goal", "Masseaufbau")
            put("targetKcal", 3334)
            put("proteinG", 132)
            put("carbsG", 566)
            put("fatG", 60)
            put("week", 1)
            put("isDeload", false)
            put("split", "Push / Pull / Legs")
            put("isOnboarded", true)
            put("lastUpdated", System.currentTimeMillis())
        }
        bulkPrefs.edit()
            .putString("cloud_user_$yamenEmail", yamenSnapshotJson.toString())
            .apply()

        // Store Yamen's default Push/Pull/Legs plans in cloud backup if not already saved
        val existingYamenPlans = bulkPrefs.getString("cloud_plans_$yamenEmail", null)
        if (existingYamenPlans == null) {
            val yamenPlans = getYamenDefaultPlans()
            bulkPrefs.edit()
                .putString("cloud_plans_$yamenEmail", serializePlans(yamenPlans))
                .apply()
        }

        // 2. Pre-seed all standard exercises into CustomExerciseEntity so they appear in Exercise Library (Übungsspeicher)
        val libraryExercises = dao.getAllCustomExercises()
        if (libraryExercises.isEmpty()) {
            val allExercises = getYamenPushExercises() + getYamenPullExercises() + getYamenLegExercises()
            allExercises.forEach { ex ->
                dao.insertCustomExercise(
                    CustomExerciseEntity(
                        id = ex.id,
                        name = ex.name,
                        category = ex.category,
                        defaultSets = ex.defaultSets,
                        targetReps = ex.targetReps,
                        notes = ex.notes,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 3. Check active user
        val activeEmail = bulkPrefs.getString("active_user_email", null)?.lowercase()?.trim()
        val existingPlans = dao.getAllCustomPlans()

        if (activeEmail == yamenEmail) {
            // Active user is Yamen -> load Yamen's latest plans
            if (existingPlans.isEmpty()) {
                val yamenPlansJson = bulkPrefs.getString("cloud_plans_$yamenEmail", null)
                val plansToLoad = if (yamenPlansJson != null) deserializePlans(yamenPlansJson) else getYamenDefaultPlans()
                plansToLoad.forEach { dao.insertCustomPlan(it) }
            }

            // Seed initial logs for Yamen so progress comparison works immediately
            val existingSessions = dao.getAllWorkoutSessions()
            if (existingSessions.isEmpty()) {
                val initialSessions = listOf(
                    Triple("Routine 1: Push", getYamenPushExercises(), "2026-08-11"),
                    Triple("Routine 2: Pull", getYamenPullExercises(), "2026-08-12"),
                    Triple("Routine 3: Leg", getYamenLegExercises(), "2026-08-13")
                )

                initialSessions.forEach { (routineName, exercises, dateStr) ->
                    val sessionId = "seed_session_${routineName.replace(" ", "_").lowercase()}"
                    var totalVolume = 0f

                    val logEntities = exercises.map { ex ->
                        val sets = mutableListOf<ExerciseSet>()
                        when (ex.name) {
                            "Bankdrücken (Langhantel)" -> {
                                sets.add(ExerciseSet(1, 10f, 4, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 15f, 5, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 15f, 5, isCompleted = true, setLabel = "2"))
                            }
                            "Schräge Brustpresse (Maschine)" -> {
                                sets.add(ExerciseSet(1, 17.5f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 17.5f, 9, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 17.5f, 9, isCompleted = true, setLabel = "3"))
                            }
                            "Schulterpresse sitzend (Maschine)" -> {
                                sets.add(ExerciseSet(1, 20f, 5, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 25f, 7, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 25f, 7, isCompleted = true, setLabel = "2"))
                            }
                            "Fliegende (Maschine)" -> {
                                sets.add(ExerciseSet(1, 61f, 7, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 61f, 7, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 61f, 7, isCompleted = true, setLabel = "3"))
                            }
                            "Einarmiges Seitheben (Kabelzug)" -> {
                                sets.add(ExerciseSet(1, 2.5f, 5, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 5f, 7, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 5f, 7, isCompleted = true, setLabel = "2"))
                            }
                            "Trizepsdrücken mit dem Seil" -> {
                                sets.add(ExerciseSet(1, 17.5f, 8, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 17.5f, 8, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 17.5f, 8, isCompleted = true, setLabel = "3"))
                            }
                            "Stirndrücken (Langhantel)" -> {
                                sets.add(ExerciseSet(1, 2.5f, 8, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 2.5f, 8, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 2.5f, 8, isCompleted = true, setLabel = "3"))
                            }
                            "Klimmzug (Gewichtet)" -> {
                                sets.add(ExerciseSet(1, 20f, 5, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 20f, 5, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 20f, 5, isCompleted = true, setLabel = "3"))
                            }
                            "Iso-laterales Rudern" -> {
                                sets.add(ExerciseSet(1, 40f, 12, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 40f, 12, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 40f, 12, isCompleted = true, setLabel = "3"))
                            }
                            "Latzug (Maschine)" -> {
                                sets.add(ExerciseSet(1, 40f, 6, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 40f, 6, isCompleted = true, setLabel = "2"))
                            }
                            "Face Pull" -> {
                                sets.add(ExerciseSet(1, 35f, 7, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 35f, 7, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 35f, 7, isCompleted = true, setLabel = "3"))
                            }
                            "Preacher Curl (Maschine)" -> {
                                sets.add(ExerciseSet(1, 30f, 5, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 30f, 5, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 30f, 5, isCompleted = true, setLabel = "3"))
                            }
                            "Bizepscurl (Kurzhantel)" -> {
                                sets.add(ExerciseSet(1, 12.5f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 12.5f, 9, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 12.5f, 9, isCompleted = true, setLabel = "3"))
                            }
                            "Hammer Curl (Kurzhantel)" -> {
                                sets.add(ExerciseSet(1, 12.5f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 12.5f, 9, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 12.5f, 9, isCompleted = true, setLabel = "3"))
                            }
                            "Hackenschmidt Squat (Maschine)" -> {
                                sets.add(ExerciseSet(1, 15f, 8, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 15f, 8, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 15f, 8, isCompleted = true, setLabel = "3"))
                            }
                            "Beinpresse" -> {
                                sets.add(ExerciseSet(1, 20f, 6, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 40f, 6, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 40f, 6, isCompleted = true, setLabel = "2"))
                            }
                            "Beinstrecken" -> {
                                sets.add(ExerciseSet(1, 25f, 5, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 45f, 7, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 45f, 7, isCompleted = true, setLabel = "2"))
                            }
                            "Beinbeugen sitzend" -> {
                                sets.add(ExerciseSet(1, 25f, 5, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 37.5f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 37.5f, 9, isCompleted = true, setLabel = "2"))
                            }
                            "Hyperextension" -> {
                                sets.add(ExerciseSet(1, 0f, 15, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 0f, 15, isCompleted = true, setLabel = "2"))
                            }
                            "Wadenheben stehend (Maschine)" -> {
                                sets.add(ExerciseSet(1, 65f, 5, isCompleted = true, isWarmup = true, setLabel = "W"))
                                sets.add(ExerciseSet(2, 105f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(3, 105f, 9, isCompleted = true, setLabel = "2"))
                            }
                            "Crunch (Maschine)" -> {
                                sets.add(ExerciseSet(1, 35f, 9, isCompleted = true, setLabel = "1"))
                                sets.add(ExerciseSet(2, 40f, 9, isCompleted = true, setLabel = "2"))
                                sets.add(ExerciseSet(3, 40f, 9, isCompleted = true, setLabel = "3"))
                            }
                            else -> {
                                sets.add(ExerciseSet(1, ex.lastWeightKg, ex.lastReps, isCompleted = true, setLabel = "1"))
                            }
                        }

                        sets.forEach { s ->
                            if (s.isCompleted) {
                                totalVolume += (s.weightKg * s.reps)
                            }
                        }

                        WorkoutLogEntryEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            workoutSessionId = sessionId,
                            exerciseName = ex.name,
                            exerciseCategory = ex.category,
                            sets = sets,
                            timestamp = System.currentTimeMillis() - 86400000L
                        )
                    }

                    dao.insertWorkoutSession(
                        WorkoutSessionEntity(
                            id = sessionId,
                            splitName = routineName,
                            dateString = dateStr,
                            durationMinutes = 55,
                            isCompleted = true,
                            totalVolumeKg = totalVolume,
                            caloriesBurned = 340,
                            timestamp = System.currentTimeMillis() - 86400000L
                        )
                    )

                    logEntities.forEach { log ->
                        dao.insertWorkoutLog(log)
                    }
                }
            }
        } else if (!activeEmail.isNullOrBlank()) {
            // Another user is logged in -> load their plans if available, otherwise keep empty
            if (existingPlans.isEmpty()) {
                val userPlansJson = bulkPrefs.getString("cloud_plans_$activeEmail", null)
                if (userPlansJson != null) {
                    val plansToLoad = deserializePlans(userPlansJson)
                    plansToLoad.forEach { dao.insertCustomPlan(it) }
                }
            }
        } else {
            // No user is logged in yet: DO NOT force Yamen's routines on the general app
            // Keep custom_workout_plans clean
            if (existingPlans.isNotEmpty()) {
                dao.clearCustomPlans()
            }
        }
    }
}

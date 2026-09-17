package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CustomExerciseEntity
import com.example.data.local.CustomWorkoutPlanEntity
import com.example.data.local.WorkoutLogEntryEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.model.CustomWorkoutDay
import com.example.data.model.CustomWorkoutPlan
import com.example.data.model.Exercise
import com.example.data.model.ExerciseSet
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutCalorieResult
import com.example.data.model.WorkoutLogEntry
import com.example.data.model.WorkoutSession
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WorkoutRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.coachDao()

    val workoutSessionsFlow: Flow<List<WorkoutSession>> = dao.getAllWorkoutSessionsFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getWorkoutSessions(): List<WorkoutSession> = withContext(Dispatchers.IO) {
        dao.getAllWorkoutSessions().map { it.toModel() }
    }

    // Built-in Base Exercise Database (Übungsspeicher)
    val defaultExercises: List<Exercise> = listOf(
        // ==================== RÜCKEN ====================
        Exercise("def_back_1", "Enges Rudern Maschine", "Rücken", 4, "8-10", 65f, 10, 67.5f, "Brustpolster nutzen, Fokus auf Lat & Rhomboiden"),
        Exercise("def_back_2", "Breites Latzug zur Brust", "Rücken", 4, "8-12", 60f, 10, 62.5f, "Schulterblätter vor Beginn nach unten ziehen"),
        Exercise("def_back_3", "Latziehen enger Parallelgriff", "Rücken", 3, "10-12", 55f, 10, 57.5f, "Ellenbogen eng am Körper nach unten führen"),
        Exercise("def_back_4", "Rudern am Kabelzug (eng)", "Rücken", 4, "8-10", 60f, 10, 62.5f, "V-Griff, aufrechter Oberkörper, Lat kontrahiert"),
        Exercise("def_back_5", "Rudern am Kabelzug (breit)", "Rücken", 3, "10-12", 50f, 10, 52.5f, "Breite Stange, Fokus auf oberen Rücken & Trapez"),
        Exercise("def_back_6", "Kurzhantel-Rudern", "Rücken", 3, "8-10", 26f, 10, 28f, "Auf Bank abgestützt, Hantel zur Hüfte ziehen"),
        Exercise("def_back_7", "T-Bar Rudern", "Rücken", 4, "6-8", 55f, 8, 57.5f, "Stabile Hüfte, maximale Dichte für Rückenmitte"),
        Exercise("def_back_8", "Langhantelrudern vorgebeugt", "Rücken", 4, "8-10", 70f, 8, 72.5f, "Oberkörper ca. 45° geneigt, zur Hüfte ziehen"),
        Exercise("def_back_9", "Klimmzüge", "Rücken", 4, "6-10", 0f, 8, 2.5f, "Kinn über die Stange, kontrolliert ablassen"),
        Exercise("def_back_10", "Klimmzüge mit Zusatzgewicht", "Rücken", 4, "5-8", 10f, 6, 12.5f, "Mit Dip-Gürtel für maximale Überlastung"),
        Exercise("def_back_11", "Kreuzheben (Deadlift)", "Rücken", 4, "5", 110f, 5, 115f, "Gerader Rücken, Lats anspannen, Beine & Rücken nutzen"),
        Exercise("def_back_12", "Hyperextensions", "Rücken", 3, "12-15", 10f, 15, 15f, "Stärkung des unteren Rückens & Gluteus"),
        Exercise("def_back_13", "Überzüge am Kabelzug (Straight Arm)", "Rücken", 3, "12-15", 25f, 12, 27.5f, "Gestreckte Arme, Isolation des Lats"),
        Exercise("def_back_14", "Brustgestütztes Rudern Maschine", "Rücken", 4, "8-10", 50f, 10, 52.5f, "Kein Schwung möglich, pure Rückenaktivierung"),
        Exercise("def_back_15", "Rack Pulls", "Rücken", 3, "6-8", 130f, 6, 135f, "Teilbewegung Kreuzheben ab Kniehöhe für Trapez & oberen Rücken"),

        // ==================== BRUST ====================
        Exercise("def_chest_1", "Bankdrücken (Flach mit Langhantel)", "Brust", 4, "6-8", 75f, 8, 77.5f, "Schulterblätter zusammen, kontrolliert zur Brust"),
        Exercise("def_chest_2", "Bankdrücken (Flach mit Kurzhantel)", "Brust", 4, "8-10", 28f, 8, 30f, "Größere ROM und Dehnung in der unteren Position"),
        Exercise("def_chest_3", "Schrägbankdrücken (Langhantel)", "Brust", 4, "6-8", 65f, 8, 67.5f, "30° Bankwinkel für oberen Brustmuskel"),
        Exercise("def_chest_4", "Schrägbankdrücken (Kurzhantel)", "Brust", 4, "8-10", 26f, 10, 28f, "Tiefe Dehnung und Peak Contraction oben"),
        Exercise("def_chest_5", "Negativbankdrücken (Langhantel)", "Brust", 3, "8-10", 70f, 8, 72.5f, "Fokus auf unteren Brustanteil"),
        Exercise("def_chest_6", "Negativbankdrücken (Kurzhantel)", "Brust", 3, "8-10", 24f, 10, 26f, "Schonend für die vordere Schulter"),
        Exercise("def_chest_7", "Brustpresse Maschine", "Brust", 4, "8-12", 70f, 10, 72.5f, "Geführte Bewegung bis zum Muskelversagen"),
        Exercise("def_chest_8", "Schrägbank Brustpresse Maschine", "Brust", 3, "10-12", 55f, 10, 57.5f, "Obere Brust im sicheren Maschinensetup"),
        Exercise("def_chest_9", "Cable Flys (hoch nach tief)", "Brust", 3, "12-15", 15f, 12, 17.5f, "Kabelzug von oben nach unten zur Hüfte führen"),
        Exercise("def_chest_10", "Cable Flys (tief nach hoch)", "Brust", 3, "12-15", 12.5f, 12, 15f, "Kabelzug von unten nach oben für obere Brust"),
        Exercise("def_chest_11", "Butterfly Maschine (Pec Deck)", "Brust", 3, "12-15", 45f, 12, 47.5f, "1s Peak Contraction im Umkehrpunkt"),
        Exercise("def_chest_12", "Dips (Brustfokus)", "Brust", 3, "8-10", 0f, 10, 5f, "Oberkörper nach vorne lehnen, Ellenbogen leicht ausstellen"),
        Exercise("def_chest_13", "Liegestütze mit Gewicht", "Brust", 3, "12-15", 10f, 15, 12.5f, "Gewichtsscheibe auf dem oberen Rücken"),

        // ==================== SCHULTERN ====================
        Exercise("def_shoulder_1", "Schulterdrücken Maschine", "Schultern", 4, "8-10", 50f, 10, 52.5f, "Geführter Bewegungsablauf für maximale Schulterlast"),
        Exercise("def_shoulder_2", "Kurzhantel-Schulterdrücken", "Schultern", 4, "8-10", 22f, 8, 24f, "Sitzend mit aufrechter Rückenlehne"),
        Exercise("def_shoulder_3", "Military Press (Langhantel OHP)", "Schultern", 4, "6-8", 45f, 8, 47.5f, "Im Stand, Core & Gluteus fest anspannen"),
        Exercise("def_shoulder_4", "Seitheben am Kabelzug", "Schultern", 4, "12-15", 7.5f, 15, 10f, "Konstante Spannung auf dem lateralen Deltamuskel"),
        Exercise("def_shoulder_5", "Seitheben mit Kurzhanteln", "Schultern", 4, "12-15", 12f, 12, 14f, "Leichte Vorbeuge, Daumen leicht nach unten"),
        Exercise("def_shoulder_6", "Seitheben Maschine", "Schultern", 3, "12-15", 35f, 12, 37.5f, "Kein Unterarm-Einsatz, pure seitliche Schulter"),
        Exercise("def_shoulder_7", "Reverse Flys (Maschine)", "Schultern", 4, "12-15", 40f, 12, 42.5f, "Rückwärts sitzend für hintere Schulter & Nacken"),
        Exercise("def_shoulder_8", "Reverse Flys (Kabelzug)", "Schultern", 3, "12-15", 10f, 12, 12.5f, "Überkreuzter Kabelzug auf Augenhöhe"),
        Exercise("def_shoulder_9", "Face Pulls am Kabelzug", "Schultern", 4, "15-20", 25f, 15, 27.5f, "Seilzug zu den Schläfen, Außenrotation fokussieren"),
        Exercise("def_shoulder_10", "Frontheben mit Kurzhanteln", "Schultern", 3, "12-15", 10f, 12, 12f, "Vordere Schulter bis auf Schulterhöhe anheben"),
        Exercise("def_shoulder_11", "Frontheben am Kabelzug", "Schultern", 3, "12-15", 12.5f, 12, 15f, "Stange oder Seil zwischen den Beinen führen"),
        Exercise("def_shoulder_12", "Nackenziehen / Shrugs (Kurzhantel)", "Schultern", 4, "10-12", 30f, 12, 32f, "Schultern gerade nach oben zu den Ohren ziehen"),
        Exercise("def_shoulder_13", "Nackenziehen / Shrugs (Langhantel)", "Schultern", 4, "8-10", 80f, 10, 85f, "Schwere Last für den oberen Trapezius"),

        // ==================== BEINE & PO ====================
        Exercise("def_leg_1", "Kniebeugen (Langhantel Back Squat)", "Beine", 4, "6-8", 90f, 8, 95f, "Hüfte unter Kniehöhe, stabiler Stand"),
        Exercise("def_leg_2", "Frontkniebeugen (Langhantel)", "Beine", 3, "6-8", 65f, 6, 70f, "Aufrechter Oberkörper, extremer Quad-Fokus"),
        Exercise("def_leg_3", "Beinpresse 45°", "Beine", 4, "10-12", 180f, 12, 190f, "Volle Bewegungsamplitude, Knie nicht durchstrecken"),
        Exercise("def_leg_4", "Beinpresse Horizontal", "Beine", 3, "12-15", 120f, 12, 130f, "Gleichmäßige Druckbewegung aus den Fersen"),
        Exercise("def_leg_5", "Hackenschmidt Kniebeugen Maschine", "Beine", 4, "8-10", 80f, 8, 85f, "Rücken fest am Polster, maximale Quad-Dehnung"),
        Exercise("def_leg_6", "Beinstrecker Maschine", "Beine", 4, "12-15", 55f, 12, 60f, "1-2s Haltepunkt oben für maximale Kontraktion"),
        Exercise("def_leg_7", "Beinbeuger Maschine (sitzend)", "Beine", 4, "10-12", 50f, 10, 52.5f, "Oberschenkel fixiert, Beugung bis zum Anschlag"),
        Exercise("def_leg_8", "Beinbeuger Maschine (liegend)", "Beine", 4, "10-12", 45f, 10, 47.5f, "Hüfte fest auf der Bank halten"),
        Exercise("def_leg_9", "Rumänisches Kreuzheben (RDL Langhantel)", "Beine", 4, "8-10", 80f, 8, 85f, "Hüfte nach hinten schieben, Dehnung im Beinbeuger"),
        Exercise("def_leg_10", "Rumänisches Kreuzheben (Kurzhantel)", "Beine", 3, "10-12", 28f, 10, 30f, "Flexiblere Griffposition für Po & Beinbeuger"),
        Exercise("def_leg_11", "Ausfallschritte (Walking Lunges)", "Beine", 3, "12-15", 20f, 12, 22.5f, "Große Schritte für Gluteus, kleinere für Quads"),
        Exercise("def_leg_12", "Bulgarian Split Squats", "Beine", 3, "8-10", 18f, 8, 20f, "Hinteres Bein auf Bank auflegen, tiefe Beugung"),
        Exercise("def_leg_13", "Hip Thrusts (Langhantel)", "Beine", 4, "8-12", 100f, 10, 105f, "Polster verwenden, oben Gluteus maximal anspannen"),
        Exercise("def_leg_14", "Hip Thrusts Maschine", "Beine", 3, "10-12", 70f, 10, 75f, "Fester Beckengurt, isolierte Po-Aktivierung"),
        Exercise("def_leg_15", "Adduktoren Maschine", "Beine", 3, "12-15", 45f, 15, 50f, "Innenschenkel stärken und Knie stabilisieren"),
        Exercise("def_leg_16", "Abduktoren Maschine", "Beine", 3, "12-15", 50f, 15, 55f, "Oberer Gluteus & Hüftstabilität"),

        // ==================== WADEN ====================
        Exercise("def_calf_1", "Wadenheben (stehend Maschine)", "Waden", 4, "12-15", 70f, 15, 75f, "Unten 2 Sekunden dehnen, oben 1 Sekunde halten"),
        Exercise("def_calf_2", "Wadenheben (sitzend Maschine)", "Waden", 4, "12-15", 40f, 15, 45f, "Isoliert den Schollenmuskel (Musculus Soleus)"),
        Exercise("def_calf_3", "Wadenheben an der Beinpresse", "Waden", 3, "15-20", 140f, 15, 150f, "Fußspitzen am unteren Rand, Beine leicht gebeugt"),

        // ==================== ARME (BIZEPS & TRIZEPS) ====================
        Exercise("def_arm_1", "SZ-Stange Bizepscurls", "Arme", 3, "8-10", 30f, 10, 32.5f, "Handgelenkschonende Stange, kein Schwungholen"),
        Exercise("def_arm_2", "Kurzhantel Bizepscurls (stehend)", "Arme", 3, "10-12", 14f, 10, 16f, "Mit Supination (Eindrehen) in der oberen Phase"),
        Exercise("def_arm_3", "Incline Dumbbell Curls (Schrägbank)", "Arme", 3, "10-12", 12f, 10, 14f, "Maximaler Stretch im langen Bizepskopf"),
        Exercise("def_arm_4", "Hammercurls mit Kurzhantel", "Arme", 3, "10-12", 16f, 10, 18f, "Neutraler Griff für Brachialis & Unterarme"),
        Exercise("def_arm_5", "Hammercurls am Kabelzug (Seil)", "Arme", 3, "12-15", 20f, 12, 22.5f, "Kontinuierliche Spannung, Seil oben aufspreizen"),
        Exercise("def_arm_6", "Bizepscurls am Kabelzug (Stange)", "Arme", 3, "10-12", 25f, 10, 27.5f, "Gleichmäßiger Widerstand über die gesamte ROM"),
        Exercise("def_arm_7", "Preacher Curls (SZ-Stange Scott-Bank)", "Arme", 3, "8-10", 25f, 8, 27.5f, "Oberarme fixiert, verhindert jegliches Abfälschen"),
        Exercise("def_arm_8", "Scott-Curls Maschine", "Arme", 3, "10-12", 30f, 10, 32.5f, "Geführte Bizeps-Isolation bis zum Muskelversagen"),
        Exercise("def_arm_9", "Konzentrationscurls Kurzhantel", "Arme", 3, "10-12", 12f, 10, 14f, "Ellenbogen am Oberschenkel abgestützt"),
        Exercise("def_arm_10", "Trizepsdrücken am Kabelzug (Seil)", "Arme", 4, "10-12", 25f, 12, 27.5f, "Ellenbogen am Körper fixiert, Seil unten spreizen"),
        Exercise("def_arm_11", "Trizepsdrücken am Kabelzug (gerade Stange)", "Arme", 3, "10-12", 30f, 10, 32.5f, "Fokus auf die äußeren Trizepsköpfe"),
        Exercise("def_arm_12", "Trizepsdrücken am Kabelzug (V-Griff)", "Arme", 3, "10-12", 30f, 10, 32.5f, "Ergonomischer Griff für höhere Arbeitslasten"),
        Exercise("def_arm_13", "French Press (Skullcrusher SZ-Stange)", "Arme", 3, "8-10", 32.5f, 8, 35f, "Liegende Ausführung zur Stirn für langen Trizepskopf"),
        Exercise("def_arm_14", "Stirndrücken (Kurzhanteln)", "Arme", 3, "10-12", 12f, 10, 14f, "Neutrale Handhaltung, isoliert beide Trizepse"),
        Exercise("def_arm_15", "Trizeps Überkopfdrücken am Kabelzug", "Arme", 3, "12-15", 20f, 12, 22.5f, "Starke Dehnung des langen Trizepskopfes"),
        Exercise("def_arm_16", "Dip-Maschine (Trizeps)", "Arme", 3, "10-12", 60f, 10, 65f, "Sitzend, aufrechter Körper für Trizepsschwerpunkt"),
        Exercise("def_arm_17", "Dips an der Bank", "Arme", 3, "12-15", 0f, 15, 10f, "Hände hinter dem Rücken auf Bank abgestützt"),
        Exercise("def_arm_18", "Enges Bankdrücken (Trizeps)", "Arme", 3, "6-8", 60f, 8, 62.5f, "Schulterbreiter Griff, Ellenbogen eng führen"),
        Exercise("def_arm_19", "Trizeps Kickbacks am Kabelzug", "Arme", 3, "12-15", 7.5f, 12, 10f, "Oberarm parallel zum Boden, Streckung hinten halten"),

        // ==================== BAUCH (CORE) ====================
        Exercise("def_core_1", "Crunches am Kabelzug (Kneeling)", "Bauch", 4, "12-15", 35f, 12, 37.5f, "Kniend vor dem Kabelzug, Rumpf bewusst einrollen"),
        Exercise("def_core_2", "Beinheben hängend", "Bauch", 3, "12-15", 0f, 12, 0f, "An Klimmzugstange hängend, Becken anheben"),
        Exercise("def_core_3", "Beinheben am Holm (Kapitänstuhl)", "Bauch", 3, "12-15", 0f, 15, 0f, "Unterarme abgestützt, Knie kontrolliert zur Brust"),
        Exercise("def_core_4", "Beinheben liegend", "Bauch", 3, "15-20", 0f, 15, 0f, "Liegend auf der Matte, unterer Rücken flach am Boden"),
        Exercise("def_core_5", "Bauchpresse Maschine", "Bauch", 3, "12-15", 40f, 12, 45f, "Geführte Rumpfbeugung mit einstellbarem Gewicht"),
        Exercise("def_core_6", "Planks (Unterarmstütz)", "Bauch", 3, "60s", 0f, 60, 0f, "Statische Haltekraft für den gesamten Core"),
        Exercise("def_core_7", "Ab Wheel Rollouts", "Bauch", 3, "10-12", 0f, 10, 0f, "Maximale exzentrische Belastung für die Bauchmuskeln"),
        Exercise("def_core_8", "Russian Twists", "Bauch", 3, "20", 5f, 20, 10f, "Rotation für die schrägen Bauchmuskeln"),
        Exercise("def_core_9", "Side Planks", "Bauch", 3, "45s", 0f, 45, 0f, "Seitliche Rumpfstabilität und schräge Bauchwand"),
        Exercise("def_core_10", "Woodchoppers am Kabelzug", "Bauch", 3, "12-15", 15f, 12, 17.5f, "Diagonale Rotationsbewegung unter Zugspannung")
    )

    // Flow of all exercises in library (Built-in + User Custom Created)
    val allLibraryExercisesFlow: Flow<List<Exercise>> = dao.getAllCustomExercisesFlow().map { customEntities ->
        val customExercises = customEntities.map { entity ->
            Exercise(
                id = entity.id,
                name = entity.name,
                category = entity.category,
                defaultSets = entity.defaultSets,
                targetReps = entity.targetReps,
                lastWeightKg = 0f,
                lastReps = 0,
                suggestedWeightKg = 20f,
                notes = entity.notes
            )
        }
        defaultExercises + customExercises
    }

    // Flow of custom workout plans created by user
    val customWorkoutPlansFlow: Flow<List<CustomWorkoutPlan>> = dao.getAllCustomPlansFlow().map { entities ->
        entities.map { it.toModel() }
    }

    // Split Templates: ONLY custom workout plans created by user (NO automatic templates!)
    val allSplitTemplatesFlow: Flow<List<WorkoutSplitTemplate>> = customWorkoutPlansFlow.map { customPlans ->
        val customTemplates = mutableListOf<WorkoutSplitTemplate>()

        customPlans.forEach { plan ->
            plan.days.forEachIndexed { dayIdx, day ->
                customTemplates.add(
                    WorkoutSplitTemplate(
                        id = "${plan.id}_day_$dayIdx",
                        title = "${plan.name}: ${day.name}",
                        subtitle = "Eigener Plan (${day.exercises.size} Übungen)",
                        exercises = day.exercises,
                        isCustom = true,
                        planId = plan.id
                    )
                )
            }
        }

        customTemplates
    }

    suspend fun saveCustomExercise(
        name: String,
        category: String,
        defaultSets: Int = 3,
        targetReps: String = "8-12",
        notes: String = ""
    ): Exercise = withContext(Dispatchers.IO) {
        val id = "custom_ex_${java.util.UUID.randomUUID().toString().take(8)}"
        val entity = CustomExerciseEntity(
            id = id,
            name = name.trim(),
            category = category.trim(),
            defaultSets = defaultSets,
            targetReps = targetReps.trim(),
            notes = notes.trim(),
            timestamp = System.currentTimeMillis()
        )
        dao.insertCustomExercise(entity)
        CoachRepository(context).saveFullCloudVaultBackup()
        Exercise(
            id = id,
            name = entity.name,
            category = entity.category,
            defaultSets = entity.defaultSets,
            targetReps = entity.targetReps,
            lastWeightKg = 0f,
            lastReps = 0,
            suggestedWeightKg = 20f,
            notes = entity.notes
        )
    }

    suspend fun deleteCustomExercise(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCustomExercise(id)
        CoachRepository(context).saveFullCloudVaultBackup()
    }

    suspend fun saveCustomPlan(plan: CustomWorkoutPlan) = withContext(Dispatchers.IO) {
        dao.insertCustomPlan(plan.toEntity())
        backupActiveUserPlans()
    }

    suspend fun deleteCustomPlan(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCustomPlan(id)
        backupActiveUserPlans()
    }

    private suspend fun backupActiveUserPlans() {
        val prefs = context.getSharedPreferences("bulk_coach_prefs", Context.MODE_PRIVATE)
        var activeEmail = prefs.getString("active_user_email", null)?.lowercase()?.trim()
        if (activeEmail.isNullOrBlank()) {
            activeEmail = dao.getUserProfile()?.email?.lowercase()?.trim()
        }
        if (!activeEmail.isNullOrBlank()) {
            val allPlans = dao.getAllCustomPlans()
            val serialized = com.example.data.local.DatabaseSeeder.serializePlans(allPlans)
            prefs.edit().putString("cloud_plans_$activeEmail", serialized).apply()
            CoachRepository(context).saveFullCloudVaultBackup(activeEmail)
        } else {
            CoachRepository(context).saveFullCloudVaultBackup()
        }
    }

    suspend fun getLastLogForExercise(exerciseName: String): WorkoutLogEntry? = withContext(Dispatchers.IO) {
        dao.getLastWorkoutLogForExercise(exerciseName)?.toModel()
    }

    fun calculateWorkoutCalories(
        userProfile: UserProfile,
        durationMinutes: Int,
        logs: List<WorkoutLogEntry>
    ): WorkoutCalorieResult {
        val weightKg = userProfile.currentWeightKg.coerceIn(40f, 200f)

        var totalVolumeKg = 0f
        var completedSetsCount = 0
        var heavyCompoundSetsCount = 0
        var moderateCompoundSetsCount = 0
        var isolationSetsCount = 0

        val heavyCompoundKeywords = listOf(
            "kniebeugen", "squat", "kreuzheben", "deadlift", "bankdrücken", "bench",
            "beinpresse", "leg press", "hackenschmidt", "rudern vorgebeugt", "t-bar",
            "military press", "overhead press", "dips"
        )
        val moderateCompoundKeywords = listOf(
            "latzug", "klimmzüge", "pull up", "chin up", "schulterdrücken", "hip thrust",
            "rdl", "rumänisches", "schrägbank", "incline", "ausfallschritte", "lunges",
            "split squat", "brustpresse", "rudern"
        )

        logs.forEach { log ->
            val nameLower = log.exerciseName.lowercase()
            val isHeavy = heavyCompoundKeywords.any { nameLower.contains(it) }
            val isModerate = !isHeavy && moderateCompoundKeywords.any { nameLower.contains(it) }

            log.sets.forEach { set ->
                if (set.isCompleted) {
                    completedSetsCount++
                    totalVolumeKg += (set.weightKg * set.reps)
                    if (isHeavy) {
                        heavyCompoundSetsCount++
                    } else if (isModerate) {
                        moderateCompoundSetsCount++
                    } else {
                        isolationSetsCount++
                    }
                }
            }
        }

        // =========================================================================
        // HYBRID-KALORIENBERECHNUNG (VOLUMEN + ZEIT MIT LOGIK-SCHUTZ)
        // =========================================================================
        // a) VOLUMEN-KALORIEN: Gesamtes bewegtes Gewicht (Summe aus Gewicht × Wdh) × 0.012 kcal
        val volumeKcal = (totalVolumeKg * 0.012f).roundToInt()

        // b) SMART-TIME-SCHUTZ:
        // Mindestdauer-Schätzung = anzahlSätze * 1.5 Minuten (Satzdauer + Satzpause)
        val estimatedMinMinutes = (completedSetsCount * 1.5f).roundToInt()
        val effectiveMinutes = maxOf(durationMinutes, estimatedMinMinutes).coerceAtLeast(1)

        // c) ZEIT-KALORIEN: Dauer des Workouts in Minuten × (MET Krafttraining ~ 4.0) × Körpergewicht / 200
        val metValue = if (completedSetsCount > 0) {
            val weightedMet = (
                (heavyCompoundSetsCount * 5.5) +
                (moderateCompoundSetsCount * 4.3) +
                (isolationSetsCount * 3.8)
            ) / completedSetsCount.toDouble()
            weightedMet.coerceIn(3.8, 5.8)
        } else {
            4.0
        }
        val timeKcal = ((effectiveMinutes * metValue * 3.5 * weightKg) / 200.0).roundToInt()

        // Gesamtkalorien = Volumen-Kalorien + Zeit-Kalorien
        val totalBurn = if (completedSetsCount == 0) {
            timeKcal.coerceAtLeast(1)
        } else {
            (volumeKcal + timeKcal).coerceIn(15, 950)
        }

        val compoundSummary = when {
            heavyCompoundSetsCount >= 4 -> "Schweres Verbundtraining (MET ~${String.format(java.util.Locale.US, "%.1f", metValue)})"
            moderateCompoundSetsCount >= 4 -> "Hypertrophie-Volumentraining (MET ~${String.format(java.util.Locale.US, "%.1f", metValue)})"
            else -> "Krafttraining (MET ~${String.format(java.util.Locale.US, "%.1f", metValue)})"
        }

        return WorkoutCalorieResult(
            totalCaloriesBurned = totalBurn,
            durationMinutes = effectiveMinutes,
            totalVolumeKg = totalVolumeKg,
            completedSetsCount = completedSetsCount,
            totalExercisesCount = logs.size,
            baseMetCalories = timeKcal,
            volumeLoadCalories = volumeKcal,
            intensityBonusCalories = 0,
            compoundExercisesSummary = compoundSummary
        )
    }

    suspend fun updatePlanExerciseDefaults(
        planId: String,
        updatedLogs: List<WorkoutLogEntry>
    ) = withContext(Dispatchers.IO) {
        val existingPlans = dao.getAllCustomPlans()
        val targetPlan = existingPlans.firstOrNull { it.id == planId } ?: return@withContext
        val modelPlan = targetPlan.toModel()

        var planModified = false
        val newDays = modelPlan.days.map { day ->
            val newExercises = day.exercises.map { ex ->
                val matchingLog = updatedLogs.firstOrNull { it.exerciseName.trim().equals(ex.name.trim(), ignoreCase = true) }
                val targetSet = matchingLog?.sets?.lastOrNull { it.isCompleted && it.weightKg > 0f }
                    ?: matchingLog?.sets?.maxByOrNull { it.weightKg }
                    ?: matchingLog?.sets?.lastOrNull { it.weightKg > 0f }

                if (targetSet != null && targetSet.weightKg > 0f) {
                    planModified = true
                    val lastWeight = targetSet.weightKg
                    val lastReps = targetSet.reps
                    val nextSuggested = if (lastReps >= 12) {
                        lastWeight + (if (ex.category.contains("Bein", ignoreCase = true)) 5.0f else 2.5f)
                    } else if (lastReps < 5) {
                        (lastWeight - 2.5f).coerceAtLeast(0f)
                    } else {
                        lastWeight
                    }
                    ex.copy(
                        lastWeightKg = lastWeight,
                        lastReps = lastReps,
                        suggestedWeightKg = nextSuggested,
                        defaultSets = (matchingLog?.sets?.size ?: ex.defaultSets).coerceAtLeast(ex.defaultSets)
                    )
                } else {
                    ex
                }
            }
            day.copy(exercises = newExercises)
        }

        if (planModified) {
            val updatedModelPlan = modelPlan.copy(days = newDays)
            dao.insertCustomPlan(updatedModelPlan.toEntity())
            backupActiveUserPlans()
        }
    }

    suspend fun updateAllMatchingExercisesInPlans(
        updatedLogs: List<WorkoutLogEntry>
    ) = withContext(Dispatchers.IO) {
        val allPlans = dao.getAllCustomPlans()
        allPlans.forEach { entity ->
            val model = entity.toModel()
            var modified = false
            val newDays = model.days.map { day ->
                val newExList = day.exercises.map { ex ->
                    val matchingLog = updatedLogs.firstOrNull { it.exerciseName.trim().equals(ex.name.trim(), ignoreCase = true) }
                    val targetSet = matchingLog?.sets?.lastOrNull { it.isCompleted && it.weightKg > 0f }
                        ?: matchingLog?.sets?.maxByOrNull { it.weightKg }
                        ?: matchingLog?.sets?.lastOrNull { it.weightKg > 0f }

                    if (targetSet != null && targetSet.weightKg > 0f) {
                        modified = true
                        val lastWeight = targetSet.weightKg
                        val lastReps = targetSet.reps
                        val nextSuggested = if (lastReps >= 12) {
                            lastWeight + (if (ex.category.contains("Bein", ignoreCase = true)) 5.0f else 2.5f)
                        } else if (lastReps < 5) {
                            (lastWeight - 2.5f).coerceAtLeast(0f)
                        } else {
                            lastWeight
                        }
                        ex.copy(
                            lastWeightKg = lastWeight,
                            lastReps = lastReps,
                            suggestedWeightKg = nextSuggested,
                            defaultSets = (matchingLog?.sets?.size ?: ex.defaultSets).coerceAtLeast(ex.defaultSets)
                        )
                    } else {
                        ex
                    }
                }
                day.copy(exercises = newExList)
            }
            if (modified) {
                dao.insertCustomPlan(model.copy(days = newDays).toEntity())
            }
        }
        backupActiveUserPlans()
    }

    suspend fun saveWorkoutSession(
        splitName: String,
        dateString: String,
        durationMinutes: Int,
        logs: List<WorkoutLogEntry>,
        caloriesBurned: Int = 0
    ): WorkoutSession = withContext(Dispatchers.IO) {
        var totalVolume = 0f
        logs.forEach { log ->
            log.sets.forEach { set ->
                if (set.isCompleted) {
                    totalVolume += (set.weightKg * set.reps)
                }
            }
        }

        val sessionId = java.util.UUID.randomUUID().toString()
        val session = WorkoutSession(
            id = sessionId,
            splitName = splitName,
            dateString = dateString,
            durationMinutes = durationMinutes,
            isCompleted = true,
            totalVolumeKg = totalVolume,
            caloriesBurned = caloriesBurned,
            timestamp = System.currentTimeMillis()
        )

        dao.insertWorkoutSession(session.toEntity())

        logs.forEach { log ->
            val logEntity = WorkoutLogEntryEntity(
                id = java.util.UUID.randomUUID().toString(),
                workoutSessionId = sessionId,
                exerciseName = log.exerciseName,
                exerciseCategory = log.exerciseCategory,
                sets = log.sets,
                timestamp = System.currentTimeMillis()
            )
            dao.insertWorkoutLog(logEntity)
        }

        CoachRepository(context).saveFullCloudVaultBackup()

        session
    }

    suspend fun saveRestDaySession(): WorkoutSession = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString()
        val session = WorkoutSession(
            id = sessionId,
            splitName = "Ruhetag (Regeneration)",
            dateString = java.time.LocalDate.now().toString(),
            durationMinutes = 0,
            isCompleted = true,
            totalVolumeKg = 0f,
            caloriesBurned = 0,
            timestamp = System.currentTimeMillis()
        )
        dao.insertWorkoutSession(session.toEntity())
        CoachRepository(context).saveFullCloudVaultBackup()
        session
    }

    suspend fun getLogsForSession(sessionId: String): List<WorkoutLogEntry> = withContext(Dispatchers.IO) {
        dao.getAllWorkoutLogs().filter { it.workoutSessionId == sessionId }.map { it.toModel() }
    }

    // Smart Progressive Overload Model
    data class SmartProgression(
        val suggestedWeightKg: Float,
        val suggestedReps: Int,
        val baselineWeightKg: Float,
        val baselineReps: Int,
        val reason: String,
        val isProgressiveIncrease: Boolean
    )

    // Calculate progression or deload weight
    fun calculateTargetWeight(
        lastWeight: Float,
        isDeload: Boolean,
        category: String
    ): Float {
        if (lastWeight <= 0f) return 20f
        if (isDeload) {
            // 20% reduction during deload week
            return (lastWeight * 0.80f)
        }
        // Progressive overload (+2.5 kg for upper body/arms, +5 kg for large leg compounds if above 60kg)
        val increment = if (category == "Beine" && lastWeight >= 60f) 5.0f else 2.5f
        return lastWeight + increment
    }

    // Smart Progressive Overload calculation based on target reps and previous session performance
    fun calculateSmartProgression(
        exercise: Exercise,
        lastLog: WorkoutLogEntry?,
        isDeload: Boolean
    ): SmartProgression {
        val lastCompletedSet = lastLog?.sets?.filter { it.isCompleted }?.maxByOrNull { it.weightKg }
        val lastWeight = lastCompletedSet?.weightKg
            ?: exercise.lastWeightKg.takeIf { it > 0f }
            ?: exercise.suggestedWeightKg.takeIf { it > 0f }
            ?: 20f
        val lastReps = lastCompletedSet?.reps
            ?: exercise.lastReps.takeIf { it > 0 }
            ?: 8

        // Parse target reps range (e.g. "8-12", "6-8", "10", "12-15")
        val repParts = exercise.targetReps.replace("s", "").split("-").mapNotNull { it.trim().toIntOrNull() }
        val minTargetReps = repParts.firstOrNull() ?: 8
        val maxTargetReps = repParts.lastOrNull() ?: minTargetReps

        if (isDeload) {
            val deloadWeight = (lastWeight * 0.80f)
            val roundedDeload = (Math.round(deloadWeight * 2) / 2.0).toFloat()
            return SmartProgression(
                suggestedWeightKg = roundedDeload,
                suggestedReps = minTargetReps,
                baselineWeightKg = lastWeight,
                baselineReps = lastReps,
                reason = "Deload (-20%)",
                isProgressiveIncrease = false
            )
        }

        if (lastCompletedSet == null && exercise.lastWeightKg <= 0f) {
            return SmartProgression(
                suggestedWeightKg = if (exercise.suggestedWeightKg > 0f) exercise.suggestedWeightKg else 20f,
                suggestedReps = minTargetReps,
                baselineWeightKg = 0f,
                baselineReps = 0,
                reason = "Basisgewicht",
                isProgressiveIncrease = false
            )
        }

        // If user achieved or exceeded top of target rep range (e.g. >= 12 on "8-12" or >= 8 on "6-8")
        return if (lastReps >= maxTargetReps) {
            val increment = if (exercise.category.contains("Bein", ignoreCase = true) && lastWeight >= 60f) 5.0f else 2.5f
            val newWeight = lastWeight + increment
            SmartProgression(
                suggestedWeightKg = newWeight,
                suggestedReps = minTargetReps,
                baselineWeightKg = lastWeight,
                baselineReps = lastReps,
                reason = "+${if (increment == 5.0f) "5,0" else "2,5"} kg ($maxTargetReps Wdh geschafft)",
                isProgressiveIncrease = true
            )
        } else {
            // User hasn't hit top rep range yet: keep weight and aim for more reps
            val nextReps = (lastReps + 1).coerceAtMost(maxTargetReps)
            SmartProgression(
                suggestedWeightKg = lastWeight,
                suggestedReps = nextReps,
                baselineWeightKg = lastWeight,
                baselineReps = lastReps,
                reason = "Ziel: $nextReps Wdh mit ${if (lastWeight % 1f == 0f) lastWeight.toInt().toString() else lastWeight.toString()} kg",
                isProgressiveIncrease = false
            )
        }
    }

    private fun WorkoutSessionEntity.toModel(): WorkoutSession {
        return WorkoutSession(
            id = id,
            splitName = splitName,
            dateString = dateString,
            durationMinutes = durationMinutes,
            isCompleted = isCompleted,
            totalVolumeKg = totalVolumeKg,
            caloriesBurned = caloriesBurned,
            timestamp = timestamp
        )
    }

    private fun WorkoutSession.toEntity(): WorkoutSessionEntity {
        return WorkoutSessionEntity(
            id = id,
            splitName = splitName,
            dateString = dateString,
            durationMinutes = durationMinutes,
            isCompleted = isCompleted,
            totalVolumeKg = totalVolumeKg,
            caloriesBurned = caloriesBurned,
            timestamp = timestamp
        )
    }

    private fun WorkoutLogEntryEntity.toModel(): WorkoutLogEntry {
        return WorkoutLogEntry(
            id = id,
            workoutSessionId = workoutSessionId,
            exerciseName = exerciseName,
            exerciseCategory = exerciseCategory,
            sets = sets,
            timestamp = timestamp
        )
    }

    private fun CustomWorkoutPlan.toEntity(): CustomWorkoutPlanEntity {
        val daysArray = JSONArray()
        days.forEach { day ->
            val dayObj = JSONObject().apply {
                put("id", day.id)
                put("name", day.name)
                val exArray = JSONArray()
                day.exercises.forEach { ex ->
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
        }
        return CustomWorkoutPlanEntity(
            id = id,
            name = name,
            description = description,
            daysJson = daysArray.toString(),
            timestamp = timestamp
        )
    }

    private fun CustomWorkoutPlanEntity.toModel(): CustomWorkoutPlan {
        val daysList = mutableListOf<CustomWorkoutDay>()
        runCatching {
            val daysArray = JSONArray(daysJson)
            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val exList = mutableListOf<Exercise>()
                val exArray = dayObj.optJSONArray("exercises")
                if (exArray != null) {
                    for (j in 0 until exArray.length()) {
                        val exObj = exArray.getJSONObject(j)
                        exList.add(
                            Exercise(
                                id = exObj.optString("id", java.util.UUID.randomUUID().toString()),
                                name = exObj.optString("name", "Übung"),
                                category = exObj.optString("category", "Ganzkörper"),
                                defaultSets = exObj.optInt("defaultSets", 3),
                                targetReps = exObj.optString("targetReps", "8-12"),
                                lastWeightKg = exObj.optDouble("lastWeightKg", 0.0).toFloat(),
                                lastReps = exObj.optInt("lastReps", 0),
                                suggestedWeightKg = exObj.optDouble("suggestedWeightKg", 20.0).toFloat(),
                                notes = exObj.optString("notes", "")
                            )
                        )
                    }
                }
                daysList.add(
                    CustomWorkoutDay(
                        id = dayObj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = dayObj.optString("name", "Tag ${i + 1}"),
                        exercises = exList
                    )
                )
            }
        }
        return CustomWorkoutPlan(
            id = id,
            name = name,
            description = description,
            days = daysList,
            timestamp = timestamp
        )
    }
}

data class WorkoutSplitTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val exercises: List<Exercise>,
    val isCustom: Boolean = false,
    val planId: String? = null
)

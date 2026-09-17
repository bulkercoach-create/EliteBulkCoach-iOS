package com.example.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomWorkoutPlan
import com.example.data.model.WorkoutCalorieResultSummary
import com.example.data.model.WorkoutSession
import com.example.data.repository.WorkoutSplitTemplate
import com.example.ui.components.RestTimerBanner
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ActiveExerciseState
import com.example.ui.viewmodel.ActiveWorkoutState
import com.example.ui.viewmodel.BulkCoachViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WorkoutScreen(
    viewModel: BulkCoachViewModel
) {
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val workoutSessions by viewModel.workoutSessions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val isRestTimerActive by viewModel.isRestTimerActive.collectAsState()
    val splitTemplates by viewModel.allSplitTemplates.collectAsState()
    val libraryExercises by viewModel.allLibraryExercises.collectAsState()
    val customPlans by viewModel.customWorkoutPlans.collectAsState()
    val completedSummary by viewModel.completedWorkoutSummary.collectAsState()

    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showExerciseLibraryDialog by remember { mutableStateOf(false) }
    var showRestDayDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<CustomWorkoutPlan?>(null) }
    var planToDeleteId by remember { mutableStateOf<String?>(null) }

    val isDeload = userProfile?.isDeloadActive == true
    val currentWeek = userProfile?.currentTrainingWeek ?: 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Rest timer floating at the very top
            RestTimerBanner(
                secondsRemaining = restTimerSeconds,
                isActive = isRestTimerActive,
                onAdd30s = { viewModel.addRestTimerSeconds(30) },
                onCancel = { viewModel.cancelRestTimer() }
            )

            if (activeWorkout != null) {
                // Active Workout Live Logger View
                ActiveWorkoutSessionView(
                    workoutState = activeWorkout!!,
                    onToggleSetCompletion = { exIdx, setIdx, weight, reps ->
                        viewModel.toggleSetCompletion(exIdx, setIdx, weight, reps)
                    },
                    onUpdateSetValues = { exIdx, setIdx, weight, reps ->
                        viewModel.updateSetValues(exIdx, setIdx, weight, reps)
                    },
                    onAddSet = { exIdx -> viewModel.addSetToExercise(exIdx) },
                    onDeleteSet = { exIdx, setIdx -> viewModel.deleteSetFromExercise(exIdx, setIdx) },
                    onFinishWorkout = { viewModel.finishWorkoutSession() },
                    onCancelWorkout = { viewModel.cancelActiveWorkout() }
                )
            } else {
                // Workout Overview & Split Selection View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = "TRAINING & PROGRESSION",
                                color = PrimaryLilac,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isDeload) "Woche $currentWeek: Deload (-20% Last)" else "Woche $currentWeek: Eigener Trainingsplan",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDeload) {
                                Text(
                                    text = "💡 Deload-Woche aktiv: Alle Arbeitsgewichte sind um 20% reduziert zur vollständigen ZNS- und Sehnenregeneration.",
                                    color = AccentBufferCoral,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // The Two Primary Decision Buttons
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. "Neuen Tag / Workout erstellen"
                            Button(
                                onClick = {
                                    editingPlan = null
                                    showCreatePlanDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryLilac,
                                    contentColor = PrimaryLilacDark
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Neuen Tag / Workout erstellen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // 2. "Kein Training / Ruhetag"
                            Button(
                                onClick = { showRestDayDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceCard,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Default.Hotel, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Kein Training / Ruhetag", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            // Übungsspeicher verwalten Link
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showExerciseLibraryDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Übungsspeicher verwalten (${libraryExercises.size} Übungen)",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (splitTemplates.isEmpty()) {
                        // Completely Empty State on first opening / when no custom plans exist
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurfaceCard)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = PrimaryLilac.copy(alpha = 0.6f),
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = "Kein Trainingsplan angelegt",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Du entscheidest zu 100% selbst über dein Training. Klicke oben auf 'Neuen Tag / Workout erstellen', um deinen eigenen Split anzulegen, oder wähle 'Ruhetag'.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "MEINE TRAININGSPLÄNE (${splitTemplates.size})",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        items(splitTemplates) { template ->
                            WorkoutSplitCard(
                                template = template,
                                isDeload = isDeload,
                                onStart = { viewModel.startWorkoutSession(template) },
                                onDelete = if (template.isCustom && template.planId != null) {
                                    { planToDeleteId = template.planId }
                                } else null,
                                onEdit = if (template.isCustom && template.planId != null) {
                                    {
                                        editingPlan = customPlans.find { it.id == template.planId }
                                        showCreatePlanDialog = true
                                    }
                                } else null
                            )
                        }
                    }

                    if (workoutSessions.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "ABGESCHLOSSENE SESSIONS & RUHETAGE",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        items(workoutSessions) { session ->
                            CompletedWorkoutSessionRow(session)
                        }
                    }
                }
            }
        }
    }

    // Plan Editor Dialog
    if (showCreatePlanDialog) {
        CustomPlanEditorDialog(
            availableExercises = libraryExercises,
            existingPlan = editingPlan,
            onSavePlan = { plan ->
                viewModel.saveCustomWorkoutPlan(plan)
                showCreatePlanDialog = false
                editingPlan = null
            },
            onCreateNewExercise = { name, category, defaultSets, targetReps, notes ->
                viewModel.createCustomExercise(name, category, defaultSets, targetReps, notes)
            },
            onDismiss = {
                showCreatePlanDialog = false
                editingPlan = null
            }
        )
    }

    // Exercise Library (Übungsspeicher) Dialog
    if (showExerciseLibraryDialog) {
        ExerciseLibraryDialog(
            exercises = libraryExercises,
            onCreateNewExercise = { name, category, defaultSets, targetReps, notes ->
                viewModel.createCustomExercise(name, category, defaultSets, targetReps, notes)
            },
            onDeleteCustomExercise = { id ->
                viewModel.deleteCustomExercise(id)
            },
            onDismiss = { showExerciseLibraryDialog = false }
        )
    }

    // Rest Day Confirmation Dialog
    if (showRestDayDialog) {
        AlertDialog(
            onDismissRequest = { showRestDayDialog = false },
            title = { Text("Kein Training / Ruhetag?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Heute trainingsfrei. Der Tag wird in deinem Trainingslog als Ruhetag festgehalten. Achte heute auf ausreichende Proteinzufuhr und ZNS-Erholung für maximalen Muskelaufbau!",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logRestDay()
                        showRestDayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLilac, contentColor = PrimaryLilacDark)
                ) {
                    Text("Ruhetag bestätigen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestDayDialog = false }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Delete Plan Confirmation Dialog
    if (planToDeleteId != null) {
        val pid = planToDeleteId!!
        AlertDialog(
            onDismissRequest = { planToDeleteId = null },
            title = { Text("Trainingsplan löschen?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Möchtest du diesen individuellen Trainingsplan wirklich löschen?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomWorkoutPlan(pid)
                        planToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBufferCoral, contentColor = Color.White)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDeleteId = null }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Workout Completion Calorie Burn Summary Dialog
    if (completedSummary != null) {
        WorkoutSummaryDialog(
            summary = completedSummary!!,
            onDismiss = { viewModel.dismissWorkoutSummary() }
        )
    }
}

@Composable
fun WorkoutSplitCard(
    template: WorkoutSplitTemplate,
    isDeload: Boolean,
    onStart: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurfaceCard)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF381E72)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = template.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (template.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF381E72))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("EIGENER PLAN", color = PrimaryLilac, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "${template.exercises.size} Übungen • ${template.subtitle}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                if (template.isCustom) {
                    Row {
                        if (onEdit != null) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = PrimaryLilac, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = AccentBufferCoral, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Exercise list preview
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                template.exercises.take(4).forEach { ex ->
                    val displayW = if (ex.suggestedWeightKg > 0f) ex.suggestedWeightKg else ex.lastWeightKg
                    val weightText = if (isDeload) "${(displayW * 0.8f).toInt()}kg (-20%)" else "${if (displayW % 1f == 0f) displayW.toInt() else displayW}kg"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${ex.name}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$weightText (${ex.targetReps} Wdh)",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLilac,
                    contentColor = PrimaryLilacDark
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Workout starten", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActiveWorkoutSessionView(
    workoutState: ActiveWorkoutState,
    onToggleSetCompletion: (exerciseIndex: Int, setIndex: Int, weight: Float, reps: Int) -> Unit,
    onUpdateSetValues: (exerciseIndex: Int, setIndex: Int, weight: Float, reps: Int) -> Unit,
    onAddSet: (exerciseIndex: Int) -> Unit,
    onDeleteSet: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AKTIVES WORKOUT",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = workoutState.template.title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onCancelWorkout,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2930))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Abbrechen", tint = TextMuted)
                }
            }
        }

        // Exercises
        itemsIndexed(
            items = workoutState.exercises,
            key = { exIdx, exState -> "${exState.exercise.name}_${exIdx}_${exState.sets.size}" }
        ) { exIdx, exState ->
            ActiveExerciseCard(
                exState = exState,
                onToggleSetCompletion = { setIdx, weight, reps ->
                    onToggleSetCompletion(exIdx, setIdx, weight, reps)
                },
                onUpdateSetValues = { setIdx, weight, reps ->
                    onUpdateSetValues(exIdx, setIdx, weight, reps)
                },
                onAddSet = { onAddSet(exIdx) },
                onDeleteSet = { setIdx -> onDeleteSet(exIdx, setIdx) }
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onFinishWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLilac,
                    contentColor = PrimaryLilacDark
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Workout abschließen & speichern", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun WorkoutNumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDecimal: Boolean = false,
    placeholder: String = "0"
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        textStyle = TextStyle(
            color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.75f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(PrimaryLilac),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) DarkSurfaceVariant else Color(0xFF26252A))
                    .border(
                        width = 1.dp,
                        color = if (enabled) DarkOutlineVariant else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextMuted.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier.height(52.dp)
    )
}

@Composable
fun ActiveExerciseCard(
    exState: ActiveExerciseState,
    onToggleSetCompletion: (setIndex: Int, weight: Float, reps: Int) -> Unit,
    onUpdateSetValues: (setIndex: Int, weight: Float, reps: Int) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setIndex: Int) -> Unit
) {
    val lastWeight = exState.exercise.lastWeightKg
    val lastReps = exState.exercise.lastReps.takeIf { it > 0 }
        ?: (exState.exercise.targetReps.replace("s", "").split("-").firstOrNull()?.trim()?.toIntOrNull() ?: 8)

    // Progressive Overload KI-Empfehlung basierend auf dem letzten Workout
    val aiRecommendation = when {
        lastWeight <= 0f -> "💡 Erstes Training: Wähle ein sauberes Startgewicht für ${exState.exercise.targetReps} Wdh."
        lastReps >= 12 -> {
            val inc = if (exState.exercise.category.contains("Bein", ignoreCase = true)) "+5.0 kg" else "+2.5 kg bis +5.0 kg"
            "📈 Gewicht erhöhen ($inc), Ziel: 6-8 Wdh"
        }
        lastReps < 5 -> "📉 Gewicht reduzieren (-2.5 kg), Ziel: 8-10 Wdh für bessere Form"
        else -> "🔥 Gewicht beibehalten (${if (lastWeight % 1f == 0f) lastWeight.toInt() else lastWeight} kg), versuche 1 Wdh mehr als letztes Mal ($lastReps Wdh)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceCard)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = exState.exercise.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ziel: ${exState.exercise.targetReps} Wdh • Zuletzt: ${if (lastWeight > 0f) "${if (lastWeight % 1f == 0f) lastWeight.toInt() else lastWeight} kg (${lastReps} Wdh)" else "Kein Vorwert"}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (exState.isDeload) Color(0xFF5A2A2A) else Color(0xFF381E72))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (exState.isDeload) "Deload: ${exState.suggestedWeightKg} kg" else "Vorschlag: ${exState.suggestedWeightKg} kg",
                        color = if (exState.isDeload) AccentBufferCoral else PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // KI-Progressions-Empfehlungs-Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            lastReps >= 12 && lastWeight > 0f -> Color(0xFF132A18)
                            lastReps < 5 && lastWeight > 0f -> Color(0xFF351A1E)
                            else -> Color(0xFF262035)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            lastReps >= 12 && lastWeight > 0f -> Color(0xFF2E7D32).copy(alpha = 0.6f)
                            lastReps < 5 && lastWeight > 0f -> Color(0xFFC62828).copy(alpha = 0.6f)
                            else -> PrimaryLilac.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = aiRecommendation,
                        color = when {
                            lastReps >= 12 && lastWeight > 0f -> Color(0xFF81C784)
                            lastReps < 5 && lastWeight > 0f -> Color(0xFFFF8A80)
                            else -> PrimaryLilac
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sets Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SATZ",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GEWICHT",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "WDH",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "TREND",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(62.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "FERTIG",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(28.dp)) // Spacer for delete button alignment
            }

            // Sets list
            exState.sets.forEachIndexed { setIdx, set ->
                var weightInput by remember(set.setNumber, set.weightKg, set.isCompleted) {
                    val wStr = if (set.weightKg % 1f == 0f) set.weightKg.toInt().toString() else set.weightKg.toString()
                    mutableStateOf(wStr)
                }
                var repsInput by remember(set.setNumber, set.reps, set.isCompleted) {
                    mutableStateOf(set.reps.toString())
                }

                val currentWeight = weightInput.replace(',', '.').toFloatOrNull() ?: set.weightKg
                val currentReps = repsInput.toIntOrNull() ?: set.reps

                // Performance Progression Evaluation:
                // Mehr Leistung -> GRÜN
                // Gleiche Leistung -> GELB
                // Weniger Leistung -> ROT
                val isProgressionHigher = (lastWeight > 0f && (currentWeight > lastWeight || (currentWeight == lastWeight && currentReps > lastReps))) || (lastWeight <= 0f && currentWeight > 0f)
                val isProgressionEqual = lastWeight > 0f && currentWeight == lastWeight && currentReps == lastReps
                val isProgressionLower = lastWeight > 0f && (currentWeight < lastWeight || (currentWeight == lastWeight && currentReps < lastReps))

                val (badgeBg, badgeBorder, badgeText, badgeLabel) = when {
                    isProgressionHigher && lastWeight > 0f -> {
                        val diffKg = currentWeight - lastWeight
                        val label = if (diffKg > 0f) "+${if (diffKg % 1f == 0f) diffKg.toInt().toString() else String.format("%.1f", diffKg)} kg" else "+${currentReps - lastReps} Wdh"
                        Quadruple(Color(0xFF16381C), Color(0xFF4CAF50), Color(0xFF81C784), "▲ $label")
                    }
                    isProgressionHigher && lastWeight <= 0f -> {
                        Quadruple(Color(0xFF2B1D4A), PrimaryLilac, PrimaryLilac, "★ Start")
                    }
                    isProgressionEqual -> {
                        Quadruple(Color(0xFF3A3114), Color(0xFFFFB300), Color(0xFFFFD54F), "= Halten")
                    }
                    isProgressionLower -> {
                        val diffKg = lastWeight - currentWeight
                        val label = if (diffKg > 0f) "-${if (diffKg % 1f == 0f) diffKg.toInt().toString() else String.format("%.1f", diffKg)} kg" else "-${lastReps - currentReps} Wdh"
                        Quadruple(Color(0xFF3D1B1F), Color(0xFFFF5252), Color(0xFFFF8A80), "▼ $label")
                    }
                    else -> {
                        Quadruple(DarkSurfaceVariant, DarkOutlineVariant, TextMuted, "—")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (set.isCompleted) Color(0xFF26242B) else Color(0xFF1F1E24))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set Number / Warmup Badge
                    val setDisplay = set.setLabel.ifBlank { if (set.isWarmup) "W" else "${set.setNumber}" }
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    set.isCompleted -> Color(0xFF381E72)
                                    set.isWarmup || setDisplay == "W" -> Color(0xFF3D2E18)
                                    else -> DarkSurfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = setDisplay,
                            color = when {
                                set.isCompleted -> PrimaryLilac
                                set.isWarmup || setDisplay == "W" -> Color(0xFFFFB74D)
                                else -> TextPrimary
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Weight TextField (no suffix collision, 52dp height, 16sp bold text)
                    WorkoutNumberInputField(
                        value = weightInput,
                        onValueChange = { newW ->
                            weightInput = newW
                            val w = newW.replace(',', '.').toFloatOrNull() ?: set.weightKg
                            val r = repsInput.toIntOrNull() ?: set.reps
                            onUpdateSetValues(setIdx, w, r)
                        },
                        isDecimal = true,
                        enabled = !set.isCompleted,
                        placeholder = "kg",
                        modifier = Modifier.weight(1f)
                    )

                    // Reps TextField (no suffix collision, 52dp height, 16sp bold text)
                    WorkoutNumberInputField(
                        value = repsInput,
                        onValueChange = { newR ->
                            repsInput = newR
                            val w = weightInput.replace(',', '.').toFloatOrNull() ?: set.weightKg
                            val r = newR.toIntOrNull() ?: set.reps
                            onUpdateSetValues(setIdx, w, r)
                        },
                        isDecimal = false,
                        enabled = !set.isCompleted,
                        placeholder = "Wdh",
                        modifier = Modifier.weight(1f)
                    )

                    // Performance Progression Indicator Badge
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Checkbox ("FERTIG")
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = set.isCompleted,
                            onCheckedChange = { _ ->
                                val sanitized = weightInput.replace(',', '.')
                                val w = sanitized.toFloatOrNull() ?: set.weightKg
                                val r = repsInput.toIntOrNull() ?: set.reps
                                onToggleSetCompletion(setIdx, w, r)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isProgressionHigher) Color(0xFF4CAF50) else PrimaryLilac,
                                checkmarkColor = if (isProgressionHigher) Color(0xFF0D2810) else PrimaryLilacDark,
                                uncheckedColor = TextMuted
                            )
                        )
                    }

                    // Delete Set Button
                    IconButton(
                        onClick = { onDeleteSet(setIdx) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Satz löschen",
                            tint = Color(0xFFFF6B6B).copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Add Set Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF26242B))
                    .clickable { onAddSet() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("+ Satz hinzufügen", color = PrimaryLilac, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun CompletedWorkoutSessionRow(session: WorkoutSession) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.GERMAN)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.splitName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${session.dateString} • ${session.durationMinutes} Min",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.caloriesBurned > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "🔥 +${numberFormat.format(session.caloriesBurned)}",
                            color = AccentBufferCoral,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kcal verbrannt",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${numberFormat.format(session.totalVolumeKg.toInt())} kg",
                        color = PrimaryLilac,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gesamtvolumen",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSummaryDialog(
    summary: WorkoutCalorieResultSummary,
    onDismiss: () -> Unit
) {
    val result = summary.result
    val numberFormat = NumberFormat.getNumberInstance(Locale.GERMAN)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Celebration & Split Name
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF381E72)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "WORKOUT BEENDET!",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = summary.splitName,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Calorie Burn Hero Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF262135))
                        .border(1.dp, PrimaryLilac.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔥 +${numberFormat.format(result.totalCaloriesBurned)} kcal",
                            color = TextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "automatisch auf dein heutiges Kalorienziel aufgerechnet!",
                            color = PrimaryLilac,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Stats 3-Column Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1D22))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "DAUER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${result.durationMinutes} Min", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(DarkOutlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "VOLUMEN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${numberFormat.format(result.totalVolumeKg.toInt())} kg", color = PrimaryLilac, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(DarkOutlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "SÄTZE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${result.completedSetsCount} Sätze", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Detailed Burn Calculation Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Berechnungs-Faktoren:",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "• Grund-MET & Dauer (${result.durationMinutes}m)", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "~${result.baseMetCalories} kcal", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "• Bewegte Last & EPOC (${numberFormat.format(result.totalVolumeKg.toInt())} kg)", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "~${result.volumeLoadCalories} kcal", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (result.intensityBonusCalories > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• Hohe Satz-Intensität", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "+${result.intensityBonusCalories} kcal", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "💡 ${result.compoundExercisesSummary}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLilac,
                    contentColor = PrimaryLilacDark
                )
            ) {
                Text("Fertig", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = null,
        containerColor = DarkSurfaceCard,
        shape = RoundedCornerShape(24.dp)
    )
}

package com.example.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomWorkoutDay
import com.example.data.model.CustomWorkoutPlan
import com.example.data.model.Exercise
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

@Composable
fun CustomPlanEditorDialog(
    availableExercises: List<Exercise>,
    existingPlan: CustomWorkoutPlan? = null,
    onSavePlan: (CustomWorkoutPlan) -> Unit,
    onCreateNewExercise: (name: String, category: String, defaultSets: Int, targetReps: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var planName by remember { mutableStateOf(existingPlan?.name ?: "") }
    var planDescription by remember { mutableStateOf(existingPlan?.description ?: "Individueller Trainingsplan") }

    val daysList = remember {
        mutableStateListOf<CustomWorkoutDay>().apply {
            if (existingPlan != null && existingPlan.days.isNotEmpty()) {
                addAll(existingPlan.days)
            }
        }
    }

    var selectedDayIndexForAddingExercise by remember { mutableStateOf<Int?>(null) }
    var editingDayIndex by remember { mutableStateOf<Int?>(null) }
    var dayNameInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkBackground,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF381E72)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("WORKOUT EDITOR", color = PrimaryLilac, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(if (existingPlan == null) "Neuen Plan erstellen" else "Plan bearbeiten", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = TextMuted)
                    }
                }

                // Plan Name and Description
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Name des Trainingsplans (z.B. Mein 4er Split)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceCard,
                        unfocusedContainerColor = DarkSurfaceCard,
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = planDescription,
                    onValueChange = { planDescription = it },
                    label = { Text("Fokus / Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceCard,
                        unfocusedContainerColor = DarkSurfaceCard,
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRAININGSTAGE (${daysList.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Button(
                        onClick = {
                            val newDayNum = daysList.size + 1
                            daysList.add(CustomWorkoutDay(name = "Tag $newDayNum", exercises = emptyList()))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2930),
                            contentColor = PrimaryLilac
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Tag hinzufügen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Days and exercises list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (daysList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceCard)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = PrimaryLilac.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Keine Trainingstage vorhanden",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Klicke oben auf '+ Tag hinzufügen', um deinen ersten Tag anzulegen und beliebig zu benennen.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(daysList) { dayIndex, day ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Day title row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = day.name,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = {
                                                    editingDayIndex = dayIndex
                                                    dayNameInput = day.name
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Umbenennen", tint = PrimaryLilac, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        IconButton(
                                            onClick = { daysList.removeAt(dayIndex) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Tag entfernen", tint = AccentBufferCoral, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Exercise items in this day
                                    if (day.exercises.isEmpty()) {
                                        Text(
                                            text = "Noch keine Übungen für diesen Tag hinzugefügt.",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            day.exercises.forEachIndexed { exIndex, ex ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(DarkSurfaceVariant)
                                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${exIndex + 1}. ${ex.name}",
                                                                color = TextPrimary,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = "${ex.category} • ${ex.defaultSets} Sätze • ${ex.targetReps} Wdh",
                                                                color = TextMuted,
                                                                fontSize = 11.sp
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                val updatedList = day.exercises.toMutableList()
                                                                updatedList.removeAt(exIndex)
                                                                daysList[dayIndex] = day.copy(exercises = updatedList)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = "Entfernen", tint = TextMuted, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Add Exercise to this day button
                                    Button(
                                        onClick = { selectedDayIndexForAddingExercise = dayIndex },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF381E72),
                                            contentColor = PrimaryLilac
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Übung aus Übungsspeicher hinzufügen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Save Plan Button
                Button(
                    onClick = {
                        val finalName = if (planName.isNotBlank()) planName.trim() else "Mein Split"
                        val plan = CustomWorkoutPlan(
                            id = existingPlan?.id ?: java.util.UUID.randomUUID().toString(),
                            name = finalName,
                            description = planDescription.trim(),
                            days = daysList.toList(),
                            timestamp = System.currentTimeMillis()
                        )
                        onSavePlan(plan)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    ),
                    enabled = planName.isNotBlank() && daysList.any { it.exercises.isNotEmpty() }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Trainingsplan speichern", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Rename day dialog
    if (editingDayIndex != null) {
        val idx = editingDayIndex!!
        AlertDialog(
            onDismissRequest = { editingDayIndex = null },
            title = { Text("Trainingstag umbenennen", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = dayNameInput,
                    onValueChange = { dayNameInput = it },
                    label = { Text("Name (z.B. Tag 1: Brust & Arme)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dayNameInput.isNotBlank()) {
                            daysList[idx] = daysList[idx].copy(name = dayNameInput.trim())
                        }
                        editingDayIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLilac, contentColor = PrimaryLilacDark)
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDayIndex = null }) {
                    Text("Abbrechen", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Exercise Picker Dialog when adding exercise to a specific day
    if (selectedDayIndexForAddingExercise != null) {
        val targetDayIndex = selectedDayIndexForAddingExercise!!
        ExercisePickerDialog(
            availableExercises = availableExercises,
            onExerciseSelected = { exercise ->
                val currentDay = daysList[targetDayIndex]
                val updatedExercises = currentDay.exercises + exercise
                daysList[targetDayIndex] = currentDay.copy(exercises = updatedExercises)
                selectedDayIndexForAddingExercise = null
            },
            onCreateNewExercise = { name, cat, sets, reps, notes ->
                onCreateNewExercise(name, cat, sets, reps, notes)
            },
            onDismiss = { selectedDayIndexForAddingExercise = null }
        )
    }
}

package com.example.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Exercise
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
fun ExercisePickerDialog(
    availableExercises: List<Exercise>,
    onExerciseSelected: (Exercise) -> Unit,
    onCreateNewExercise: (name: String, category: String, defaultSets: Int, targetReps: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var showCreateNewDialog by remember { mutableStateOf(false) }

    val categories = listOf("Alle", "Rücken", "Brust", "Schultern", "Beine", "Arme", "Bauch", "Waden")

    fun matchesCategoryFilter(category: String, selectedFilter: String): Boolean {
        if (selectedFilter == "Alle") return true
        return when (selectedFilter) {
            "Rücken" -> category.contains("Rücken", ignoreCase = true)
            "Brust" -> category.contains("Brust", ignoreCase = true)
            "Schultern" -> category.contains("Schulter", ignoreCase = true)
            "Beine" -> category.contains("Bein", ignoreCase = true) || category.contains("Po", ignoreCase = true) || category.contains("Glute", ignoreCase = true)
            "Arme" -> category.contains("Arm", ignoreCase = true) || category.contains("Bizeps", ignoreCase = true) || category.contains("Trizeps", ignoreCase = true)
            "Bauch" -> category.contains("Bauch", ignoreCase = true) || category.contains("Core", ignoreCase = true)
            "Waden" -> category.contains("Wade", ignoreCase = true)
            else -> category.equals(selectedFilter, ignoreCase = true)
        }
    }

    val filteredExercises = availableExercises.filter { ex ->
        val matchesCategory = matchesCategoryFilter(ex.category, selectedCategory)
        val matchesSearch = searchQuery.isBlank() ||
                ex.name.contains(searchQuery, ignoreCase = true) ||
                ex.category.contains(searchQuery, ignoreCase = true) ||
                ex.notes.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

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
                    .fillMaxWidth()
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF381E72)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("ÜBUNGSSPEICHER", color = PrimaryLilac, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Übung auswählen (${filteredExercises.size})", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = TextMuted)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Übung suchen (z.B. Rudern, Bankdrücken)...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryLilac) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Suche leeren", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
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

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryLilac else DarkSurfaceCard)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) PrimaryLilacDark else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Create New Custom Exercise Button
                Button(
                    onClick = { showCreateNewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2B2930),
                        contentColor = PrimaryLilac
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ Komplett neue Übung erstellen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // List of Exercises
                if (filteredExercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
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
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Text("Keine Übung gefunden", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (searchQuery.isNotBlank()) "Keine Treffer für '$searchQuery'. Erstelle jetzt oben eine eigene Übung!" else "Keine Übungen in dieser Kategorie vorhanden.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            val isCustom = exercise.id.startsWith("custom_ex_")
                            val equipmentType = when {
                                exercise.name.contains("Maschine", ignoreCase = true) -> "Maschine"
                                exercise.name.contains("Kabel", ignoreCase = true) -> "Kabelzug"
                                exercise.name.contains("Kurzhantel", ignoreCase = true) || exercise.name.contains("Dumbbell", ignoreCase = true) -> "Kurzhantel"
                                exercise.name.contains("Langhantel", ignoreCase = true) || exercise.name.contains("Barbell", ignoreCase = true) || exercise.name.contains("SZ-Stange", ignoreCase = true) -> "Langhantel"
                                exercise.name.contains("Klimmzug", ignoreCase = true) || exercise.name.contains("Dip", ignoreCase = true) || exercise.name.contains("Liegestütz", ignoreCase = true) || exercise.name.contains("Plank", ignoreCase = true) || exercise.name.contains("Beinheben", ignoreCase = true) -> "Eigengewicht"
                                else -> null
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceCard)
                                    .clickable {
                                        onExerciseSelected(exercise)
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = exercise.name,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isCustom) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF381E72))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("EIGENE", color = PrimaryLilac, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (equipmentType != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DarkSurfaceVariant)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(equipmentType, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(3.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = exercise.category,
                                                color = PrimaryLilac,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "• ${exercise.defaultSets} Sätze • ${exercise.targetReps} Wdh",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (exercise.notes.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = exercise.notes,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF381E72)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen", tint = PrimaryLilac, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateNewDialog) {
        CreateCustomExerciseDialog(
            initialCategory = if (selectedCategory != "Alle") selectedCategory else "Brust",
            onSave = { name, cat, sets, reps, notes ->
                onCreateNewExercise(name, cat, sets, reps, notes)
                showCreateNewDialog = false
            },
            onDismiss = { showCreateNewDialog = false }
        )
    }
}

@Composable
fun CreateCustomExerciseDialog(
    initialCategory: String = "Brust",
    onSave: (name: String, category: String, defaultSets: Int, targetReps: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var defaultSets by remember { mutableStateOf("3") }
    var targetReps by remember { mutableStateOf("8-12") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Brust", "Rücken", "Beine", "Schultern", "Arme", "Core", "Waden", "Ganzkörper")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Neue Übung erstellen",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Übungsname (z.B. Cable Incline Press)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )

                Text("Muskelgruppe:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryLilac else DarkSurfaceVariant)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) PrimaryLilacDark else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = defaultSets,
                        onValueChange = { defaultSets = it },
                        label = { Text("Sätze (z.B. 3)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = DarkOutlineVariant
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetReps,
                        onValueChange = { targetReps = it },
                        label = { Text("Wdh (z.B. 8-12)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = DarkOutlineVariant
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen / Ausführungshinweis (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name.trim(),
                                    category,
                                    defaultSets.toIntOrNull() ?: 3,
                                    targetReps.ifBlank { "8-12" },
                                    notes.trim()
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLilac,
                            contentColor = PrimaryLilacDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Speichern", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

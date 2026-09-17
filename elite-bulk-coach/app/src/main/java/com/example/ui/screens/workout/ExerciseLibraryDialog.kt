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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
fun ExerciseLibraryDialog(
    exercises: List<Exercise>,
    onCreateNewExercise: (name: String, category: String, defaultSets: Int, targetReps: String, notes: String) -> Unit,
    onDeleteCustomExercise: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var showCreateDialog by remember { mutableStateOf(false) }

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

    val filtered = exercises.filter { ex ->
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
                            Text("ÜBUNGSSPEICHER & DATENBANK", color = PrimaryLilac, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${filtered.size} von ${exercises.size} Übungen", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text("Übung suchen (z.B. Rudern, Bankdrücken, Kabelzug)...", color = TextMuted, fontSize = 13.sp) },
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

                // Add Custom Exercise Button
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ Eigene Übung erstellen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Exercises List
                if (filtered.isEmpty()) {
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
                                text = if (searchQuery.isNotBlank()) "Keine Treffer für '$searchQuery'. Du kannst oben eine eigene Übung erstellen!" else "Keine Übungen in dieser Kategorie vorhanden.",
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
                        items(filtered) { exercise ->
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceCard)
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
                                        Text(
                                            text = "${exercise.category} • ${exercise.defaultSets} Sätze • ${exercise.targetReps} Wdh",
                                            color = PrimaryLilac.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (exercise.notes.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = exercise.notes,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    if (isCustom) {
                                        IconButton(
                                            onClick = { onDeleteCustomExercise(exercise.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = AccentBufferCoral, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomExerciseDialog(
            initialCategory = if (selectedCategory != "Alle") selectedCategory else "Brust",
            onSave = { name, cat, sets, reps, notes ->
                onCreateNewExercise(name, cat, sets, reps, notes)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

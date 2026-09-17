package com.example.ui.screens.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.WeightCheckIn
import com.example.ui.theme.AccentSuccessGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel

@Composable
fun CheckInScreen(
    viewModel: BulkCoachViewModel
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val checkIns by viewModel.checkIns.collectAsState()
    val isTodayMonday = viewModel.isTodayMonday

    val profile = userProfile ?: UserProfile()
    var weightInput by remember(profile.currentWeightKg) { mutableStateOf(profile.currentWeightKg.toString()) }
    var heightInput by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var feelingScore by remember { mutableFloatStateOf(8f) }
    var notesInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
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
                        text = "MONTAGS-CHECK-IN",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Wöchentlicher Fortschritt",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Monday Check-in Locked or Active Status
            item {
                if (!isTodayMonday) {
                    // Monday Locked Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(DarkSurfaceCard)
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF381E72)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(28.dp))
                            }

                            Text(
                                text = "Montags-Check-in gesperrt",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Der offizielle Gewichts- & Fortschritts-Check-in öffnet sich jeden Montag automatisch beim Start der App. So filtern wir tägliche Wasserschwankungen heraus und analysieren saubere Wochen-Trends.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Nächster Check-in: ${viewModel.getNextMondayDateFormatted()}",
                                        color = PrimaryLilac,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Active Monday Check-In Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(DarkSurfaceCard)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        text = "Montags-Check-in bereit",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Heute ist Montag – Zeit für dein Wochen-Update!",
                                        color = AccentSuccessGreen,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { weightInput = it },
                                    label = { Text("Gewicht (kg)", color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant,
                                        focusedBorderColor = PrimaryLilac,
                                        unfocusedBorderColor = DarkOutlineVariant
                                    ),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = { heightInput = it },
                                    label = { Text("Größe (cm)", color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Height, contentDescription = null, tint = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant,
                                        focusedBorderColor = PrimaryLilac,
                                        unfocusedBorderColor = DarkOutlineVariant
                                    ),
                                    singleLine = true
                                )
                            }

                            // Feeling score slider (1-10)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wie fühlst du dich (Erholung / Energie)?",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${feelingScore.toInt()} / 10",
                                        color = PrimaryLilac,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = feelingScore,
                                    onValueChange = { feelingScore = it },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryLilac,
                                        activeTrackColor = PrimaryLilac,
                                        inactiveTrackColor = DarkSurfaceVariant
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                placeholder = { Text("Notizen (z.B. Starker Pump, Schlaf 8h, Muskelkater)", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedBorderColor = PrimaryLilac,
                                    unfocusedBorderColor = DarkOutlineVariant
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val w = weightInput.toFloatOrNull() ?: profile.currentWeightKg
                                    val h = heightInput.toIntOrNull() ?: profile.heightCm
                                    viewModel.submitMondayCheckIn(w, feelingScore.toInt(), notesInput, h)
                                    notesInput = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryLilac,
                                    contentColor = PrimaryLilacDark
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Check-in speichern & Kalorien anpassen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Check-in History
            if (checkIns.isNotEmpty()) {
                item {
                    Text(
                        text = "HISTORISCHE CHECK-INS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                items(checkIns) { checkIn ->
                    CheckInHistoryRow(checkIn)
                }
            }
        }
    }
}

@Composable
fun CheckInHistoryRow(checkIn: WeightCheckIn) {
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
            Column {
                Text(
                    text = "${checkIn.weightKg} kg",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${checkIn.dateString} • Erholung: ${checkIn.feelingScore}/10",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                if (checkIn.note.isNotBlank()) {
                    Text(
                        text = checkIn.note,
                        color = PrimaryLilac,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF381E72))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Geloggt", color = PrimaryLilac, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

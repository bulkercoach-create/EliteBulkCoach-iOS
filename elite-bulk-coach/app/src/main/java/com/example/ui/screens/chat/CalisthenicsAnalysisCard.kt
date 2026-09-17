package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalisthenicsAnalysisResult
import com.example.data.model.CalisthenicsAssistanceDrill
import com.example.ui.theme.AccentSuccessGreen
import com.example.ui.theme.AccentWarningOrange
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalisthenicsAnalysisCard(
    result: CalisthenicsAnalysisResult,
    onSaveDrill: (CalisthenicsAssistanceDrill) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val scoreFloat = (result.scorePercentage / 100f).coerceIn(0f, 1f)

    val scoreBadgeColor = when {
        result.scorePercentage >= 85 -> AccentSuccessGreen
        result.scorePercentage >= 70 -> PrimaryLilac
        else -> AccentWarningOrange
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E1B24),
        border = BorderStroke(1.5.dp, PrimaryLilac.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF381E72)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (result.isVideo) Icons.Default.Videocam else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CALISTHENICS FORM-CHECK",
                            color = PrimaryLilac,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = result.skillName,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Score Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scoreBadgeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, scoreBadgeColor.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = scoreBadgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = result.gradeText,
                            color = scoreBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Score Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ausführungs-Sauberkeit", color = TextSecondary, fontSize = 11.sp)
                    Text("${result.scorePercentage}%", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { scoreFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = scoreBadgeColor,
                    trackColor = Color(0xFF2B2930),
                )
            }

            // Section 1: 🎯 Auswertung & Form-Check
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🎯", fontSize = 14.sp)
                    Text(
                        text = "Auswertung & Form-Check",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2B2930),
                    border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = result.formCheckSummary,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Section 2: ⚠️ Gefundene Schwachstellen
            if (result.weakPoints.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 14.sp)
                        Text(
                            text = "Gefundene Schwachstellen",
                            color = AccentWarningOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentWarningOrange.copy(alpha = 0.1f))
                            .border(1.dp, AccentWarningOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        result.weakPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = AccentWarningOrange,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = point,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: 💪 Geschwächte Muskelgruppen
            if (result.weakMuscles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "💪", fontSize = 14.sp)
                        Text(
                            text = "Geschwächte Muskelgruppen",
                            color = PrimaryLilac,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        result.weakMuscles.forEach { muscle ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF381E72),
                                border = BorderStroke(1.dp, PrimaryLilac.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = muscle,
                                    color = PrimaryLilac,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: 🏋️‍♂️ Empfohlene Hilfsübungen
            if (result.recommendedDrills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🏋️‍♂️", fontSize = 14.sp)
                            Text(
                                text = "Empfohlene Hilfsübungen",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${result.recommendedDrills.size} Übungen",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    result.recommendedDrills.forEach { drill ->
                        DrillItemCard(drill = drill, onSave = { onSaveDrill(drill) })
                    }
                }
            }
        }
    }
}

@Composable
fun DrillItemCard(
    drill: CalisthenicsAssistanceDrill,
    onSave: () -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2B2930),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drill.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⏱️ ${drill.setsAndReps}",
                        color = PrimaryLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (!isSaved) {
                            onSave()
                            isSaved = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSaved) AccentSuccessGreen.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isSaved) AccentSuccessGreen else PrimaryLilac
                    ),
                    border = BorderStroke(1.dp, if (isSaved) AccentSuccessGreen else PrimaryLilac.copy(alpha = 0.6f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isSaved) "Gespeichert" else "Speichern",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Target muscles
            if (drill.targetMuscles.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🎯", fontSize = 10.sp)
                    Text(
                        text = drill.targetMuscles,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Tip
            if (drill.tip.isNotBlank()) {
                Text(
                    text = "💡 ${drill.tip}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

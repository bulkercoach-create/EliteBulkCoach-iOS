package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MacroGrid(
    proteinGrams: Float,
    proteinTarget: Int,
    carbsGrams: Float,
    carbsTarget: Int,
    fatGrams: Float,
    fatTarget: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MacroCard(
            title = "PROTEIN",
            currentGrams = proteinGrams.toInt(),
            targetGrams = proteinTarget,
            barColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            title = "CARBS",
            currentGrams = carbsGrams.toInt(),
            targetGrams = carbsTarget,
            barColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            title = "FETTE",
            currentGrams = fatGrams.toInt(),
            targetGrams = fatTarget,
            barColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MacroCard(
    title: String,
    currentGrams: Int,
    targetGrams: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (targetGrams > 0) {
        (currentGrams.toFloat() / targetGrams.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "macro_progress_$title"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }

            Text(
                text = "${currentGrams}g / ${targetGrams}g",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealType

/**
 * Data representation for a single meal's progress state
 */
data class MealProgressData(
    val type: MealType,
    val title: String,
    val splitPercentage: Int,
    val currentKcal: Int,
    val targetKcal: Int
)

/**
 * 2x2 Grid of Animated Meal Progress Cards
 * Formel: (Eingetragene kcal der Mahlzeit / Empfohlene Muskelaufbau-kcal der Mahlzeit) * 100
 */
@Composable
fun MealProgressGrid(
    targetDailyKcal: Int,
    breakfastKcal: Int,
    lunchKcal: Int,
    dinnerKcal: Int,
    snackKcal: Int,
    onMealClick: ((MealType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val meals = listOf(
        MealProgressData(
            type = MealType.BREAKFAST,
            title = "Frühstück",
            splitPercentage = 30,
            currentKcal = breakfastKcal,
            targetKcal = (targetDailyKcal * 0.30f).toInt().coerceAtLeast(1)
        ),
        MealProgressData(
            type = MealType.LUNCH,
            title = "Mittagessen",
            splitPercentage = 35,
            currentKcal = lunchKcal,
            targetKcal = (targetDailyKcal * 0.35f).toInt().coerceAtLeast(1)
        ),
        MealProgressData(
            type = MealType.DINNER,
            title = "Abendessen",
            splitPercentage = 25,
            currentKcal = dinnerKcal,
            targetKcal = (targetDailyKcal * 0.25f).toInt().coerceAtLeast(1)
        ),
        MealProgressData(
            type = MealType.SNACK_SHAKE,
            title = "Snacks",
            splitPercentage = 10,
            currentKcal = snackKcal,
            targetKcal = (targetDailyKcal * 0.10f).toInt().coerceAtLeast(1)
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Frühstück (30%) & Mittagessen (35%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MealProgressCard(
                meal = meals[0],
                onClick = onMealClick?.let { { it(meals[0].type) } },
                modifier = Modifier.weight(1f)
            )
            MealProgressCard(
                meal = meals[1],
                onClick = onMealClick?.let { { it(meals[1].type) } },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Abendessen (25%) & Snacks (10%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MealProgressCard(
                meal = meals[2],
                onClick = onMealClick?.let { { it(meals[2].type) } },
                modifier = Modifier.weight(1f)
            )
            MealProgressCard(
                meal = meals[3],
                onClick = onMealClick?.let { { it(meals[3].type) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual Meal Card with horizontal animated progress bar
 * Matches the styling and Akzent-Blau of the MacroCards
 */
@Composable
fun MealProgressCard(
    meal: MealProgressData,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExceeded = meal.currentKcal > meal.targetKcal
    val rawFraction = if (meal.targetKcal > 0) {
        (meal.currentKcal.toFloat() / meal.targetKcal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = if (isExceeded) 1f else rawFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "meal_progress_${meal.type.name}"
    )

    // Keep consistent accent blue styling even when exceeded (no negative warning red/orange)
    val barColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Titel & Split: z.B. "Frühstück (30%)"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.title} (${meal.splitPercentage}%)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                if (isExceeded) {
                    Text(
                        text = "+${meal.currentKcal - meal.targetKcal} kcal",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Horizontaler Ladebalken (Progress Bar) analog zu Makro-Balken
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
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

            // Stand: "[Eingetragene kcal] / [Ziel kcal] kcal"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.currentKcal} / ${meal.targetKcal} kcal",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                val percent = if (meal.targetKcal > 0) {
                    ((meal.currentKcal.toFloat() / meal.targetKcal.toFloat()) * 100).toInt()
                } else 0
                Text(
                    text = "$percent%",
                    color = if (isExceeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package com.example.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.ChatMessage
import com.example.ui.theme.AccentSuccessGreen
import com.example.ui.theme.AccentWarningOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.PrimaryLilacLightContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel

@Composable
fun CoachChatScreen(
    viewModel: BulkCoachViewModel
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isReplying by viewModel.isCoachReplying.collectAsState()
    val isAnalyzingCali by viewModel.isAnalyzingCalisthenics.collectAsState()
    val nutritionSummary by viewModel.todayNutritionSummary.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showCaliFormSheet by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isReplying, isAnalyzingCali) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showCaliFormSheet) {
        CalisthenicsFormCheckSheet(
            viewModel = viewModel,
            onDismiss = { showCaliFormSheet = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF381E72)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "AXON AI COACH",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Coach & Calisthenics AI",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Calisthenics Form-Check CTA Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF381E72),
                    border = BorderStroke(1.dp, PrimaryLilac),
                    modifier = Modifier.clickable { showCaliFormSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Cali Check",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Nutrition Status Bar (Offen heute)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceCard,
                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentSuccessGreen)
                        )
                        Text(
                            text = "Offen heute:",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${nutritionSummary.remainingCalories} kcal  •  ${nutritionSummary.remainingProtein.toInt()}g P  •  ${nutritionSummary.remainingCarbs.toInt()}g C  •  ${nutritionSummary.remainingFat.toInt()}g F",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Quick Prompts Chips (Scrollable with Calisthenics quick actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Calisthenics Direct Triggers
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF381E72))
                        .clickable { showCaliFormSheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(13.dp))
                        Text("📹 Video Form-Check", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                listOf(
                    "Muscle-Up Form-Check" to "Muscle-Up",
                    "Planche Form-Check" to "Planche",
                    "Handstand Form-Check" to "Handstand",
                    "Front Lever Check" to "Front Lever",
                    "L-Sit Form-Check" to "L-Sit",
                    "Dips Form-Check" to "Dips"
                ).forEach { (label, skill) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2B2930))
                            .clickable {
                                viewModel.analyzeCalisthenicsVideoOrPhoto(
                                    skillName = skill,
                                    mediaUri = null,
                                    directBitmap = null,
                                    isVideo = true,
                                    userNotes = "Schnell-Formcheck"
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🤸", fontSize = 11.sp)
                            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                listOf(
                    "Was kann ich noch essen?" to Icons.Default.Restaurant,
                    "Ich habe nur Eier, Reis & Milch" to Icons.Default.Kitchen,
                    "Bulk-Shake Rezept" to Icons.Default.Restaurant,
                    "Satz fertig" to Icons.Default.FitnessCenter,
                    "Deload-Plan" to Icons.Default.FitnessCenter
                ).forEach { (prompt, icon) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2B2930))
                            .clickable { viewModel.sendChatMessage(prompt) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(12.dp))
                            Text(prompt, color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onSaveDrill = { drill -> viewModel.addCalisthenicsDrillToExercises(drill) }
                    )
                }

                if (isAnalyzingCali) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF381E72).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, PrimaryLilac.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                CircularProgressIndicator(color = PrimaryLilac, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Cali Coach analysiert Video & Form...", color = PrimaryLilac, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Körperspannung, Hebel, Gelenkwinkel & Schwachstellen werden berechnet", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else if (isReplying) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryLilac, modifier = Modifier.size(16.dp))
                            Text("Coach berechnet Nährwerte & Antwort...", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video & Photo trigger button
                IconButton(
                    onClick = { showCaliFormSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF381E72))
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Calisthenics Video Upload",
                        tint = PrimaryLilac,
                        modifier = Modifier.size(22.dp)
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Frage an Coach oder 'Satz fertig'...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceCard,
                        unfocusedContainerColor = DarkSurfaceCard,
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendChatMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PrimaryLilac)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Senden",
                        tint = PrimaryLilacDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSaveDrill: (com.example.data.model.CalisthenicsAssistanceDrill) -> Unit = {}
) {
    val isUser = message.sender == "user"

    if (message.calisthenicsAnalysis != null) {
        // Render rich Calisthenics Analysis Card
        CalisthenicsAnalysisCard(
            result = message.calisthenicsAnalysis,
            onSaveDrill = onSaveDrill
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.82f else 0.88f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(if (isUser) PrimaryLilacLightContainer else DarkSurfaceCard)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isUser) {
                        Text(
                            text = "AXON AI COACH",
                            color = PrimaryLilac,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = message.text,
                        color = if (isUser) Color(0xFF21005D) else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

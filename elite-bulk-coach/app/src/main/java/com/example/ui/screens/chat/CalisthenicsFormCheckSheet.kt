package com.example.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.AccentSuccessGreen
import com.example.ui.theme.DarkOutlineVariant
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryLilac
import com.example.ui.theme.PrimaryLilacDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BulkCoachViewModel
import java.io.File

private enum class CaliCameraAction {
    RECORD_VIDEO,
    TAKE_PHOTO
}

private fun createTempMediaUri(context: Context, isVideo: Boolean): Uri? {
    return try {
        val extension = if (isVideo) ".mp4" else ".jpg"
        val prefix = if (isVideo) "cali_video_" else "cali_photo_"
        val storageDir = File(context.cacheDir, "camera_media").apply { mkdirs() }
        val tempFile = File.createTempFile(prefix, extension, storageDir).apply {
            createNewFile()
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalisthenicsFormCheckSheet(
    viewModel: BulkCoachViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedSkill by remember { mutableStateOf("Muscle-Up") }
    var customSkillInput by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVideoSelected by remember { mutableStateOf(true) }
    var pendingTempUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAction by remember { mutableStateOf<CaliCameraAction?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetSkills = listOf(
        "Muscle-Up",
        "Planche",
        "Handstand",
        "Front Lever",
        "L-Sit",
        "Dips",
        "Pull-Ups",
        "Human Flag"
    )

    // Activity Result Launchers
    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            capturedBitmap = null
            isVideoSelected = true
            errorMessage = null
        }
    }

    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = com.example.util.ImageUtils.processAndSaveImageToAppStorage(context, uri.toString(), "calisthenics")
            selectedMediaUri = Uri.parse("file://$path")
            capturedBitmap = null
            isVideoSelected = false
            errorMessage = null
        }
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success && pendingTempUri != null) {
            selectedMediaUri = pendingTempUri
            capturedBitmap = null
            isVideoSelected = true
            errorMessage = null
        }
    }

    val photoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = com.example.util.ImageUtils.processAndSaveBitmapToAppStorage(context, bitmap, "calisthenics")
            if (path.isNotBlank()) {
                selectedMediaUri = Uri.parse("file://$path")
                capturedBitmap = null
                isVideoSelected = false
                errorMessage = null
            }
        }
    }

    // Safe trigger methods
    fun launchVideoDirectly() {
        try {
            val uri = createTempMediaUri(context, isVideo = true)
            if (uri != null) {
                pendingTempUri = uri
                videoCaptureLauncher.launch(uri)
            } else {
                errorMessage = "Temporäre Datei konnte nicht erstellt werden. Bitte Video aus Galerie wählen."
                Toast.makeText(context, "Kamera konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            errorMessage = "Kamera konnte nicht geöffnet werden (${e.localizedMessage ?: "Fehler"}). Bitte Galerie nutzen."
            Toast.makeText(context, "Kamera konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPhotoDirectly() {
        try {
            photoCaptureLauncher.launch(null)
        } catch (e: Exception) {
            errorMessage = "Kamera konnte nicht geöffnet werden (${e.localizedMessage ?: "Fehler"}). Bitte Galerie nutzen."
            Toast.makeText(context, "Kamera konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            when (pendingAction) {
                CaliCameraAction.RECORD_VIDEO -> launchVideoDirectly()
                CaliCameraAction.TAKE_PHOTO -> launchPhotoDirectly()
                null -> {}
            }
            pendingAction = null
        } else {
            errorMessage = "Kamera-Berechtigung erforderlich für Direktaufnahme. Bitte in den Systemeinstellungen erlauben oder Galerie nutzen."
            Toast.makeText(context, "Kamera-Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraRequest(action: CaliCameraAction) {
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (action == CaliCameraAction.RECORD_VIDEO) {
            if (hasCamera && hasAudio) {
                launchVideoDirectly()
            } else {
                pendingAction = action
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            if (hasCamera) {
                launchPhotoDirectly()
            } else {
                pendingAction = action
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceCard,
        contentColor = TextPrimary,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF381E72)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryLilac,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI CALISTHENICS COACH",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Video- & Form-Analyse",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2930))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Error or Permission Info Banner
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF381E28))
                        .border(1.dp, AccentBufferCoral.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentBufferCoral, modifier = Modifier.size(20.dp))
                        Text(
                            text = errorMessage!!,
                            color = AccentBufferCoral,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = AccentBufferCoral, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Subtitle Description Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
            ) {
                Text(
                    text = "Lade ein Ausführungs-Video oder Foto deines Calisthenics-Skills hoch. Die KI analysiert Körperspannung, Hebel, Gelenkwinkel, Schwachstellen und erstellt gezielte Assistenzübungen.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Step 1: Skill Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Wähle deinen Skill:",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetSkills.forEach { skill ->
                        val isSelected = selectedSkill == skill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryLilac else Color(0xFF2B2930))
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryLilac else DarkOutlineVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSkill = skill
                                    customSkillInput = ""
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = skill,
                                color = if (isSelected) PrimaryLilacDark else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Custom Skill input
                OutlinedTextField(
                    value = customSkillInput,
                    onValueChange = {
                        customSkillInput = it
                        if (it.isNotBlank()) selectedSkill = it
                    },
                    placeholder = { Text("Anderer Skill (z.B. Back Lever, V-Sit, Pistol Squat)...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2B2930),
                        unfocusedContainerColor = Color(0xFF2B2930),
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    singleLine = true
                )
            }

            // Step 2: Upload Video or Photo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. Video oder Foto auswählen:",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // 2x2 Grid of Media Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Videos (Record Live vs Gallery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Video Recording
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handleCameraRequest(CaliCameraAction.RECORD_VIDEO) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedMediaUri != null && isVideoSelected) Color(0xFF381E72) else Color(0xFF2B2930),
                            border = BorderStroke(
                                1.dp,
                                if (selectedMediaUri != null && isVideoSelected) PrimaryLilac else DarkOutlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = if (selectedMediaUri != null && isVideoSelected) PrimaryLilac else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Video aufnehmen",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (selectedMediaUri != null && isVideoSelected) "✓ Video geladen" else "Live Kamera",
                                    color = if (selectedMediaUri != null && isVideoSelected) AccentSuccessGreen else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Video Gallery Picker
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    runCatching { videoGalleryLauncher.launch("video/*") }
                                        .onFailure { e ->
                                            errorMessage = "Fehler beim Öffnen der Galerie: ${e.localizedMessage}"
                                        }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2B2930),
                            border = BorderStroke(1.dp, DarkOutlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Video Galerie",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Aus Datei (.mp4)",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Row 2: Photos (Live Snap vs Gallery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Photo Snap
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handleCameraRequest(CaliCameraAction.TAKE_PHOTO) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (capturedBitmap != null) Color(0xFF381E72) else Color(0xFF2B2930),
                            border = BorderStroke(
                                1.dp,
                                if (capturedBitmap != null) PrimaryLilac else DarkOutlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = if (capturedBitmap != null) PrimaryLilac else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Kamera Snap",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (capturedBitmap != null) "✓ Foto da" else "Live Snapshot",
                                    color = if (capturedBitmap != null) AccentSuccessGreen else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Photo Gallery Picker
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    runCatching { photoGalleryLauncher.launch("image/*") }
                                        .onFailure { e ->
                                            errorMessage = "Fehler beim Öffnen der Galerie: ${e.localizedMessage}"
                                        }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedMediaUri != null && !isVideoSelected) Color(0xFF381E72) else Color(0xFF2B2930),
                            border = BorderStroke(
                                1.dp,
                                if (selectedMediaUri != null && !isVideoSelected) PrimaryLilac else DarkOutlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (selectedMediaUri != null && !isVideoSelected) PrimaryLilac else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Foto Galerie",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (selectedMediaUri != null && !isVideoSelected) "✓ Foto geladen" else "Aus Datei (.jpg)",
                                    color = if (selectedMediaUri != null && !isVideoSelected) AccentSuccessGreen else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Optional Notes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "3. Notiz an den Cali Coach (optional):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = userNotes,
                    onValueChange = { userNotes = it },
                    placeholder = { Text("z.B. 'Komme nicht sauber über die Stange', 'Hüfte knickt nach 5s ab'...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2B2930),
                        unfocusedContainerColor = Color(0xFF2B2930),
                        focusedBorderColor = PrimaryLilac,
                        unfocusedBorderColor = DarkOutlineVariant
                    ),
                    maxLines = 2
                )
            }

            // Submit Button
            val hasMedia = selectedMediaUri != null || capturedBitmap != null
            val activeSkill = if (customSkillInput.isNotBlank()) customSkillInput.trim() else selectedSkill

            Button(
                onClick = {
                    viewModel.analyzeCalisthenicsVideoOrPhoto(
                        skillName = activeSkill,
                        mediaUri = selectedMediaUri,
                        directBitmap = capturedBitmap,
                        isVideo = isVideoSelected,
                        userNotes = userNotes
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLilac,
                    contentColor = PrimaryLilacDark
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (hasMedia) "🚀 KI Form-Analyse starten ($activeSkill)" else "🚀 Analyse mit Skill-Preset ($activeSkill)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

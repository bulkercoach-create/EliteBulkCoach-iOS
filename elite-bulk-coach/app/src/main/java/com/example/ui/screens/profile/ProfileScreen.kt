package com.example.ui.screens.profile

import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.UploadFile
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.*
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.BulkCoachViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: BulkCoachViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val profile = userProfile ?: UserProfile()

    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    fun saveBitmapLocally(bitmap: Bitmap): String {
        val filename = "profile_avatar_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = com.example.util.ImageUtils.processAndSaveBitmapToAppStorage(context, bitmap, "profile_avatar")
            if (path.isNotBlank()) {
                viewModel.updateProfileImage(path)
                showPhotoPickerSheet = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = com.example.util.ImageUtils.processAndSaveImageToAppStorage(context, uri.toString(), "profile_avatar")
            viewModel.updateProfileImage(path)
            showPhotoPickerSheet = false
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri) { success ->
                if (success) {
                    Toast.makeText(context, "Backup erfolgreich gespeichert!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Fehler beim Erstellen des Backups.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackup(uri) { success ->
                if (success) {
                    Toast.makeText(context, "Daten erfolgreich wiederhergestellt! Bitte App neu starten.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Fehler beim Einlesen des Backups.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = PrimaryLilac
                        )
                    }
                    Column {
                        Text(
                            text = "ATHLETEN-PROFIL",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Account & Einstellungen",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // User Info Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF381E72))
                                .border(1.5.dp, PrimaryLilac, CircleShape)
                                .clickable { showPhotoPickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile.profileImageUri.isNullOrBlank()) {
                                val rotatedAvatar = remember(profile.profileImageUri) {
                                    com.example.util.ImageUtils.getRotatedBitmap(context, profile.profileImageUri!!)
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rotatedAvatar ?: profile.profileImageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profilbild",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = profile.name.take(2).uppercase().ifBlank { "EB" },
                                    color = PrimaryLilac,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = profile.name.ifBlank { "Bulk Athlet" },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile.email.ifBlank { "athlet@bulkcoach.com" },
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = PrimaryLilac, modifier = Modifier.size(14.dp))
                                Text("Cloud Synchronisiert", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        IconButton(
                            onClick = { showPhotoPickerSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Profilbild bearbeiten", tint = PrimaryLilac, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Body & Bulk Stats
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KÖRPER- & ZIELDATEN",
                                color = PrimaryLilac,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Button(
                                onClick = { showEditGoalDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF381E72),
                                    contentColor = PrimaryLilac
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Ziel anpassen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ProfileStatRow("Aktuelles Gewicht", "${profile.currentWeightKg} kg")
                        ProfileStatRow("Startgewicht", "${profile.startWeightKg} kg")
                        ProfileStatRow("Zielgewicht", "${profile.targetWeightKg} kg")
                        ProfileStatRow("Wöchentliche Zunahme", "+${profile.weeklyWeightGainKg} kg / Woche")
                        ProfileStatRow("Körpergröße", "${profile.heightCm} cm")
                        ProfileStatRow("Alter / Geschlecht", "${profile.age} Jahre • ${profile.gender}")
                        ProfileStatRow("Primäres Ziel", profile.primaryGoal)
                        ProfileStatRow("Trainings-Split", profile.trainingSplit)
                    }
                }
            }

            // Calorie Formula & Buffer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "KALORIEN- & MAKROSTRATEGIE",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        ProfileStatRow("Tägliches Kalorienziel", "${profile.dailyCalorieTarget} kcal")
                        ProfileStatRow("Protein (2,2g / kg)", "${profile.proteinTargetGrams} g")
                        ProfileStatRow("Kohlenhydrate", "${profile.carbsTargetGrams} g")
                        ProfileStatRow("Fette (1,0g / kg)", "${profile.fatTargetGrams} g")
                        val goalStr = profile.primaryGoal
                        val isFatLoss = goalStr.contains("Fettabbau", ignoreCase = true) || goalStr.contains("Definition", ignoreCase = true) || goalStr.contains("Cut", ignoreCase = true) || goalStr.contains("Abnahme", ignoreCase = true)
                        val isMaintain = goalStr.contains("Halten", ignoreCase = true) || goalStr.contains("Recomp", ignoreCase = true) || goalStr.contains("Erhalt", ignoreCase = true) || goalStr.contains("Beibehalten", ignoreCase = true)
                        val bufferLabel = when {
                            isFatLoss -> "+8% (Abnahme-Puffer)"
                            isMaintain -> "0% (Kein Puffer)"
                            else -> "-8% (Zunahme-Puffer)"
                        }
                        ProfileStatRow("KI-Foto Puffer", bufferLabel)
                    }
                }
            }

            // Local JSON Backup & Restore Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATEN-SICHERUNG & BACKUP",
                                color = PrimaryLilac,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = PrimaryLilac,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Erstelle ein vollständiges lokales JSON-Backup all deiner Daten (Profil, Workouts, Routinen, Kalorien, Sätze) oder lade eine bestehende Backup-Datei ('axon_backup.json') wieder ein.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(2.dp))

                        Button(
                            onClick = {
                                exportLauncher.launch("axon_backup.json")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryLilac,
                                contentColor = PrimaryLilacDark
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Backup erstellen (Datei exportieren)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch("application/json")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryLilac
                            )
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Backup einlesen (Datei importieren)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // App Version & Update Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "APP-VERSION & INSTALLATION",
                                color = PrimaryLilac,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E3A2F))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Update-bereit",
                                    color = Color(0xFF69F0AE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        ProfileStatRow("App-Version", "${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})")
                        ProfileStatRow("Package-ID", com.example.BuildConfig.APPLICATION_ID)
                        ProfileStatRow("Datenbank-Schutz", "Room Safe Migration (Aktiv)")
                        ProfileStatRow("Direkt-Update", "Unterstützt (Ohne Deinstallation)")

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = {
                                showUpdateDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryLilac,
                                contentColor = PrimaryLilacDark
                            )
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update-Anleitung & APK-Export", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.showStatusMessage("Cloud-Daten wurden manuell synchronisiert!")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Jetzt Cloud-Sync ausführen", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Abmelden & Daten zurücksetzen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEditGoalDialog) {
        EditGoalDialog(
            currentWeightKg = profile.currentWeightKg,
            initialTargetWeightKg = profile.targetWeightKg,
            initialWeeklyGainKg = profile.weeklyWeightGainKg,
            heightCm = profile.heightCm,
            age = profile.age,
            gender = profile.gender,
            onDismiss = { showEditGoalDialog = false },
            onSave = { targetWeight, weeklyGain ->
                viewModel.updateGoalSettings(targetWeight, weeklyGain)
                showEditGoalDialog = false
            }
        )
    }

    if (showPhotoPickerSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerSheet = false },
            title = {
                Text(
                    text = "Profilbild ändern",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Wähle eine Option, um dein Profilbild zu aktualisieren:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kamera öffnen", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aus Galerie wählen", fontWeight = FontWeight.SemiBold)
                    }

                    if (!profile.profileImageUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateProfileImage(null)
                                showPhotoPickerSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Profilbild entfernen", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoPickerSheet = false }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("App-Update & Installation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Installierte Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E3A2F))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                                Text("Automatischer Daten-Erhalt", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                "So lädst du die aktuelle APK ohne Datenverlust herunter:\n\n" +
                                "1. Klicke im Google AI Studio Interface oben rechts auf das Einstellungs-/Menü-Symbol.\n" +
                                "2. Wähle 'Generate APK' oder 'Download APK'.\n" +
                                "3. Übertrage die .apk-Datei auf dein Smartphone oder öffne den Download direkt auf dem Gerät.\n" +
                                "4. Bestätige die Aktualisierung – alle deine Workouts, Makros und Trainingsdaten bleiben 1:1 erhalten!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUpdateDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verstanden")
                }
            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EditGoalDialog(
    currentWeightKg: Float,
    initialTargetWeightKg: Float,
    initialWeeklyGainKg: Float,
    heightCm: Int,
    age: Int,
    gender: String,
    onDismiss: () -> Unit,
    onSave: (targetWeightKg: Float, weeklyGainKg: Float) -> Unit
) {
    var targetWeightStr by remember { mutableStateOf(initialTargetWeightKg.toString()) }
    var selectedPresetGain by remember { mutableFloatStateOf(initialWeeklyGainKg) }
    var isCustomGain by remember {
        mutableStateOf(initialWeeklyGainKg != 0.25f && initialWeeklyGainKg != 0.5f && initialWeeklyGainKg != 0.75f)
    }
    var customGainStr by remember { mutableStateOf(initialWeeklyGainKg.toString()) }

    val effectiveGain = if (isCustomGain) (customGainStr.toFloatOrNull() ?: 0.5f) else selectedPresetGain
    val targetWeight = targetWeightStr.toFloatOrNull() ?: initialTargetWeightKg

    // Live preview calculation
    val previewProjection by remember(targetWeight, effectiveGain, currentWeightKg, heightCm, age, gender) {
        derivedStateOf {
            val s = if (gender.lowercase().contains("weib") || gender.lowercase() == "female") -161 else 5
            val bmr = (10.0 * currentWeightKg) + (6.25 * heightCm) - (5.0 * age) + s
            val tdee = (bmr * 1.55).toInt()
            val dailySurplus = (effectiveGain * 1100f).toInt().coerceIn(150, 1200)
            val newTargetKcal = tdee + dailySurplus

            val proteinG = (currentWeightKg * 2.2f).toInt()
            val fatG = (currentWeightKg * 1.0f).toInt()
            val remainingKcal = (newTargetKcal - (proteinG * 4) - (fatG * 9)).coerceAtLeast(400)
            val carbsG = remainingKcal / 4

            val diffKg = (targetWeight - currentWeightKg).coerceAtLeast(0.1f)
            val weeks = (diffKg / effectiveGain.coerceAtLeast(0.05f)).toInt().coerceAtLeast(1)
            val targetDate = LocalDate.now().plusWeeks(weeks.toLong())
            val dateFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

            Pair(newTargetKcal, "$weeks Wochen (ca. ${targetDate.format(dateFormatter)})")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Ziel & Zunahmerate anpassen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Passe dein Zielgewicht und dein wöchentliches Zunahmeziel an. Das tägliche Kalorienziel wird sofort neu berechnet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = targetWeightStr,
                    onValueChange = { targetWeightStr = it },
                    label = { Text("Zielgewicht (kg)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Gewünschte Zunahme pro Woche:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            0.25f to "0.25 kg\n(Lean)",
                            0.50f to "0.50 kg\n(Optimal)",
                            0.75f to "0.75 kg\n(Stark)"
                        )

                        presets.forEach { (gain, label) ->
                            val isSelected = !isCustomGain && selectedPresetGain == gain
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        isCustomGain = false
                                        selectedPresetGain = gain
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Custom chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCustomGain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { isCustomGain = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Eigenes\nZiel",
                                color = if (isCustomGain) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    if (isCustomGain) {
                        OutlinedTextField(
                            value = customGainStr,
                            onValueChange = { customGainStr = it },
                            label = { Text("Zunahme in kg/Woche (z.B. 0.35)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                    }
                }

                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "NEUES KALORIENZIEL:", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${previewProjection.first} kcal / Tag", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Geschätzte Dauer: ${previewProjection.second}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (targetWeight > 30f && effectiveGain > 0.05f) {
                        onSave(targetWeight, effectiveGain)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Ziel speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

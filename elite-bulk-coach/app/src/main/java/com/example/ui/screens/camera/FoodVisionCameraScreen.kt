package com.example.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.MealType
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.AccentBufferCoral
import com.example.ui.theme.ChipBadgeBg
import com.example.ui.theme.DarkBackground
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
fun FoodVisionCameraScreen(
    viewModel: BulkCoachViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingPhoto.collectAsState()
    val analysisResult by viewModel.photoAnalysisResult.collectAsState()
    val barcodeResult by viewModel.barcodeLookupResult.collectAsState()
    val isLookingUpBarcode by viewModel.isLookingUpBarcode.collectAsState()

    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var customKeyInput by remember { mutableStateOf(viewModel.getSavedCustomApiKey()) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyTestResult by remember { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDeniedBanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Camera Capture Launcher with crash safety
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            var targetBmp = bitmap
            if (bitmap.width > bitmap.height) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(90f)
                targetBmp = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            selectedBitmap = targetBmp
            viewModel.analyzeFoodPhoto(targetBmp)
        }
    }

    // Camera Runtime Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionDeniedBanner = false
            runCatching {
                takePhotoLauncher.launch(null)
            }.onFailure { e ->
                errorMessage = "Kamera konnte nicht geöffnet werden: ${e.localizedMessage ?: "Unbekannter Fehler"}. Bitte Galerie nutzen."
            }
        } else {
            showPermissionDeniedBanner = true
            viewModel.showStatusMessage("Kamera-Berechtigung verweigert. Du kannst Bilder aus der Galerie wählen.")
        }
    }

    fun handleCameraClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            runCatching {
                takePhotoLauncher.launch(null)
            }.onFailure { e ->
                errorMessage = "Kamera nicht verfügbar (${e.localizedMessage}). Bitte Galerie nutzen."
            }
        } else {
            // Ask for runtime permission
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Barcode Scanner Permission Launcher
    val barcodePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBarcodeDialog = true
        } else {
            viewModel.showStatusMessage("Kamera-Zugriff abgelehnt. Du kannst Barcodes auch manuell eingeben.")
            showBarcodeDialog = true
        }
    }

    val openBarcodeScannerWithPermission = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showBarcodeDialog = true
        } else {
            barcodePermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = com.example.util.ImageUtils.getRotatedBitmap(context, uri.toString())
            if (bitmap != null) {
                selectedBitmap = bitmap
                viewModel.analyzeFoodPhoto(bitmap)
            } else {
                errorMessage = "Bild konnte nicht dekodiert werden."
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = PrimaryLilac
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VISION KI SCANNER",
                            color = PrimaryLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Foto-Ernährungsanalyse",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            customKeyInput = viewModel.getSavedCustomApiKey()
                            keyTestResult = null
                            showApiKeyDialog = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API-Key einstellen",
                            tint = PrimaryLilac
                        )
                    }
                }
            }

            // Permission Denied Info Banner if applicable
            if (showPermissionDeniedBanner) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF381E28))
                            .padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentBufferCoral)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kamera-Zugriff verweigert",
                                    color = AccentBufferCoral,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kein Problem! Du kannst Mahlzeiten direkt aus deiner Galerie hochladen.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBufferCoral,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Erlauben", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF381E28))
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(errorMessage!!, color = AccentBufferCoral, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = AccentBufferCoral)
                            }
                        }
                    }
                }
            }

            // Image Preview or Placeholder
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Mahlzeit Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF381E72)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryLilac,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Fotografiere deinen Teller oder Barcode",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Gemini KI erkennt Portionen & Nährwerte automatisch",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = PrimaryLilac)
                                Text(
                                    text = "Analysiere Mahlzeit...",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Take Photo or Pick from Gallery or Scan Barcode
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { handleCameraClick() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryLilac,
                                contentColor = PrimaryLilacDark
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Kamera", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                runCatching { pickImageLauncher.launch("image/*") }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B2930),
                                contentColor = PrimaryLilac
                            )
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Galerie", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { openBarcodeScannerWithPermission() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceCard,
                            contentColor = PrimaryLilac
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Barcode Scanner mit Portions-Berechnung", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Demo / Quick Test row for testing macros
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡ BEISPIEL-MAHLZEIT TESTEN:",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, PrimaryLilac.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setSampleFoodAnalysis(
                                                dishName = "Hähnchenbrust mit Basmatireis & Brokkoli",
                                                portion = "ca. 350g",
                                                calories = 520,
                                                protein = 46f,
                                                carbs = 62f,
                                                fat = 8f
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("🍗 Hähnchen & Reis (520 kcal)", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, PrimaryLilac.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setSampleFoodAnalysis(
                                                dishName = "Haferflocken mit Whey & Erdnussbutter",
                                                portion = "ca. 200g",
                                                calories = 650,
                                                protein = 42f,
                                                carbs = 75f,
                                                fat = 18f
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("🥣 Bulk-Oats (650 kcal)", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, PrimaryLilac.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setSampleFoodAnalysis(
                                                dishName = "Magerquark mit Beeren & Mandeln",
                                                portion = "ca. 300g",
                                                calories = 380,
                                                protein = 38f,
                                                carbs = 24f,
                                                fat = 12f
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("🍓 Magerquark-Bowl (380 kcal)", color = PrimaryLilac, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Analysis Result Card
            if (analysisResult != null) {
                val res = analysisResult!!
                item {
                    if (!res.isFood) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF381E28))
                                .border(1.dp, AccentBufferCoral.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AccentBufferCoral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AccentBufferCoral,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                val isApiError = (res.errorMessage?.startsWith("API-Fehler") == true || res.errorMessage?.startsWith("Fehler:") == true)
                                val errorTitle = res.errorMessage ?: "Kein Essen auf dem Foto erkannt! Bitte fotografiere eine Mahlzeit."
                                val errorSubtitle = if (isApiError) {
                                    "Bitte trage deinen GEMINI_API_KEY im Secrets-Panel ein oder überprüfe deine Internetverbindung."
                                } else {
                                    "Die KI analysiert nur essbare Lebensmittel, Getränke und Mahlzeiten. Gegenstände oder unklare Bilder werden nicht getrackt."
                                }

                                Text(
                                    text = errorTitle,
                                    color = AccentBufferCoral,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Text(
                                    text = errorSubtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        viewModel.clearPhotoAnalysis()
                                        handleCameraClick()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentBufferCoral,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Neues Foto aufnehmen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        customKeyInput = viewModel.getSavedCustomApiKey()
                                        keyTestResult = null
                                        showApiKeyDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2B2930),
                                        contentColor = PrimaryLilac
                                    )
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("API-Key einrichten / testen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.setSampleFoodAnalysis(
                                            dishName = "Hähnchen mit Basmatireis",
                                            portion = "ca. 350g",
                                            calories = 520,
                                            protein = 46f,
                                            carbs = 62f,
                                            fat = 8f
                                        )
                                    }
                                ) {
                                    Text("Muster-Mahlzeit direkt laden & testen", color = PrimaryLilac, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
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
                                        text = "ERKANNTES GERICHT",
                                        color = PrimaryLilac,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ChipBadgeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "-8% BULK-SICHERHEIT",
                                            color = AccentBufferCoral,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = res.dishName,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Portion: ${res.portion}",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )

                                // Calories comparison
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Echte Schätzung:", color = TextMuted, fontSize = 10.sp)
                                        Text("${res.rawCalories} kcal", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Eingeloggt (Sicherheits-Puffer):", color = AccentBufferCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${res.bufferedCalories} kcal",
                                            color = AccentBufferCoral,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Macros
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MacroMiniChip(
                                        label = "Protein",
                                        value = "${res.proteinGrams.toInt()}g",
                                        color = PrimaryLilac,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MacroMiniChip(
                                        label = "Carbs",
                                        value = "${res.carbsGrams.toInt()}g",
                                        color = Color(0xFF80D4FF),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MacroMiniChip(
                                        label = "Fett",
                                        value = "${res.fatGrams.toInt()}g",
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Meal Type Selector
                                Text("Mahlzeit zuordnen:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MealType.values().forEach { mealType ->
                                        val isSelected = selectedMealType == mealType
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryLilac else DarkSurfaceVariant)
                                                .clickable { selectedMealType = mealType }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mealType.displayName.take(5),
                                                color = if (isSelected) PrimaryLilacDark else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // Add to Daily Log Button
                                Button(
                                    onClick = {
                                        viewModel.addFoodItem(
                                            name = res.dishName,
                                            portion = res.portion,
                                            calories = res.rawCalories,
                                            protein = res.proteinGrams,
                                            carbs = res.carbsGrams,
                                            fat = res.fatGrams,
                                            mealType = selectedMealType,
                                            isAiEstimated = true
                                        )
                                        viewModel.clearPhotoAnalysis()
                                        onBack()
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
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("In Tageslog eintragen", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeDialog) {
        BarcodeScannerDialog(
            lookupResult = barcodeResult,
            isLoading = isLookingUpBarcode,
            onLookup = { code -> viewModel.lookupBarcode(code) },
            onAddFood = { name, portion, kcal, p, c, f, mealType, barcode ->
                viewModel.addFoodItem(name, portion, kcal, p, c, f, mealType, isAiEstimated = false, barcode = barcode)
                viewModel.clearBarcodeResult()
                onBack()
            },
            onDismiss = {
                showBarcodeDialog = false
                viewModel.clearBarcodeResult()
            },
            onScanPhoto = { bitmap ->
                viewModel.analyzeBarcodeFromPhoto(bitmap)
            }
        )
    }

    if (showApiKeyDialog) {
        val detectedKey = viewModel.getApiKey()
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryLilac)
                    Text("Gemini API-Key Einstellungen", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val statusText = if (detectedKey.isNotBlank()) {
                        "✅ Aktiver API-Key erkannt (Endet auf: ...${detectedKey.takeLast(4)})"
                    } else {
                        "⚠️ Kein API-Key erkannt. Bitte trage deinen Gemini API-Key unten ein."
                    }
                    Text(
                        text = statusText,
                        color = if (detectedKey.isNotBlank()) PrimaryLilac else AccentBufferCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = customKeyInput,
                        onValueChange = {
                            customKeyInput = it
                            keyTestResult = null
                        },
                        label = { Text("Gemini API Key (z.B. AIzaSy...)") },
                        placeholder = { Text("Hier Key einfügen...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLilac,
                            unfocusedBorderColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (keyTestResult != null) {
                        Text(
                            text = keyTestResult!!,
                            color = if (keyTestResult!!.contains("erfolgreich", ignoreCase = true) || keyTestResult!!.contains("OK", ignoreCase = true)) PrimaryLilac else AccentBufferCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            isTestingKey = true
                            keyTestResult = "Teste Verbindung mit Gemini (gemini-3.5-flash)..."
                            viewModel.testApiKey(customKeyInput) { success, msg ->
                                isTestingKey = false
                                keyTestResult = msg
                            }
                        },
                        enabled = !isTestingKey,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceCard,
                            contentColor = PrimaryLilac
                        )
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryLilac, strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Verbinde...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Verbindung jetzt testen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomApiKey(customKeyInput)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLilac,
                        contentColor = PrimaryLilacDark
                    )
                ) {
                    Text("Speichern", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Schließen", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun MacroMiniChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 10.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

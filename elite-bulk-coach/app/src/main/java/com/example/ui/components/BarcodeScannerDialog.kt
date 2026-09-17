package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.MealType
import com.example.data.repository.BarcodeLookupResult
import androidx.compose.material3.MaterialTheme
import java.util.Locale

enum class PortionMode {
    GRAMS,
    PIECES
}

@Composable
fun BarcodeScannerDialog(
    lookupResult: BarcodeLookupResult?,
    isLoading: Boolean,
    onLookup: (String) -> Unit,
    onAddFood: (name: String, portion: String, calories: Int, protein: Float, carbs: Float, fat: Float, mealType: MealType, barcode: String) -> Unit,
    onDismiss: () -> Unit,
    onScanPhoto: ((Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    var barcodeInput by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    val focusManager = LocalFocusManager.current

    // Camera Launchers for live scanning
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && onScanPhoto != null) {
            onScanPhoto(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            runCatching { cameraLauncher.launch(null) }
        }
    }

    // Portion state
    var portionMode by remember { mutableStateOf(PortionMode.GRAMS) }
    var gramsInput by remember { mutableStateOf("100") }
    var pieceCountInput by remember { mutableStateOf("1") }

    // When a new product is loaded, initialize defaults
    LaunchedEffect(lookupResult) {
        if (lookupResult != null) {
            if (lookupResult.hasPieceServing && lookupResult.pieceWeightGrams > 0) {
                portionMode = PortionMode.PIECES
                pieceCountInput = "1"
                val grams = lookupResult.pieceWeightGrams
                gramsInput = if (grams % 1f == 0f) grams.toInt().toString() else "%.1f".format(Locale.US, grams)
            } else {
                portionMode = PortionMode.GRAMS
                gramsInput = "100"
                pieceCountInput = "1"
            }
        }
    }

    // Dynamic Live Nutrition Calculation
    val calculatedNutrition by remember(lookupResult, portionMode, gramsInput, pieceCountInput) {
        derivedStateOf {
            val res = lookupResult ?: return@derivedStateOf null
            if (portionMode == PortionMode.GRAMS) {
                val grams = gramsInput.replace(',', '.').toFloatOrNull() ?: 0f
                res.calculateForGrams(grams)
            } else {
                val pieces = pieceCountInput.replace(',', '.').toFloatOrNull() ?: 0f
                res.calculateForPieces(pieces)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "BARCODE SCANNER",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Flexible Portionsberechnung",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Prominent Camera Scan Button
                Button(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            runCatching { cameraLauncher.launch(null) }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Barcode mit Kamera scannen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Barcode Input & Scanner Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Schnellauswahl Test-Barcodes:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Preset chips for testing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Haferflocken" to "4008400404127",
                                "Quark" to "4014400900018",
                                "Whey" to "4260389580012",
                                "Brot" to "4008400123456"
                            ).forEach { (label, code) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (barcodeInput == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            barcodeInput = code
                                            onLookup(code)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (barcodeInput == code) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Proteinriegel" to "4251877701234",
                                "Bio Ei" to "4001234560000",
                                "Banane" to "4000000000001",
                                "Keks" to "4008400200019"
                            ).forEach { (label, code) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (barcodeInput == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            barcodeInput = code
                                            onLookup(code)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (barcodeInput == code) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = {
                                barcodeInput = it
                                if (it.length >= 8) onLookup(it)
                            },
                            placeholder = { Text("EAN oder Barcode Nummer eingeben...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    focusManager.clearFocus()
                                    if (barcodeInput.isNotBlank()) onLookup(barcodeInput)
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Suchen", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    if (barcodeInput.isNotBlank()) onLookup(barcodeInput)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text("Suche Produkt in Datenbank...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                } else if (lookupResult != null) {
                    val res = lookupResult
                    val calculated = calculatedNutrition

                    // Recognized Product Info Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ERKANNTES PRODUKT",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                if (res.brand.isNotBlank()) {
                                    Text(
                                        text = res.brand,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = res.productName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 100g Reference Tag
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Basis pro 100g: ${res.caloriesPer100g.toInt()} kcal  •  ${String.format(Locale.US, "%.1f", res.proteinPer100g)}g P  •  ${String.format(Locale.US, "%.1f", res.carbsPer100g)}g C  •  ${String.format(Locale.US, "%.1f", res.fatPer100g)}g F",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Flexible Portion Selector Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PORTION WÄHLEN",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Kein 100g-Zwang",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Portion Mode Toggle (Gramm vs Stück/Portionen)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (portionMode == PortionMode.GRAMS) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            portionMode = PortionMode.GRAMS
                                            // Sync grams
                                            if (gramsInput.isBlank() || gramsInput == "0") {
                                                gramsInput = "100"
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = null,
                                            tint = if (portionMode == PortionMode.GRAMS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Gramm (g)",
                                            color = if (portionMode == PortionMode.GRAMS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = if (portionMode == PortionMode.GRAMS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (portionMode == PortionMode.PIECES) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            portionMode = PortionMode.PIECES
                                            if (pieceCountInput.isBlank() || pieceCountInput == "0") {
                                                pieceCountInput = "1"
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (portionMode == PortionMode.PIECES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (res.hasPieceServing) res.pieceUnitName.take(16) else "Stück / Portion",
                                            color = if (portionMode == PortionMode.PIECES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = if (portionMode == PortionMode.PIECES) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Mode 1: GRAMS INPUT
                            if (portionMode == PortionMode.GRAMS) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Minus Button
                                        IconButton(
                                            onClick = {
                                                val curr = gramsInput.toFloatOrNull() ?: 100f
                                                val next = (curr - 10f).coerceAtLeast(5f)
                                                gramsInput = next.toInt().toString()
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "-10g", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        // Grams Text Field
                                        OutlinedTextField(
                                            value = gramsInput,
                                            onValueChange = { gramsInput = it },
                                            label = { Text("Menge in Gramm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                                            trailingIcon = { Text("g", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            ),
                                            singleLine = true
                                        )

                                        // Plus Button
                                        IconButton(
                                            onClick = {
                                                val curr = gramsInput.toFloatOrNull() ?: 100f
                                                val next = curr + 10f
                                                gramsInput = next.toInt().toString()
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+10g", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    // Quick Gram Preset Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("30", "50", "100", "150", "200", "250").forEach { g ->
                                            val isSelected = gramsInput == g
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        gramsInput = g
                                                        focusManager.clearFocus()
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${g}g",
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Mode 2: PIECES / SERVINGS INPUT
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Info row about single piece weight
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "1 Einheit: ${res.pieceUnitName}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "ca. ${res.pieceWeightGrams.toInt()}g",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Minus Button
                                        IconButton(
                                            onClick = {
                                                val curr = pieceCountInput.toFloatOrNull() ?: 1f
                                                val next = (curr - 0.5f).coerceAtLeast(0.5f)
                                                pieceCountInput = if (next % 1f == 0f) next.toInt().toString() else "%.1f".format(Locale.US, next)
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "-0.5", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        // Piece count text field
                                        OutlinedTextField(
                                            value = pieceCountInput,
                                            onValueChange = { pieceCountInput = it },
                                            label = { Text("Anzahl / Stück", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                                            trailingIcon = { Text("Stück", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            ),
                                            singleLine = true
                                        )

                                        // Plus Button
                                        IconButton(
                                            onClick = {
                                                val curr = pieceCountInput.toFloatOrNull() ?: 1f
                                                val next = curr + 0.5f
                                                pieceCountInput = if (next % 1f == 0f) next.toInt().toString() else "%.1f".format(Locale.US, next)
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+0.5", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    // Quick Piece Multiplier Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("0.5", "1", "1.5", "2", "3", "4").forEach { countStr ->
                                            val isSelected = pieceCountInput == countStr
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        pieceCountInput = countStr
                                                        focusManager.clearFocus()
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${countStr}x",
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Calculated Nutrition Card
                    if (calculated != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "BERECHNETE MAHLZEIT",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Für ${calculated.portionText}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${calculated.calories}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "kcal Gesamt",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Live Macro Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MacroCalculatedChip(
                                        label = "Protein",
                                        value = "${String.format(Locale.US, "%.1f", calculated.protein)}g",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MacroCalculatedChip(
                                        label = "Carbs",
                                        value = "${String.format(Locale.US, "%.1f", calculated.carbs)}g",
                                        color = Color(0xFF80D4FF),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MacroCalculatedChip(
                                        label = "Fett",
                                        value = "${String.format(Locale.US, "%.1f", calculated.fat)}g",
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Meal Type Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Mahlzeit zuordnen:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedMealType = mealType }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (mealType) {
                                                MealType.BREAKFAST -> "Frühstück"
                                                MealType.LUNCH -> "Mittag"
                                                MealType.DINNER -> "Abend"
                                                MealType.SNACK_SHAKE -> "Shake"
                                            },
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Primary Action Button: Add to Log
                        Button(
                            onClick = {
                                onAddFood(
                                    res.productName,
                                    calculated.portionText,
                                    calculated.calories,
                                    calculated.protein,
                                    calculated.carbs,
                                    calculated.fat,
                                    selectedMealType,
                                    barcodeInput
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "In Tageslog eintragen (${calculated.calories} kcal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroCalculatedChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

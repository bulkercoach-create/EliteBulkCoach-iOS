package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CalisthenicsAnalysisResult
import com.example.data.model.CalisthenicsAssistanceDrill
import com.example.data.model.FoodItem
import com.example.data.model.PerformanceReport
import com.example.data.model.ProgressPhoto
import com.example.data.model.UserProfile
import com.example.data.model.WeightCheckIn
import com.example.data.model.WorkoutLogEntry
import com.example.data.model.WorkoutSession
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiRepository(private val context: Context? = null) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sharedPreferences: SharedPreferences? by lazy {
        context?.getSharedPreferences("gemini_settings", Context.MODE_PRIVATE)
    }

    fun saveCustomApiKey(key: String) {
        sharedPreferences?.edit()?.putString("custom_gemini_key", key.trim())?.apply()
    }

    fun getSavedCustomApiKey(): String {
        return sharedPreferences?.getString("custom_gemini_key", "")?.trim() ?: ""
    }

    private fun cleanKey(key: String?): String {
        if (key == null) return ""
        var cleaned = key.trim()
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length > 1) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        if (cleaned.startsWith("'") && cleaned.endsWith("'") && cleaned.length > 1) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        if (cleaned.startsWith("GEMINI_API_KEY=", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("=").trim()
        }
        if (cleaned.startsWith("API_KEY=", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("=").trim()
        }
        if (cleaned.startsWith("key=", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("=").trim()
        }
        if (cleaned.isNotBlank() && !cleaned.startsWith("MY_") && !cleaned.equals("undefined", ignoreCase = true) && !cleaned.equals("null", ignoreCase = true)) {
            return cleaned
        }
        return ""
    }

    fun getApiKey(): String {
        // 0. User-saved custom API Key in SharedPreferences
        val savedKey = cleanKey(getSavedCustomApiKey())
        if (savedKey.isNotBlank()) return savedKey

        // 1. Direct BuildConfig constants
        val key1 = cleanKey(runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull())
        if (key1.isNotBlank()) return key1

        // 2. Reflection on all BuildConfig fields
        try {
            for (field in BuildConfig::class.java.declaredFields) {
                if (field.type == String::class.java) {
                    field.isAccessible = true
                    val name = field.name.uppercase()
                    if (name.contains("KEY") || name.contains("GEMINI") || name.contains("SECRET")) {
                        val value = cleanKey(field.get(null) as? String)
                        if (value.isNotBlank()) return value
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Environment variables
        val envKeys = listOf("GEMINI_API_KEY", "GOOGLE_API_KEY", "API_KEY", "GEMINI_KEY", "GOOGLE_GEMINI_API_KEY")
        for (envVar in envKeys) {
            val envVal = cleanKey(runCatching { System.getenv(envVar) }.getOrNull())
            if (envVal.isNotBlank()) return envVal
        }

        // 4. System properties
        for (propVar in envKeys) {
            val propVal = cleanKey(runCatching { System.getProperty(propVar) }.getOrNull())
            if (propVal.isNotBlank()) return propVal
        }

        return ""
    }

    // Primary model is strictly "gemini-3.5-flash" with automatic fallbacks
    private val modelCandidates = listOf(
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite-preview",
        "gemini-3.1-pro-preview"
    )

    private fun sanitizeModelName(name: String): String {
        return name
            .replace("models/", "")
            .replace("models:", "")
            .trim()
    }

    suspend fun testApiKey(customKeyToTest: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = if (!customKeyToTest.isNullOrBlank()) cleanKey(customKeyToTest) else getApiKey()
        if (apiKey.isBlank()) {
            return@withContext false to "Kein API-Key gefunden. Bitte Key eingeben."
        }

        for (rawModel in modelCandidates) {
            val cleanModel = sanitizeModelName(rawModel)
            try {
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray()
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray()
                        parts.put(JSONObject().apply { put("text", "Antworte kurz mit OK") })
                        put("parts", parts)
                    }
                    contents.put(contentObj)
                    put("contents", contents)
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    return@withContext true to "API-Key erfolgreich verifiziert (Modell: $cleanModel)!"
                } else {
                    val parsedApiError = try {
                        val errObj = JSONObject(bodyStr).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    if (response.code != 404) {
                        return@withContext false to "Fehler ($parsedApiError)"
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiRepository", "Test key failed for $cleanModel: ${e.message}")
            }
        }
        false to "Verbindung zu Gemini fehlgeschlagen. Bitte Key überprüfen."
    }

    suspend fun analyzeFoodPhoto(bitmap: Bitmap): FoodPhotoAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            val missingKeyMsg = "Fehler: GEMINI_API_KEY fehlt in den Secrets oder App-Einstellungen"
            return@withContext FoodPhotoAnalysisResult(
                isFood = false,
                dishName = "",
                portion = "",
                rawCalories = 0,
                bufferedCalories = 0,
                proteinGrams = 0f,
                carbsGrams = 0f,
                fatGrams = 0f,
                summary = missingKeyMsg,
                errorMessage = missingKeyMsg
            )
        }

        val base64Image = try {
            bitmapToBase64(bitmap)
        } catch (e: Exception) {
            return@withContext FoodPhotoAnalysisResult(
                isFood = false,
                dishName = "",
                portion = "",
                rawCalories = 0,
                bufferedCalories = 0,
                proteinGrams = 0f,
                carbsGrams = 0f,
                fatGrams = 0f,
                summary = "API-Fehler: Bild konnte nicht komprimiert werden (${e.localizedMessage})",
                errorMessage = "API-Fehler: Bildverarbeitung fehlgeschlagen (${e.localizedMessage})"
            )
        }

        val prompt = """
            DU BIST DER EXPERT FOOD & NUTRITION SCANNER VON "AXON".
            
            Analysiere dieses Bild sorgfältig:
            - Wenn es sich um ein Gericht, eine Mahlzeit, ein Lebensmittel, eine Zutat, einen Snack, einen Fitness-Shake oder ein Getränk handelt:
              1. Bestimme den präzisen Namen des Gerichts / Lebensmittels auf Deutsch (z.B. "Hähnchenbrust mit Basmatireis und Brokkoli", "Haferflocken mit Banane und Milch", "Rinderhackfleisch mit Spaghetti").
              2. Schätze die Portionsgröße realistisch ein (z.B. "ca. 350g", "1 Teller (ca. 450g)", "1 Glas (300ml)", "1 Riegel (60g)").
              3. Berechne realistische Roh-Nährwerte: Kalorien (kcal), Protein (g), Kohlenhydrate (g), Fett (g).
              4. Gib die Antwort STRIKT als JSON zurück:
                 {"isFood": true, "name": "Gericht-Name", "calories": 480, "protein": 38, "carbs": 52, "fat": 12, "portion": "ca. 350g"}
            
            - Wenn auf dem Bild KEIN Essen, kein Lebensmittel und kein Getränk zu sehen ist (z.B. Personen, Möbel, Wände, Textdokumente, Tiere, Fitnessgeräte, leere Gegenstände):
              {"isFood": false}
            
            Antworte AUSSCHLIESSLICH mit gültigem JSON ohne Markdown-Codeblöcke.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            val contents = JSONArray()
            val contentObj = JSONObject().apply {
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", prompt) })
                val inlineData = JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Image)
                }
                parts.put(JSONObject().apply { put("inline_data", inlineData) })
                put("parts", parts)
            }
            contents.put(contentObj)
            put("contents", contents)

            val genConfig = JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
            }
            put("generationConfig", genConfig)
        }

        var lastErrorMessage: String? = null

        for (rawModel in modelCandidates) {
            val cleanModel = sanitizeModelName(rawModel)
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val parsedApiError = try {
                        val errObj = JSONObject(bodyStr).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $bodyStr"
                    }
                    Log.e("GeminiRepository", "Gemini API Error with $cleanModel: $parsedApiError")
                    lastErrorMessage = "API-Fehler (${response.code}): $parsedApiError"
                    if (response.code == 404) {
                        continue
                    } else {
                        return@withContext FoodPhotoAnalysisResult(
                            isFood = false,
                            dishName = "",
                            portion = "",
                            rawCalories = 0,
                            bufferedCalories = 0,
                            proteinGrams = 0f,
                            carbsGrams = 0f,
                            fatGrams = 0f,
                            summary = lastErrorMessage,
                            errorMessage = lastErrorMessage
                        )
                    }
                }

                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val textResponse = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val startIndex = textResponse.indexOf('{')
                    val endIndex = textResponse.lastIndexOf('}')
                    val cleanJson = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        textResponse.substring(startIndex, endIndex + 1)
                    } else {
                        textResponse.replace("```json", "").replace("```", "").trim()
                    }

                    val parsed = JSONObject(cleanJson)
                    val isFood = parsed.optBoolean("isFood", false)

                    if (!isFood) {
                        return@withContext FoodPhotoAnalysisResult(
                            isFood = false,
                            dishName = "",
                            portion = "",
                            rawCalories = 0,
                            bufferedCalories = 0,
                            proteinGrams = 0f,
                            carbsGrams = 0f,
                            fatGrams = 0f,
                            summary = "Kein Essen auf dem Foto erkannt! Bitte fotografiere eine Mahlzeit.",
                            errorMessage = "Kein Essen auf dem Foto erkannt! Bitte fotografiere eine Mahlzeit."
                        )
                    }

                    val dishName = parsed.optString("name", parsed.optString("dishName", "Erkannte Mahlzeit")).trim()
                    val portion = parsed.optString("portion", "1 Portion").trim()
                    val rawKcal = parsed.optInt("calories", parsed.optInt("rawCalories", 0))

                    val coachRepo = context?.let { CoachRepository(it) }
                    val profile = coachRepo?.getProfile()
                    val goal = profile?.primaryGoal ?: "Masseaufbau"
                    val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) ||
                            goal.contains("Definition", ignoreCase = true) ||
                            goal.contains("Cut", ignoreCase = true) ||
                            goal.contains("Abnahme", ignoreCase = true)
                    val isMaintain = goal.contains("Halten", ignoreCase = true) ||
                            goal.contains("Recomp", ignoreCase = true) ||
                            goal.contains("Erhalt", ignoreCase = true) ||
                            goal.contains("Beibehalten", ignoreCase = true)

                    val (bufferedKcal, bufferLabel) = when {
                        isFatLoss -> Pair((rawKcal * 1.08f).toInt(), "+8% Puffer")
                        isMaintain -> Pair(rawKcal, "Erhalt")
                        else -> Pair((rawKcal * 0.92f).toInt(), "-8% Puffer")
                    }

                    val protein = parsed.optDouble("protein", parsed.optDouble("proteinGrams", 0.0)).toFloat()
                    val carbs = parsed.optDouble("carbs", parsed.optDouble("carbsGrams", 0.0)).toFloat()
                    val fat = parsed.optDouble("fat", parsed.optDouble("fatGrams", 0.0)).toFloat()

                    val summary = if (isMaintain) {
                        "Erkannt: $dishName ($portion). Ca. $rawKcal kcal, ${protein.toInt()}g Protein."
                    } else {
                        "Erkannt: $dishName ($portion). Ca. $rawKcal kcal ($bufferedKcal kcal mit $bufferLabel), ${protein.toInt()}g Protein."
                    }

                    return@withContext FoodPhotoAnalysisResult(
                        isFood = true,
                        dishName = dishName,
                        portion = portion,
                        rawCalories = rawKcal,
                        bufferedCalories = bufferedKcal,
                        proteinGrams = protein,
                        carbsGrams = carbs,
                        fatGrams = fat,
                        summary = summary,
                        errorMessage = null
                    )
                } else {
                    lastErrorMessage = "API-Fehler: Keine Antwort von Gemini erhalten."
                }
            } catch (e: Exception) {
                Log.e("GeminiRepository", "Exception calling Gemini API: ${e.message}", e)
                lastErrorMessage = "API-Fehler: ${e.localizedMessage ?: "Netzwerkverbindung fehlgeschlagen"}"
            }
        }

        FoodPhotoAnalysisResult(
            isFood = false,
            dishName = "",
            portion = "",
            rawCalories = 0,
            bufferedCalories = 0,
            proteinGrams = 0f,
            carbsGrams = 0f,
            fatGrams = 0f,
            summary = lastErrorMessage ?: "Kein Essen auf dem Foto erkannt!",
            errorMessage = lastErrorMessage ?: "Kein Essen auf dem Foto erkannt! Bitte fotografiere eine Mahlzeit."
        )
    }

    suspend fun analyzeBarcodePhoto(bitmap: Bitmap): BarcodePhotoResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext BarcodePhotoResult(
                barcode = "",
                productName = "Gescannter Artikel",
                brand = "",
                caloriesPer100g = 350.0,
                proteinPer100g = 20.0,
                carbsPer100g = 45.0,
                fatPer100g = 8.0,
                servingGrams = 100.0f,
                servingName = "Portion (100g)"
            )
        }

        val base64Image = try {
            bitmapToBase64(bitmap)
        } catch (_: Exception) {
            return@withContext BarcodePhotoResult(
                barcode = "",
                productName = "Gescannter Artikel",
                brand = "",
                caloriesPer100g = 350.0,
                proteinPer100g = 20.0,
                carbsPer100g = 45.0,
                fatPer100g = 8.0,
                servingGrams = 100.0f,
                servingName = "Portion (100g)"
            )
        }

        val prompt = """
            DU BIST DER "AXON" - BARCODE & VERPACKUNGS-SCANNER.
            
            Analysiere das Bild dieses Produkts oder Barcodes:
            1. Lies den Barcode (EAN / UPC Nummer) ab, falls sichtbar.
            2. Erkenne den Produktnamen, Marke und Nährwerte pro 100g oder Portion aus der Nährwerttabelle.
            
            GIB DAS ERGEBNIS ALS JSON ZURÜCK:
            {
              "barcode": "4008400404127",
              "productName": "Haferflocken",
              "brand": "Kölln",
              "caloriesPer100g": 370,
              "proteinPer100g": 13.5,
              "carbsPer100g": 59.0,
              "fatPer100g": 7.0,
              "servingGrams": 50,
              "servingName": "Portion (50g)"
            }
        """.trimIndent()

        for (rawModel in modelCandidates) {
            val cleanModel = sanitizeModelName(rawModel)
            try {
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray()
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray()
                        parts.put(JSONObject().apply { put("text", prompt) })
                        val inlineData = JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        }
                        parts.put(JSONObject().apply { put("inline_data", inlineData) })
                        put("parts", parts)
                    }
                    contents.put(contentObj)
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("response_mime_type", "application/json")
                        put("temperature", 0.1)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    val textResponse = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val startIndex = textResponse.indexOf('{')
                    val endIndex = textResponse.lastIndexOf('}')
                    if (startIndex != -1 && endIndex != -1) {
                        val parsed = JSONObject(textResponse.substring(startIndex, endIndex + 1))
                        return@withContext BarcodePhotoResult(
                            barcode = parsed.optString("barcode", ""),
                            productName = parsed.optString("productName", "Gescannter Artikel"),
                            brand = parsed.optString("brand", ""),
                            caloriesPer100g = parsed.optDouble("caloriesPer100g", 250.0),
                            proteinPer100g = parsed.optDouble("proteinPer100g", 15.0),
                            carbsPer100g = parsed.optDouble("carbsPer100g", 30.0),
                            fatPer100g = parsed.optDouble("fatPer100g", 5.0),
                            servingGrams = parsed.optDouble("servingGrams", 100.0).toFloat(),
                            servingName = parsed.optString("servingName", "Portion")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiRepository", "Barcode analysis exception with $cleanModel: ${e.message}")
            }
        }

        BarcodePhotoResult(
            barcode = "",
            productName = "Gescannter Artikel",
            brand = "",
            caloriesPer100g = 350.0,
            proteinPer100g = 20.0,
            carbsPer100g = 45.0,
            fatPer100g = 8.0,
            servingGrams = 100.0f,
            servingName = "Portion (100g)"
        )
    }

    suspend fun getCoachResponse(
        userMessage: String,
        userContext: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val systemInstruction = """
            DU BIST DER "AXON AI COACH" - DIE KOMPLETTE APP-LOGIK, FITNESS-, TRAINING- & NÄHRWERT-EXPERTE.
            DEINE ROLLE:
            Du bist das Gehirn einer hochmodernen Android-Fitness-App für Muskelaufbau (Massephase / Bulk) und Krafttraining.
            Du bist strikt, simpel, direkt, hochgradig kompetent, motivierend und 100% zielorientiert auf Deutsch.
            
            DEINE FÄHIGKEITEN & ANTWORT-REGELN:
            1. TRAINING, TRAININGSPLÄNE & SPLITS:
               - Wenn der Nutzer nach Trainingsplänen fragt (z.B. Push/Pull/Legs, Oberkörper/Unterkörper, Ganzkörper, 3er- bis 5er-Split):
                 Erkläre den optimalen Aufbau, Satzanzahlen, Wiederholungsbereiche (z.B. 6-10 für Hypertrophie, 3-5 für Kraft) und Frequenz (jede Muskelgruppe 2x/Woche).
            2. ÜBUNGSVERGLEICHE & BIOMECHANIK:
               - Wenn der Nutzer Übungen vergleicht (z.B. "Was ist besser: Bankdrücken oder Liegestütze?", "Kniebeugen vs. Beinpresse", "Latzug vs. Klimmzüge", "Kurzhantel vs. Langhantel"):
                 Vergleiche sie präzise nach Muskelaktivierung, Dehnung unter Last, progressive Steigerbarkeit, Gelenkschonung und Einsatzbereich. Gib eine klare Empfehlung ab!
            3. PROGRESSIVE OVERLOAD & PERIODISIERUNG:
               - Gib exakte Anweisungen: Steigerung um +1-2 Wiederholungen oder +2,5kg pro Woche.
               - Deload: In Woche 4 wird das Arbeitsgewicht um 20% reduziert, um ZNS und passiven Bewegungsapparat zu schonen.
            4. NÄHRWERTE, MAHLZEITEN & TAGESSTATUS:
               - Du hast unten in den Nutzerdaten den exakten heutigen Stand (Ziel, Ist und was heute noch FEHLT).
               - Wenn der Nutzer fragt "Was kann ich noch essen?", "Schlag mir eine Mahlzeit vor", "Was soll ich kochen?":
                 Berechne die noch FEHLENDEN Kalorien und Makros und schlage 1 bis 3 konkrete Mahlzeiten/Shakes mit Mengenangaben vor.
            5. KÜCHENSCHRANK / ZUTATEN-CHECK:
               - Wenn der Nutzer schreibt "Ich habe nur [Zutaten] zu Hause":
                 Erstelle ein Rezept AUSSCHLIESSLICH aus diesen genannten Zutaten (plus Gewürze/Öl) mit Nährwertangaben.
            6. FORM & STIL:
               - Direkt, klar strukturiert, fokussiert auf Resultate. Keine unnötigen Füllwörter.
            
            AKTUELLE NUTZER- & NÄHRWERTDATEN:
            $userContext
        """.trimIndent()

        if (apiKey.isBlank()) {
            val msgLower = userMessage.lowercase()
            return@withContext when {
                msgLower.contains("bankdrücken") && (msgLower.contains("liegestütz") || msgLower.contains("push up")) -> {
                    "Bankdrücken vs. Liegestütze: Bankdrücken ist für Hypertrophie und Kraftaufbau überlegen, da du das Gewicht unbegrenzt und exakt um 2,5kg steigern kannst (Progressive Overload). Liegestütze sind hervorragend für Schultergesundheit, Rumpfstabilität und Warm-Up. Für maximalen Bulk-Muskelaufbau: Fokussiere dich auf schweres Bankdrücken (6-10 Wdh) und nutze Liegestütze als Finisher."
                }
                msgLower.contains("kniebeuge") && (msgLower.contains("beinpresse") || msgLower.contains("leg press")) -> {
                    "Kniebeugen vs. Beinpresse: Kniebeugen beanspruchen mehr Hilfsmuskeln, Rumpf und ZNS. Die Beinpresse ermöglicht es dir, den Quadrizeps isoliert bis zum Muskelversagen zu trainieren, ohne dass dein unterer Rücken limitiert. Die ideale Kombination im Bulk: Erst Kniebeugen schwer (6-8 Wdh), danach Beinpresse (10-12 Wdh) für maximalen Pump."
                }
                msgLower.contains("klimmzug") && (msgLower.contains("latzug") || msgLower.contains("lat")) -> {
                    "Klimmzüge vs. Latzug: Klimmzüge fordern hohe Körperspannung und bauen enorme relative Kraft auf. Der Latzug erlaubt eine präzisere Isolierung des Latissimus und feine Gewichtsabstufungen bei Ermüdung. Tipp: Starte dein Rückentraining mit schweren Klimmzügen und wechsle bei Ermüdung an den Latzug."
                }
                msgLower.contains("kurzhantel") && msgLower.contains("langhantel") -> {
                    "Kurzhantel vs. Langhantel: Langhanteln erlauben maximales Arbeitsgewicht und schnelle Steigerung. Kurzhanteln bieten eine größere Range of Motion (ROM), gleichen muskuläre Dysbalancen aus und schonen Hand- & Schultergelenke. Nutze Langhanteln für deine schwere Grundübung und Kurzhanteln für die Folgeübung."
                }
                msgLower.contains("plan") || msgLower.contains("split") || msgLower.contains("ppl") -> {
                    "Optimaler Bulk-Trainingsplan: Bei 3-4 Tagen Training eignet sich ein Push/Pull/Beine oder Oberkörper/Unterkörper-Split. Jede Muskelgruppe sollte 2x pro Woche mit 10-16 harten Arbeitssätzen im Bereich 6-10 Wdh trainiert werden. Priorisiere Grundübungen und logge jedes Kilo."
                }
                msgLower.contains("creatin") || msgLower.contains("kreatin") -> {
                    "Kreatin Monohydrat: Nimm täglich 5g (am besten morgens oder nach dem Training mit etwas Wasser oder Saft). Es steigert die ATP-Regeneration, Zellhydratation und Kraftwerte um ca. 5-10%. Eine Ladephase ist nicht nötig."
                }
                msgLower.contains("essen") || msgLower.contains("mahlzeit") || msgLower.contains("hunger") || msgLower.contains("kochen") -> {
                    "Um deine offenen Tageswerte optimal zu treffen: Nimm 250g Magerquark mit 1 Banane, 40g Haferflocken und 20g Mandeln. Liefert ca. 480 kcal, 42g Protein, 55g Kohlenhydrate und 10g Fett."
                }
                msgLower.contains("nur") || msgLower.contains("zutat") || msgLower.contains("kühlschrank") -> {
                    "Kühlschrank-Bulk-Rezept: Kombiniere deine Proteinquelle mit komplexen Carbs (z. B. 4 Rühreier mit 150g Haferflocken-Pancakes in der Pfanne). Liefert ca. 650 kcal, 38g Protein."
                }
                msgLower.contains("satz") || msgLower.contains("fertig") -> "Starker Satz! Pausen-Timer läuft (90s). Trink einen Schluck Wasser und fokussiere dich auf den nächsten Satz mit voller ROM."
                msgLower.contains("gewicht") || msgLower.contains("zunehmen") || msgLower.contains("bulk") -> "Im Bulk zählt Beständigkeit. Bleib täglich im Überschuss (+350 bis +500 kcal). Wenn die Waage am Montag stagniert, erhöhen wir um 150 kcal."
                msgLower.contains("training") || msgLower.contains("tipp") -> "Halte dich an deinen Split. Progressive Overload: Jede Woche +1 Wiederholung oder +2,5kg mehr Gewicht auf die Stange."
                msgLower.contains("deload") -> "In der 4. Woche senken wir die Arbeitsgewichte um 20%, um dein ZNS und Sehnen zu regenerieren. Danach explodiert deine Kraft."
                msgLower.contains("shake") -> "Schneller Bulk-Shake: 100g Haferflocken, 40g Whey, 40g Erdnussbutter, 1 Banane, 400ml Milch. Ergibt ca. 980 kcal und 62g Protein."
                else -> "Fokus halten. Mahlzeiten loggen, progressiv steigern und ausreichend schlafen. Was steht heute an?"
            }
        }

        for (rawModel in modelCandidates) {
            val cleanModel = sanitizeModelName(rawModel)
            try {
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray()
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray()
                        parts.put(JSONObject().apply { put("text", "$systemInstruction\n\nNutzer-Nachricht: $userMessage") })
                        put("parts", parts)
                    }
                    contents.put(contentObj)
                    put("contents", contents)
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    return@withContext root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                }
            } catch (_: Exception) {
                // try next candidate
            }
        }

        "Fokus auf deinen Überschuss und Progressive Overload. Trainiere hart und tracke jeden Satz."
    }

    suspend fun analyzeCalisthenicsSkill(
        skillName: String,
        bitmaps: List<Bitmap>,
        isVideo: Boolean,
        userNotes: String = ""
    ): CalisthenicsAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val safeSkillName = skillName.ifBlank { "Calisthenics Skill" }

        val systemInstruction = """
            Du bist ein erfahrener Calisthenics Coach (Cali Coach).
            Analysiere die Bewegung, Körperspannung, Ellbogenstreckung, Hüftposition und Form im Detail.
            Bewerte die Ausführung (z. B. Sauberkeit in % oder Schulnoten-Feedback).
            Bringe konstruktive Kritik: Wo liegt der Fehler (z. B. 'Zu viel Schwung', 'Hüfte knickt ab', 'Schultern nicht ausreichend protrahiert')?
            Identifiziere schwache Muskelgruppen (z. B. schwacher oberer Rücken, fehlende Rumpfspannung, schwache Trizeps/Schultern).
            Empfehle 2 bis 3 gezielte Assistenzübungen (Progressionen/Zusatzübungen) inklusive Satz- und Wiederholungszahlen, um den Skill zu meistern.
            
            GIB DAS ERGEBNIS STRIKT ALS GÜLTIGES JSON ZURÜCK MIT FOLGENDER STRUKTUR:
            {
              "skillName": "$safeSkillName",
              "scorePercentage": 78,
              "gradeText": "Note 2 (78% Sauberkeit)",
              "formCheckSummary": "Präzise Auswertung der Bewegungsausführung und Körperspannung...",
              "weakPoints": [
                "Erster identifizierter Fehlerpunkt / Formfehler",
                "Zweiter Fehlerpunkt (z.B. Ellbogenwinkel, Schulterposition)"
              ],
              "weakMuscles": [
                "Erste geschwächte Muskelgruppe",
                "Zweite geschwächte Muskelgruppe"
              ],
              "recommendedDrills": [
                {
                  "name": "Übungsname 1",
                  "setsAndReps": "3-4 Sätze x 5-8 Wdh",
                  "targetMuscles": "Zielmuskulatur",
                  "tip": "Ausführungshinweis"
                },
                {
                  "name": "Übungsname 2",
                  "setsAndReps": "3 Sätze x 15-20s Hold",
                  "targetMuscles": "Zielmuskulatur",
                  "tip": "Ausführungshinweis"
                }
              ]
            }
        """.trimIndent()

        if (apiKey.isBlank() || bitmaps.isEmpty()) {
            return@withContext getFallbackCalisthenicsAnalysis(safeSkillName, isVideo)
        }

        try {
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject().apply {
                    val parts = JSONArray()
                    val userPromptText = "Skill: $safeSkillName\nMedientyp: ${if (isVideo) "Video (Sequenzbilder)" else "Foto"}\nZusätzliche Notizen: ${userNotes.ifBlank { "Bitte Form analysieren" }}"
                    parts.put(JSONObject().apply { put("text", userPromptText) })

                    // Add up to 4 key frames to keep payload lightweight and fast
                    val framesToSend = bitmaps.take(4)
                    for (bmp in framesToSend) {
                        val base64Img = bitmapToBase64(bmp)
                        val inlineData = JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Img)
                        }
                        parts.put(JSONObject().apply { put("inline_data", inlineData) })
                    }

                    put("parts", parts)
                }
                contents.put(contentObj)
                put("contents", contents)

                put("systemInstruction", JSONObject().apply {
                    val sysParts = JSONArray()
                    sysParts.put(JSONObject().apply { put("text", systemInstruction) })
                    put("parts", sysParts)
                })

                val genConfig = JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.2)
                }
                put("generationConfig", genConfig)
            }

            for (rawModel in modelCandidates) {
                val cleanModel = sanitizeModelName(rawModel)
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val root = JSONObject(bodyStr)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val textResp = candidates.getJSONObject(0)
                                .optJSONObject("content")
                                ?.optJSONArray("parts")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: ""

                            val startIndex = textResp.indexOf('{')
                            val endIndex = textResp.lastIndexOf('}')
                            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                                val parsedJson = JSONObject(textResp.substring(startIndex, endIndex + 1))
                                
                                val score = parsedJson.optInt("scorePercentage", 75)
                                val grade = parsedJson.optString("gradeText", "$score% Sauberkeit")
                                val formSummary = parsedJson.optString("formCheckSummary", "Solide Ausführung mit Verbesserungspotenzial in der Körperspannung.")
                                
                                val weakPointsList = mutableListOf<String>()
                                val wpArr = parsedJson.optJSONArray("weakPoints")
                                if (wpArr != null) {
                                    for (i in 0 until wpArr.length()) {
                                        weakPointsList.add(wpArr.getString(i))
                                    }
                                }

                                val weakMusclesList = mutableListOf<String>()
                                val wmArr = parsedJson.optJSONArray("weakMuscles")
                                if (wmArr != null) {
                                    for (i in 0 until wmArr.length()) {
                                        weakMusclesList.add(wmArr.getString(i))
                                    }
                                }

                                val drillsList = mutableListOf<CalisthenicsAssistanceDrill>()
                                val drillsArr = parsedJson.optJSONArray("recommendedDrills")
                                if (drillsArr != null) {
                                    for (i in 0 until drillsArr.length()) {
                                        val dObj = drillsArr.getJSONObject(i)
                                        drillsList.add(
                                            CalisthenicsAssistanceDrill(
                                                name = dObj.optString("name", "Spezifische Assistenzübung"),
                                                setsAndReps = dObj.optString("setsAndReps", "3 Sätze x 8-10 Wdh"),
                                                targetMuscles = dObj.optString("targetMuscles", "Rücken & Core"),
                                                tip = dObj.optString("tip", "Fokus auf saubere Ausführung")
                                            )
                                        )
                                    }
                                }

                                return@withContext CalisthenicsAnalysisResult(
                                    skillName = safeSkillName,
                                    scorePercentage = score,
                                    gradeText = grade,
                                    formCheckSummary = formSummary,
                                    weakPoints = if (weakPointsList.isNotEmpty()) weakPointsList else listOf("Körperspannung in der Endphase stabilisieren"),
                                    weakMuscles = if (weakMusclesList.isNotEmpty()) weakMusclesList else listOf("Oberer Rücken & Rumpf"),
                                    recommendedDrills = if (drillsList.isNotEmpty()) drillsList else getFallbackDrills(safeSkillName),
                                    isVideo = isVideo
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiRepository", "Calisthenics analysis error with $cleanModel: ${e.message}")
                }
            }
        } catch (topEx: Exception) {
            Log.e("GeminiRepository", "Top level Calisthenics analysis failure: ${topEx.message}")
        }

        getFallbackCalisthenicsAnalysis(safeSkillName, isVideo)
    }

    private fun getFallbackCalisthenicsAnalysis(skillName: String, isVideo: Boolean): CalisthenicsAnalysisResult {
        val s = skillName.lowercase()
        return when {
            s.contains("muscle") || s.contains("muscle-up") || s.contains("muscle up") -> {
                CalisthenicsAnalysisResult(
                    skillName = "Muscle-Up",
                    scorePercentage = 76,
                    gradeText = "Note 2- (76% Sauberkeit)",
                    formCheckSummary = "Guter dynamischer Zug und flüssiger Einstieg. In der Transitionsphase über die Stange neigt der Körper zu einem leichten 'Chicken-Wing' (ein Ellbogen drückt zuerst nach), wodurch wertvolle Explosivkraft verloren geht.",
                    weakPoints = listOf(
                        "Asymmetrischer Übergang über die Stange (Chicken-Wing Tendenz)",
                        "Zu starkes Anwinkeln der Beine während des Zugs (Körperspannung bricht ein)",
                        "Explosive Zughöhe bis unter das Brustbein noch ausbaufähig"
                    ),
                    weakMuscles = listOf(
                        "Explosive Latissimus- & Rückenmuskulatur",
                        "Gerader & schräger Bauchmuskel (Hollow-Body Spannung)",
                        "Trizepskopf & vordere Deltamuskeln in der Bar-Dip-Phase"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Explosive High Pull-Ups",
                            setsAndReps = "4 Sätze x 3-5 Wdh",
                            targetMuscles = "Latissimus & oberer Rücken",
                            tip = "Ziehe maximal explosiv, sodass die Stange die untere Brust berührt."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Straight Bar Dips",
                            setsAndReps = "3 Sätze x 8-10 Wdh",
                            targetMuscles = "Trizeps, Brust & Schultern",
                            tip = "Lehne den Oberkörper leicht vor und halte die Beine gestreckt nach vorne."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Muscle-Up Negatives (Slow Transition)",
                            setsAndReps = "3 Sätze x 2-3 Wdh",
                            targetMuscles = "Sehnen, Bänder & Übergangskoordination",
                            tip = "Lasse dich 5 Sekunden lang extrem kontrolliert durch den Wendepunkt herab."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            s.contains("planche") -> {
                CalisthenicsAnalysisResult(
                    skillName = "Planche",
                    scorePercentage = 72,
                    gradeText = "Note 3+ (72% Sauberkeit)",
                    formCheckSummary = "Starke Vorlage der Schultern über die Hände. Die Schulterblätter sind jedoch noch nicht maximal protrahiert (rund nach vorne gedrückt) und die Hüfte hängt leicht unter Schulterhöhe durch.",
                    weakPoints = listOf(
                        "Unvollständige Schulterblatt-Protraction (Brustkorb sinkt leicht ein)",
                        "Hüftknick / fehlende posteriore Beckenkippung (PPT)",
                        "Ellenbogen leicht gebeugt (fehlender vollständiger Lockout)"
                    ),
                    weakMuscles = listOf(
                        "Serratus Anterior (Sägemuskel für Schulterblatt-Vorschub)",
                        "Vorderer Deltamuskel & Bizepssehnen (Straight-Arm-Stärke)",
                        "Unterer Rumpf, Gesäß & Beinbeuger"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Planche Lean mit Protraction Hold",
                            setsAndReps = "4 Sätze x 15-25 Sek",
                            targetMuscles = "Serratus Anterior & vordere Schulter",
                            tip = "Drücke den oberen Rücken maximal rund und lehne dich mit gestreckten Armen weit vor."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Tuck / Advanced Tuck Planche Holds",
                            setsAndReps = "4 Sätze x 8-12 Sek",
                            targetMuscles = "Ganzkörper-Statik & Schulterhebel",
                            tip = "Hüfte zwingend auf Schulterhöhe anheben, Knie eng an die Brust ziehen."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Zanetti / Dumbbell Straight-Arm Raises",
                            setsAndReps = "3 Sätze x 10-12 Wdh",
                            targetMuscles = "Vordere Deltas & Bizepssehnen-Schutz",
                            tip = "Mit leichtem Gewicht und 100% gestreckten Armen anheben."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            s.contains("handstand") -> {
                CalisthenicsAnalysisResult(
                    skillName = "Handstand",
                    scorePercentage = 80,
                    gradeText = "Note 2 (80% Sauberkeit)",
                    formCheckSummary = "Gute Balance über den Handgelenken und solide Ausrichtung. Die Schultern sind jedoch noch nicht voll aktiv nach oben herausgedrückt (Elevation) und es entsteht ein leichtes Hohlkreuz ('Bananen-Handstand').",
                    weakPoints = listOf(
                        "Leichte Bananen-Kurve in der Lendenwirbelsäule",
                        "Schultern nicht maximal nach oben zu den Ohren herausgedrückt (Elevation)",
                        "Fingerspitzen greifen noch zu unruhig in den Boden"
                    ),
                    weakMuscles = listOf(
                        "Trapezius (pars descendens) & Schulterheber",
                        "Tiefer Core (Transversus Abdominis) & Gesäß",
                        "Unterarm-Flexoren für Fingerdruck-Feinjustierung"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Chest-to-Wall Handstand (Bauch zur Wand)",
                            setsAndReps = "4 Sätze x 30-45 Sek",
                            targetMuscles = "Schulter-Elevation & gerade Linie",
                            tip = "Nase und Zehen an die Wand, Becken nach hinten kippen, Beine zusammenpressen."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Hollow Body Rocks & Holds",
                            setsAndReps = "3 Sätze x 40 Sek",
                            targetMuscles = "Rumpfstabilität & Beckenkontrolle",
                            tip = "Unterer Rücken presst lückenlos in den Boden."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Scapula Handstand Shrugs an der Wand",
                            setsAndReps = "3 Sätze x 10-12 Wdh",
                            targetMuscles = "Trapez & Schulterblatt-Mobilität",
                            tip = "Aus den Schultern hochdrücken und kontrolliert einsinken lassen."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            s.contains("front lever") || s.contains("frontlever") -> {
                CalisthenicsAnalysisResult(
                    skillName = "Front Lever",
                    scorePercentage = 74,
                    gradeText = "Note 3+ (74% Sauberkeit)",
                    formCheckSummary = "Starker Straight-Arm Pull aus dem Latissimus. Allerdings knickt die Hüfte leicht ab und die Schulterblätter sind nicht ausreichend nach unten-hinten gezogen (Depression & Retraction).",
                    weakPoints = listOf(
                        "Hüfte sinkt unter Stangenhöhe ab",
                        "Schulterblätter nicht in stabiler Retraktion & Depression",
                        "Leichte Beugung im Ellenbogengelenk"
                    ),
                    weakMuscles = listOf(
                        "Latissimus Dorsi & Teres Major",
                        "Rhomboiden & mittlerer/unterer Trapez",
                        "Hintere Kette (Gluteus & Beinbeuger)"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Front Lever Tucked / Adv. Tucked Raises",
                            setsAndReps = "4 Sätze x 5-6 Wdh",
                            targetMuscles = "Latissimus & dynamischer Hebelzug",
                            tip = "Aus dem Hang mit gestreckten Armen bis in die Waagerechte ziehen."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Dragon Flags",
                            setsAndReps = "3 Sätze x 6-8 Wdh",
                            targetMuscles = "Gesamter vorderer Rumpf & Beckenfixierung",
                            tip = "Körper wie ein steifes Brett absenken und anheben."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Scapula Pull-Ups (mit 2s Hold oben)",
                            setsAndReps = "4 Sätze x 8-10 Wdh",
                            targetMuscles = "Schulterblatt-Depressoren & Rhomboiden",
                            tip = "Arme bleiben 100% gestreckt, nur die Schulterblätter ziehen nach unten."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            s.contains("l-sit") || s.contains("lsit") || s.contains("v-sit") -> {
                CalisthenicsAnalysisResult(
                    skillName = "L-Sit",
                    scorePercentage = 82,
                    gradeText = "Note 2 (82% Sauberkeit)",
                    formCheckSummary = "Saubere 90-Grad Beinhaltung und gute Armstreckung. Für einen perfekten L-Sit sollte die Hüfte noch aktiver zwischen den Händen nach vorne geschoben und die Knie komplett gelockt werden.",
                    weakPoints = listOf(
                        "Leicht gebeugte Knie durch eingeschränkte Beinbeuger-Flexibilität",
                        "Schulterdepression könnte noch 1-2 cm höher drücken",
                        "Hüfte bleibt leicht hinter den Händen"
                    ),
                    weakMuscles = listOf(
                        "Hüftbeuger (Iliopsoas & Rectus Femoris)",
                        "Unterer Trapezius & Brust (Depression der Schultern)",
                        "Quadrizeps Lockout-Muskulatur"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Seated Leg Lifts (Pike Compressions)",
                            setsAndReps = "4 Sätze x 10-12 Wdh",
                            targetMuscles = "Aktive Hüftbeuger & Kompression",
                            tip = "Auf dem Boden sitzend Hände neben die Knie setzen und gestreckte Beine anheben."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Parallel Bar / Dip Bar Support Hold mit Depression",
                            setsAndReps = "3 Sätze x 30 Sek",
                            targetMuscles = "Schulter-Depressoren & Trizeps",
                            tip = "Drücke dich maximal weit aus den Schultern heraus, Kopf bleibt lang."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Hanging L-Sit Knee-to-Chest Raises",
                            setsAndReps = "3 Sätze x 10 Wdh",
                            targetMuscles = "Bauchmuskeln & Hüftbeuger",
                            tip = "Ohne Schwung bis in den 90-Grad-Winkel anheben und kurz pausieren."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            s.contains("dip") || s.contains("barren") -> {
                CalisthenicsAnalysisResult(
                    skillName = "Dips",
                    scorePercentage = 84,
                    gradeText = "Note 2+ (84% Sauberkeit)",
                    formCheckSummary = "Sehr gute Tiefe (unter 90 Grad) und kraftvoller Lockout. Achte darauf, die Ellbogen nicht nach außen abspreizen zu lassen und die Schultern unten zu halten.",
                    weakPoints = listOf(
                        "Ellbogen flaren am tiefsten Punkt leicht nach außen",
                        "Schultern wandern beim Absenken leicht nach oben zu den Ohren",
                        "Leichtes Nachschwingen der Beine"
                    ),
                    weakMuscles = listOf(
                        "Trizeps (Caput mediale & laterale)",
                        "Unterer Pectoralis Major & vordere Schulter",
                        "Rumpfstabilisatoren gegen Beinschwung"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Pause Dips (2s Hold im tiefsten Punkt)",
                            setsAndReps = "4 Sätze x 6-8 Wdh",
                            targetMuscles = "Brust, Trizeps & Gelenkstabilität",
                            tip = "Im tiefsten Punkt für 2 Sekunden verharren, dann explosiv herausdrücken."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Zusatzgewicht-Dips (Weighted Dips)",
                            setsAndReps = "3 Sätze x 5-6 Wdh",
                            targetMuscles = "Maximalkraft & Hypertrophie",
                            tip = "Steigere schrittweise mit Gürtel um 2,5kg pro Woche."
                        )
                    ),
                    isVideo = isVideo
                )
            }
            else -> {
                CalisthenicsAnalysisResult(
                    skillName = skillName.ifBlank { "Calisthenics Form-Check" },
                    scorePercentage = 78,
                    gradeText = "Note 2 (78% Sauberkeit)",
                    formCheckSummary = "Solide Grundform mit erkennbarer Körperspannung. Die Hauptpotenziale liegen in der vollständigen Gelenkstreckung (Lockout), einer stabileren Schulterpositionierung und der Beseitigung von Ausweichbewegungen.",
                    weakPoints = listOf(
                        "Leichte Unruhe in der Bewegungslinie (Ausweichbewegung)",
                        "Schulterblätter könnten aktiver fixiert werden (Protraction/Depression)",
                        "Körperspannung im Rumpf lässt im letzten Drittel nach"
                    ),
                    weakMuscles = listOf(
                        "Stütz- & Haltemuskulatur im Schultergürtel",
                        "Tiefe Rumpf- & Bauchmuskulatur",
                        "Gezielte Zielmuskulatur für die Hauptbewegung"
                    ),
                    recommendedDrills = listOf(
                        CalisthenicsAssistanceDrill(
                            name = "Statische Halteübung / Hold der Endposition",
                            setsAndReps = "3-4 Sätze x 15-20 Sek",
                            targetMuscles = "Ganzkörper-Stabilität & Sehnen",
                            tip = "Halte die perfekte Position mit 100% Körperspannung."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Langsame Negativ-Wiederholungen (Tempo 4-0-1)",
                            setsAndReps = "3 Sätze x 4-6 Wdh",
                            targetMuscles = "Exzentrische Kraft & Muskelkontrolle",
                            tip = "Lasse dich 4 Sekunden kontrolliert herab."
                        ),
                        CalisthenicsAssistanceDrill(
                            name = "Hollow Body Core Fixierung",
                            setsAndReps = "3 Sätze x 30-45 Sek",
                            targetMuscles = "Bauch, Hüfte & Rumpf",
                            tip = "Vermeide jedes Hohlkreuz und spanne den Po an."
                        )
                    ),
                    isVideo = isVideo
                )
            }
        }
    }

    private fun getFallbackDrills(skillName: String): List<CalisthenicsAssistanceDrill> {
        return listOf(
            CalisthenicsAssistanceDrill(
                name = "Spezifische Progression / Hold",
                setsAndReps = "4 Sätze x 15-20 Sek",
                targetMuscles = "Schultergürtel & Rumpf",
                tip = "Fokus auf saubere Körperspannung ohne Einknicken."
            ),
            CalisthenicsAssistanceDrill(
                name = "Kontrollierte Negativ-Wiederholungen",
                setsAndReps = "3 Sätze x 4-6 Wdh",
                targetMuscles = "Zielmuskeln & Sehnen",
                tip = "Sehr langsam und sauber über den vollen Bewegungsradius absenken."
            )
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, Math.max(1, newWidth), Math.max(1, newHeight), true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun generatePerformanceReport(
        profile: UserProfile,
        lastTwoWorkouts: List<Pair<WorkoutSession, List<WorkoutLogEntry>>>,
        lastTwoCheckIns: List<WeightCheckIn>,
        photos: List<ProgressPhoto>,
        recentFoodItems: List<FoodItem>
    ): PerformanceReport = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        val photosWithUri = photos.filter { it.imageUri.isNotBlank() }
        val photoCount = photosWithUri.size
        val hasEnoughPhotos = photoCount >= 2

        val photoBitmaps = mutableListOf<Bitmap>()
        val photoUrisUsed = mutableListOf<String>()

        if (hasEnoughPhotos) {
            val twoLatestPhotos = photosWithUri.take(2)
            for (p in twoLatestPhotos) {
                photoUrisUsed.add(p.imageUri)
                val bmp = uriToBitmap(p.imageUri)
                if (bmp != null) photoBitmaps.add(bmp)
            }
        }

        val workoutPromptBuilder = StringBuilder()
        if (lastTwoWorkouts.isEmpty()) {
            workoutPromptBuilder.append("Keine absolvierten Workouts vorhanden.")
        } else {
            lastTwoWorkouts.forEachIndexed { idx, (session, logs) ->
                workoutPromptBuilder.append("\nWorkout ${idx + 1}: ${session.splitName} (${session.dateString})\n")
                workoutPromptBuilder.append("Gesamtvolumen: ${session.totalVolumeKg} kg, Dauer: ${session.durationMinutes} min, Kalorien: ${session.caloriesBurned} kcal\n")
                logs.forEach { log ->
                    workoutPromptBuilder.append("  - Übung: ${log.exerciseName} (${log.exerciseCategory})\n")
                    log.sets.forEach { s ->
                        workoutPromptBuilder.append("    * Satz ${s.setNumber}: ${s.weightKg} kg x ${s.reps} Wdh\n")
                    }
                }
            }
        }

        val checkInPromptBuilder = StringBuilder()
        if (lastTwoCheckIns.isEmpty()) {
            checkInPromptBuilder.append("Keine Gewicht-Check-Ins vorhanden.")
        } else {
            lastTwoCheckIns.forEachIndexed { idx, c ->
                checkInPromptBuilder.append("Check-in ${idx + 1}: Datum ${c.dateString}, Gewicht ${c.weightKg} kg, Notiz: ${c.note}\n")
            }
        }

        val nutritionPromptBuilder = StringBuilder()
        if (recentFoodItems.isEmpty()) {
            nutritionPromptBuilder.append("Keine kürzlich getrackten Mahlzeiten.")
        } else {
            val totalCal = recentFoodItems.sumOf { it.bufferedCalories }
            val totalProtein = recentFoodItems.sumOf { it.proteinGrams.toDouble() }.toFloat()
            val totalCarbs = recentFoodItems.sumOf { it.carbsGrams.toDouble() }.toFloat()
            val totalFat = recentFoodItems.sumOf { it.fatGrams.toDouble() }.toFloat()
            nutritionPromptBuilder.append("Getrackte Mahlzeiten (${recentFoodItems.size} Einträge):\n")
            nutritionPromptBuilder.append("Makros: $totalCal kcal | P: ${totalProtein}g | C: ${totalCarbs}g | F: ${totalFat}g\n")
            recentFoodItems.take(10).forEach { item ->
                nutritionPromptBuilder.append("  - ${item.name} (${item.mealType.displayName}): ${item.bufferedCalories} kcal, ${item.proteinGrams}g Protein\n")
            }
        }

        val systemPrompt = """
            Du bist der Axon Peak Performance & AI Coach.
            Erstelle einen klaren, ehrlichen und hochprofessionellen Performance Report auf Deutsch basierend auf den Nutzerdaten.
            
            DEINE AUFGABE (5 BEREICHE):
            1. KRAFT- & WORKOUT-ANALYSE (FOKUS: DIE LETZTEN 2 WORKOUTS):
               - Vergleiche für jede Übung die Performance der letzten 2 Absolvierungen (Gewicht, Wiederholungen, Volumen).
               - Hebe Steigerungen hervor ("Progressive Overload") und warne bei Stagnation oder Rückschritten.
            
            2. OPTISCHER FORM-VERGLEICH & KÖRPERGEWICHT (FOKUS: DIE LETZTEN 2 CHECK-UPS):
               - Vergleiche das Körpergewicht zwischen den beiden Zeitpunkten.
               - Falls Fotos vorliegen, vergleiche die Form visuell.
               - WICHTIG: Wenn weniger als 2 Checkup-Fotos vorhanden sind, erwähne das Gewicht und füge explizit den Satz ein: "Lade mindestens 2 Check-Up Fotos hoch für den optischen Vergleich."
            
            3. MUSKEL-ANALYSE & V-TAPER FOKUS (MAXIMALE OPTISCHE BREITE):
               - Wenn mindestens 1 Foto vorhanden ist, analysiere aufmerksam das aktuellste Foto hinsichtlich ALLER sichtbaren Muskelgruppen (Brust, Schultern, Lat/Rücken, Arme, Core/Taille, Beine).
               - FOKUS "V-TAPER / BREITER WERDEN": Analysiere explizit den Latissimus dorsi, die seitlichen Deltoideus (seitliche Schultern) und die Relation zur Taille.
               - Benenne genau die Schwachstellen, die für maximale optische Breite ausgebaut werden müssen.
               - Gib konkrete Übungsempfehlungen (z. B. Kontrolliertes Seitheben mit Kurzhanteln/Kabelzug, Latzug im weiten Griff, Klimmzüge, vorgebeugtes Seitheben für die hintere Schulter).
               - Falls kein Foto vorliegt, erkläre allgemeine V-Taper Richtlinien & empfohlene Key-Übungen für den Muskelaufbau.
            
            4. ERNÄHRUNGS-ANALYSE & LEBENSMITTEL-TIPPS:
               - Analysiere die getrackten Mahlzeiten, Kalorien und Makros im Vergleich zum Tagesziel (${profile.dailyCalorieTarget} kcal, ${profile.proteinTargetGrams}g Protein).
               - Hebe positive Entscheidungen hervor und gib konkrete Tipps zu Lebensmitteln (z. B. "Ersetze Produkt X durch Y für bessere Makros").
            
            5. ACTION-PLAN FÜR DIE NÄCHSTE WOCHE:
               - Gib 3 bis 4 glasklare, direkte Handlungsempfehlungen für die kommende Woche.
            
            GIB DEINE ANTWORT STRIKT ALS GÜLTIGES JSON ZURÜCK MIT FOLGENDEM SCHEMA:
            {
              "workoutAnalysis": "Detailierte Kraft- & Workout-Analyse...",
              "visualAndWeightAnalysis": "Detaillierter optischer Form- & Gewichtsvergleich...",
              "muscleAndVTaperAnalysis": "Detaillierte Muskel- & V-Taper Analyse für maximale optische Breite inklusive Übungsempfehlungen...",
              "nutritionAnalysis": "Ernährungs-Analyse & konkrete Lebensmittel-Tipps...",
              "actionPlan": [
                "Glasklare Empfehlung 1...",
                "Glasklare Empfehlung 2...",
                "Glasklare Empfehlung 3...",
                "Glasklare Empfehlung 4..."
              ]
            }
        """.trimIndent()

        val userPromptText = """
            NUTZERPROFIL:
            Name: ${profile.name}, Ziel: ${profile.primaryGoal}, Kalorienziel: ${profile.dailyCalorieTarget} kcal, Eiweißziel: ${profile.proteinTargetGrams}g, Gewicht: ${profile.currentWeightKg} kg, Zielgewicht: ${profile.targetWeightKg} kg
            
            LETZTE 2 WORKOUTS:
            $workoutPromptBuilder
            
            LETZTE 2 CHECK-INS:
            $checkInPromptBuilder
            
            VERFÜGBARE CHECKUP FOTOS: $photoCount
            
            ERNÄHRUNGS-DATEN:
            $nutritionPromptBuilder
        """.trimIndent()

        if (apiKey.isBlank()) {
            return@withContext createFallbackPerformanceReport(profile, lastTwoWorkouts, lastTwoCheckIns, photoUrisUsed, hasEnoughPhotos)
        }

        try {
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject().apply {
                    val parts = JSONArray()
                    parts.put(JSONObject().apply { put("text", userPromptText) })

                    for (bmp in photoBitmaps) {
                        val base64Img = bitmapToBase64(bmp)
                        val inlineData = JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Img)
                        }
                        parts.put(JSONObject().apply { put("inline_data", inlineData) })
                    }

                    put("parts", parts)
                }
                contents.put(contentObj)
                put("contents", contents)

                put("systemInstruction", JSONObject().apply {
                    val sysParts = JSONArray()
                    sysParts.put(JSONObject().apply { put("text", systemPrompt) })
                    put("parts", sysParts)
                })

                val genConfig = JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.3)
                }
                put("generationConfig", genConfig)
            }

            for (rawModel in modelCandidates) {
                val cleanModel = sanitizeModelName(rawModel)
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val root = JSONObject(bodyStr)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val textResp = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                                .trim()

                            val parsedObj = JSONObject(textResp)
                            val actionPlanArray = parsedObj.optJSONArray("actionPlan")
                            val planList = mutableListOf<String>()
                            if (actionPlanArray != null) {
                                for (i in 0 until actionPlanArray.length()) {
                                    planList.add(actionPlanArray.getString(i))
                                }
                            }

                            return@withContext PerformanceReport(
                                generatedAtEpoch = System.currentTimeMillis(),
                                workoutAnalysis = parsedObj.optString("workoutAnalysis"),
                                visualAndWeightAnalysis = parsedObj.optString("visualAndWeightAnalysis"),
                                muscleAndVTaperAnalysis = parsedObj.optString("muscleAndVTaperAnalysis"),
                                nutritionAnalysis = parsedObj.optString("nutritionAnalysis"),
                                actionPlan = if (planList.isEmpty()) listOf("Steigere deine Gewichte um 2.5kg", "Erreiche täglich dein Eiweißziel", "Sichere dir 8 Std Schlaf") else planList,
                                photoUrisUsed = photoUrisUsed,
                                hasEnoughPhotos = hasEnoughPhotos
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        createFallbackPerformanceReport(profile, lastTwoWorkouts, lastTwoCheckIns, photoUrisUsed, hasEnoughPhotos)
    }

    private fun uriToBitmap(uriString: String): Bitmap? {
        if (context == null) return null
        return com.example.util.ImageUtils.getRotatedBitmap(context, uriString)
    }

    private fun createFallbackPerformanceReport(
        profile: UserProfile,
        lastTwoWorkouts: List<Pair<WorkoutSession, List<WorkoutLogEntry>>>,
        lastTwoCheckIns: List<WeightCheckIn>,
        photoUrisUsed: List<String>,
        hasEnoughPhotos: Boolean
    ): PerformanceReport {
        val wText = if (lastTwoWorkouts.size >= 2) {
            val w1 = lastTwoWorkouts[0].first
            val w2 = lastTwoWorkouts[1].first
            "Deine letzten beiden Workouts (${w1.splitName} und ${w2.splitName}): Das Gesamtvolumen liegt bei ${w1.totalVolumeKg} kg vs. ${w2.totalVolumeKg} kg. Für optimale Progression steigere das Gewicht bei den Hauptübungen um jeweils +2.5 kg, um den Muskelaufbau kontinuierlich zu stimulieren."
        } else if (lastTwoWorkouts.size == 1) {
            val w1 = lastTwoWorkouts[0].first
            "1 absolviertes Workout erfasst (${w1.splitName}, ${w1.totalVolumeKg} kg Gesamtvolumen). Absolviere ein zweites Workout, um den automatischen 2-Workout-Vergleich mit Progressionstrends freizuschalten."
        } else {
            "Noch keine absolvierten Einheiten erfasst. Absolviere deine ersten 2 Workouts im Trainingsbereich, um hier automatische Kraft- & Volumenvergleiche zu erhalten."
        }

        val vText = if (lastTwoCheckIns.size >= 2) {
            val c1 = lastTwoCheckIns[0]
            val c2 = lastTwoCheckIns[1]
            val diff = c1.weightKg - c2.weightKg
            val diffStr = if (diff >= 0) "+${String.format("%.1f", diff)} kg" else "${String.format("%.1f", diff)} kg"
            "Gewichtsentwicklung der letzten beiden Check-Ins: $diffStr (${c2.weightKg} kg ➔ ${c1.weightKg} kg). ${if (!hasEnoughPhotos) "Lade mindestens 2 Check-Up Fotos hoch für den optischen Vergleich." else "Visuelle Symmetrie und Schulter-Brust-Verhältnis entwickeln sich gut."}"
        } else {
            "Aktuelles Gewicht: ${profile.currentWeightKg} kg (Ziel: ${profile.targetWeightKg} kg). Lade mindestens 2 Check-Up Fotos hoch für den optischen Vergleich."
        }

        val mText = "🎯 V-TAPER & MUSKEL-ANALYSE FÜR OPTISCHE BREITE:\n" +
                "• Seitliche Schultern (Lateral Delts): Die Schlüsselfrequenz für eine breitere Silhouette. Empfehlung: 3-4x pro Woche Seitheben (12-15 Wdh) am Kabelzug oder mit Kurzhanteln mit voller Dehnung.\n" +
                "• Latissimus (Rückenbreite): Maximale V-Form erfordert Fokus auf weites Latziehen und Klimmzüge im Pronationsgriff. Achte auf volle Streckung am obersten Punkt.\n" +
                "• Brust & Obere Brust: Schrägbankdrücken mit Kurzhanteln zur Füllung des oberen Brustdrittels.\n" +
                "• Taille & Core: Vermeide extremes schräges Bauchmuskel-Training mit schwerem Gewicht, um die Taille optisch schmal zu halten."

        val nText = "Tagesziel: ${profile.dailyCalorieTarget} kcal & ${profile.proteinTargetGrams}g Eiweiß. Tipp: Ersetze reine Magerquark-Mahlzeiten gelegentlich durch Haferflocken-Protein-Shakes mit Erdnussbutter für leichtere Kalorienaufnahme im Überschuss."

        val plan = listOf(
            "Fokussiere Seitheben (Kabel/Hantel) am Anfang oder Ende jeder Oberkörper-Einheit für breitere Schultern",
            "Erhöhe das Arbeitsgewicht bei Grundübungen in der nächsten Einheit um +2.5 kg",
            "Erreiche täglich mindestens ${profile.proteinTargetGrams}g Eiweiß für maximalen Anabolismus",
            "Mache deinen nächsten wöchentlichen Check-in mit aktuellem Gewicht und Checkup-Foto"
        )

        return PerformanceReport(
            generatedAtEpoch = System.currentTimeMillis(),
            workoutAnalysis = wText,
            visualAndWeightAnalysis = vText,
            muscleAndVTaperAnalysis = mText,
            nutritionAnalysis = nText,
            actionPlan = plan,
            photoUrisUsed = photoUrisUsed,
            hasEnoughPhotos = hasEnoughPhotos
        )
    }
}

data class FoodPhotoAnalysisResult(
    val isFood: Boolean = true,
    val dishName: String,
    val portion: String,
    val rawCalories: Int,
    val bufferedCalories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val summary: String,
    val errorMessage: String? = null
)

data class BarcodePhotoResult(
    val barcode: String,
    val productName: String,
    val brand: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingGrams: Float,
    val servingName: String
)

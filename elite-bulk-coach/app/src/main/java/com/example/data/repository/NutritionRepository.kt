package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FoodItemEntity
import com.example.data.model.FoodItem
import com.example.data.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NutritionRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.coachDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getFoodsForDateFlow(date: String): Flow<List<FoodItem>> {
        return dao.getFoodsForDateFlow(date).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun getFoodsForDate(date: String): List<FoodItem> = withContext(Dispatchers.IO) {
        dao.getFoodsForDate(date).map { it.toModel() }
    }

    suspend fun getAllFoods(): List<FoodItem> = withContext(Dispatchers.IO) {
        dao.getAllFoods().map { it.toModel() }
    }

    fun getFoodsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<FoodItem>> {
        return dao.getFoodsBetweenDatesFlow(startDate, endDate).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun addFoodItem(
        name: String,
        portionDescription: String,
        rawCalories: Int,
        proteinGrams: Float,
        carbsGrams: Float,
        fatGrams: Float,
        mealType: MealType,
        dateString: String,
        isAiEstimated: Boolean = false,
        barcode: String? = null
    ): FoodItem = withContext(Dispatchers.IO) {
        // Apply 8% safety buffer ONLY for AI photo recognition (isAiEstimated = true).
        // If goal is Abnahme (Fat Loss), +8% calories recorded to protect deficit.
        // If goal is Zunahme / Masseaufbau (Bulk), -8% calories recorded to guarantee surplus.
        // If goal is Erhalt (Maintain), exact calories.
        val bufferedCalories = if (isAiEstimated) {
            val coachRepo = CoachRepository(context)
            val profile = coachRepo.getProfile()
            val goal = profile?.primaryGoal ?: "Masseaufbau"
            val isFatLoss = goal.contains("Fettabbau", ignoreCase = true) ||
                    goal.contains("Definition", ignoreCase = true) ||
                    goal.contains("Cut", ignoreCase = true) ||
                    goal.contains("Abnahme", ignoreCase = true)
            val isMaintain = goal.contains("Halten", ignoreCase = true) ||
                    goal.contains("Recomp", ignoreCase = true) ||
                    goal.contains("Erhalt", ignoreCase = true) ||
                    goal.contains("Beibehalten", ignoreCase = true)

            when {
                isFatLoss -> (rawCalories * 1.08f).toInt()
                isMaintain -> rawCalories
                else -> (rawCalories * 0.92f).toInt()
            }
        } else {
            rawCalories
        }

        val item = FoodItem(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            portionDescription = portionDescription,
            rawCalories = rawCalories,
            bufferedCalories = bufferedCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            mealType = mealType,
            dateString = dateString,
            isAiEstimated = isAiEstimated,
            barcode = barcode,
            timestamp = System.currentTimeMillis()
        )

        dao.insertFood(item.toEntity())
        CoachRepository(context).saveFullCloudVaultBackup()
        item
    }

    suspend fun updateFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        dao.updateFood(item.toEntity())
        CoachRepository(context).saveFullCloudVaultBackup()
    }

    suspend fun deleteFoodItem(id: String) = withContext(Dispatchers.IO) {
        dao.deleteFood(id)
        CoachRepository(context).saveFullCloudVaultBackup()
    }

    suspend fun deleteFoodsForDate(date: String) = withContext(Dispatchers.IO) {
        dao.deleteFoodsForDate(date)
        CoachRepository(context).saveFullCloudVaultBackup()
    }

    // Predefined comprehensive bodybuilding, everyday staples & bakery foods in German
    fun getCommonBulkFoods(): List<FoodItemPreset> {
        return listOf(
            FoodItemPreset("Haferflocken mit Vollmilch", "150g Haferflocken + 300ml Milch", 720, 26f, 102f, 21f, MealType.BREAKFAST),
            FoodItemPreset("Magerquark mit Banane & Honig", "250g Magerquark + 1 Banane + 15g Honig", 360, 32f, 54f, 1f, MealType.SNACK_SHAKE),
            FoodItemPreset("Hähnchenbrust mit Reis & Brokkoli", "200g Hähnchen + 120g Reis + 150g Brokkoli", 640, 52f, 92f, 6f, MealType.LUNCH),
            FoodItemPreset("Rinderhackfleisch mit Vollkornnudeln", "180g Rinderhack + 100g Nudeln + Tomatensauce", 780, 48f, 75f, 32f, MealType.DINNER),
            FoodItemPreset("Laugenbrezel / Bayerische Brezel", "1 Stück (ca. 85g)", 245, 7.5f, 48.0f, 2.5f, MealType.SNACK_SHAKE),
            FoodItemPreset("Kaiserbrötchen / Weizenbrötchen", "1 Stück (ca. 60g)", 165, 5.5f, 32.0f, 1.0f, MealType.BREAKFAST),
            FoodItemPreset("Vollkornbrötchen mit Körnern", "1 Stück (ca. 75g)", 210, 8.0f, 36.0f, 3.5f, MealType.BREAKFAST),
            FoodItemPreset("Buttercroissant", "1 Stück (ca. 60g)", 280, 5.0f, 28.0f, 16.0f, MealType.BREAKFAST),
            FoodItemPreset("Franzbrötchen (Zimt)", "1 Stück (ca. 90g)", 390, 5.5f, 48.0f, 19.0f, MealType.SNACK_SHAKE),
            FoodItemPreset("Elite Bulk Shake (Monster Shake)", "100g Hafer, 40g Whey, 40g Erdnussbutter, 400ml Milch, 1 Banane", 980, 62f, 115f, 32f, MealType.SNACK_SHAKE),
            FoodItemPreset("Rührei mit Vollkornbrot & Avocado", "4 Eier + 2 Scheiben Brot + 50g Avocado", 580, 32f, 38f, 32f, MealType.BREAKFAST),
            FoodItemPreset("Lachsfilet mit Süßkartoffeln", "200g Lachs + 300g Süßkartoffel", 690, 42f, 65f, 26f, MealType.DINNER),
            FoodItemPreset("Thunfisch-Wrap mit Frischkäse", "1 Dose Thunfisch + 2 Tortilla Wraps + 50g Frischkäse", 520, 44f, 52f, 14f, MealType.LUNCH),
            FoodItemPreset("Whey Protein Shake", "35g Whey Isolat + 300ml Wasser/Milch", 180, 28f, 3f, 2f, MealType.SNACK_SHAKE),
            FoodItemPreset("Erdnussbutter-Brot", "2 Scheiben Vollkornbrot + 40g Erdnussbutter", 420, 16f, 40f, 22f, MealType.SNACK_SHAKE)
        )
    }

    // Extended built-in local catalog with German Bakery & Grocery staples
    private val localFoodCatalog: List<FoodSearchResultItem> by lazy {
        listOf(
            // Bäckerei & Backwaren
            FoodSearchResultItem(name = "Laugenbrezel", brand = "Bäckerei Frisch", caloriesPer100g = 290.0, proteinPer100g = 8.8, carbsPer100g = 56.5, fatPer100g = 2.9, servingSizeDescription = "1 Stück (85g)", pieceWeightGrams = 85f, pieceUnitName = "Brezel", hasPieceServing = true),
            FoodSearchResultItem(name = "Laugenstange", brand = "Bäckerei Frisch", caloriesPer100g = 295.0, proteinPer100g = 9.0, carbsPer100g = 57.0, fatPer100g = 3.1, servingSizeDescription = "1 Stück (80g)", pieceWeightGrams = 80f, pieceUnitName = "Stange", hasPieceServing = true),
            FoodSearchResultItem(name = "Kaiserbrötchen / Schnittbrötchen", brand = "Bäckerei Frisch", caloriesPer100g = 275.0, proteinPer100g = 9.2, carbsPer100g = 53.5, fatPer100g = 1.6, servingSizeDescription = "1 Stück (60g)", pieceWeightGrams = 60f, pieceUnitName = "Brötchen", hasPieceServing = true),
            FoodSearchResultItem(name = "Vollkornbrötchen mit Körnern", brand = "Bäckerei Frisch", caloriesPer100g = 280.0, proteinPer100g = 10.7, carbsPer100g = 48.0, fatPer100g = 4.7, servingSizeDescription = "1 Stück (75g)", pieceWeightGrams = 75f, pieceUnitName = "Brötchen", hasPieceServing = true),
            FoodSearchResultItem(name = "Dinkelbrötchen", brand = "Bäckerei Frisch", caloriesPer100g = 270.0, proteinPer100g = 11.5, carbsPer100g = 50.0, fatPer100g = 2.2, servingSizeDescription = "1 Stück (70g)", pieceWeightGrams = 70f, pieceUnitName = "Brötchen", hasPieceServing = true),
            FoodSearchResultItem(name = "Buttercroissant", brand = "Bäckerei Frisch", caloriesPer100g = 465.0, proteinPer100g = 8.3, carbsPer100g = 46.5, fatPer100g = 27.0, servingSizeDescription = "1 Stück (60g)", pieceWeightGrams = 60f, pieceUnitName = "Croissant", hasPieceServing = true),
            FoodSearchResultItem(name = "Franzbrötchen (Zimt & Butter)", brand = "Bäckerei Hamburg", caloriesPer100g = 430.0, proteinPer100g = 6.1, carbsPer100g = 53.0, fatPer100g = 21.0, servingSizeDescription = "1 Stück (90g)", pieceWeightGrams = 90f, pieceUnitName = "Franzbrötchen", hasPieceServing = true),
            FoodSearchResultItem(name = "Schokobrötchen", brand = "Bäckerei Frisch", caloriesPer100g = 390.0, proteinPer100g = 7.5, carbsPer100g = 52.0, fatPer100g = 16.0, servingSizeDescription = "1 Stück (70g)", pieceWeightGrams = 70f, pieceUnitName = "Brötchen", hasPieceServing = true),
            FoodSearchResultItem(name = "Vollkornbrot Saftig", brand = "Lieken Urkorn", caloriesPer100g = 210.0, proteinPer100g = 8.0, carbsPer100g = 38.0, fatPer100g = 1.5, servingSizeDescription = "1 Scheibe (45g)", pieceWeightGrams = 45f, pieceUnitName = "Scheibe", hasPieceServing = true),
            FoodSearchResultItem(name = "Eiweißbrot Protein Bread", brand = "Mestemacher / Harry", caloriesPer100g = 265.0, proteinPer100g = 22.0, carbsPer100g = 10.5, fatPer100g = 13.5, servingSizeDescription = "1 Scheibe (50g)", pieceWeightGrams = 50f, pieceUnitName = "Scheibe", hasPieceServing = true),
            FoodSearchResultItem(name = "Dinkel-Vollkornbrot", brand = "Bäckerei", caloriesPer100g = 225.0, proteinPer100g = 9.5, carbsPer100g = 41.0, fatPer100g = 2.0, servingSizeDescription = "1 Scheibe (50g)", pieceWeightGrams = 50f, pieceUnitName = "Scheibe", hasPieceServing = true),
            FoodSearchResultItem(name = "Sandwich Toast Weizen", brand = "Golden Toast", caloriesPer100g = 260.0, proteinPer100g = 8.0, carbsPer100g = 48.0, fatPer100g = 3.5, servingSizeDescription = "1 Scheibe (37g)", pieceWeightGrams = 37f, pieceUnitName = "Scheibe", hasPieceServing = true),
            FoodSearchResultItem(name = "Vollkorn Sandwich Toast", brand = "Golden Toast", caloriesPer100g = 245.0, proteinPer100g = 9.0, carbsPer100g = 43.0, fatPer100g = 3.0, servingSizeDescription = "1 Scheibe (37g)", pieceWeightGrams = 37f, pieceUnitName = "Scheibe", hasPieceServing = true),
            FoodSearchResultItem(name = "Tortilla Wrap Weizen", brand = "Fajita / Poco Loco", caloriesPer100g = 310.0, proteinPer100g = 8.5, carbsPer100g = 54.0, fatPer100g = 6.0, servingSizeDescription = "1 Wrap (62g)", pieceWeightGrams = 62f, pieceUnitName = "Wrap", hasPieceServing = true),
            FoodSearchResultItem(name = "Pita Brot / Fladenbrot", brand = "Bäckerei", caloriesPer100g = 275.0, proteinPer100g = 9.0, carbsPer100g = 54.0, fatPer100g = 1.5, servingSizeDescription = "1 Tasche (80g)", pieceWeightGrams = 80f, pieceUnitName = "Pita", hasPieceServing = true),

            // Bulk Proteinquellen & Basics
            FoodSearchResultItem(name = "Zarte Haferflocken", brand = "Kölln / Edeka", caloriesPer100g = 370.0, proteinPer100g = 13.5, carbsPer100g = 59.0, fatPer100g = 7.0, servingSizeDescription = "Portion (50g)", pieceWeightGrams = 50f, pieceUnitName = "Portion", hasPieceServing = true),
            FoodSearchResultItem(name = "Speisequark Magerstufe", brand = "Milbona / Gut & Günstig", caloriesPer100g = 68.0, proteinPer100g = 12.0, carbsPer100g = 4.0, fatPer100g = 0.2, servingSizeDescription = "Becher (250g)", pieceWeightGrams = 250f, pieceUnitName = "Becher (250g)", hasPieceServing = true),
            FoodSearchResultItem(name = "Skyr Natur", brand = "Ísey / Arla", caloriesPer100g = 63.0, proteinPer100g = 11.0, carbsPer100g = 4.0, fatPer100g = 0.2, servingSizeDescription = "Becher (450g)", pieceWeightGrams = 450f, pieceUnitName = "Becher (450g)", hasPieceServing = true),
            FoodSearchResultItem(name = "Hähnchenbrustfilet Natur", brand = "Wiesenhof / Bio Geflügel", caloriesPer100g = 110.0, proteinPer100g = 23.5, carbsPer100g = 0.0, fatPer100g = 1.5, servingSizeDescription = "Filet (150g)", pieceWeightGrams = 150f, pieceUnitName = "Filet (150g)", hasPieceServing = true),
            FoodSearchResultItem(name = "Putenbrustfilet", brand = "Geflügel", caloriesPer100g = 107.0, proteinPer100g = 24.0, carbsPer100g = 0.0, fatPer100g = 1.2, servingSizeDescription = "Portion (150g)", pieceWeightGrams = 150f, pieceUnitName = "Portion", hasPieceServing = true),
            FoodSearchResultItem(name = "Rinderhackfleisch Mager (<5% Fett)", brand = "Metzgerei / Edeka", caloriesPer100g = 125.0, proteinPer100g = 21.5, carbsPer100g = 0.0, fatPer100g = 4.2, servingSizeDescription = "Portion (200g)", pieceWeightGrams = 200f, pieceUnitName = "Portion", hasPieceServing = true),
            FoodSearchResultItem(name = "Rumpsteak / Rindersteak", brand = "Rind", caloriesPer100g = 135.0, proteinPer100g = 22.0, carbsPer100g = 0.0, fatPer100g = 5.0, servingSizeDescription = "Steak (200g)", pieceWeightGrams = 200f, pieceUnitName = "Steak", hasPieceServing = true),
            FoodSearchResultItem(name = "Lachsfilet Frisch", brand = "Norwegen", caloriesPer100g = 208.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 14.0, servingSizeDescription = "Filet (150g)", pieceWeightGrams = 150f, pieceUnitName = "Filet", hasPieceServing = true),
            FoodSearchResultItem(name = "Thunfisch im eigenen Saft", brand = "Saupiquet / Edeka", caloriesPer100g = 105.0, proteinPer100g = 24.0, carbsPer100g = 0.0, fatPer100g = 0.8, servingSizeDescription = "Dose (150g)", pieceWeightGrams = 150f, pieceUnitName = "Dose", hasPieceServing = true),
            FoodSearchResultItem(name = "Bio Hühnerei (Größe M)", brand = "Frische Eier", caloriesPer100g = 140.0, proteinPer100g = 12.5, carbsPer100g = 0.7, fatPer100g = 9.8, servingSizeDescription = "1 Ei (55g)", pieceWeightGrams = 55f, pieceUnitName = "Ei", hasPieceServing = true),
            FoodSearchResultItem(name = "Designer Whey Protein Pulver", brand = "ESN", caloriesPer100g = 383.0, proteinPer100g = 78.0, carbsPer100g = 5.0, fatPer100g = 4.0, servingSizeDescription = "1 Scoop (30g)", pieceWeightGrams = 30f, pieceUnitName = "Scoop (30g)", hasPieceServing = true),
            FoodSearchResultItem(name = "Erdnussbutter 100% Peanuts", brand = "Bulk / Gym Nutrition", caloriesPer100g = 595.0, proteinPer100g = 25.0, carbsPer100g = 14.0, fatPer100g = 49.0, servingSizeDescription = "1 EL (20g)", pieceWeightGrams = 20f, pieceUnitName = "EL", hasPieceServing = true),
            FoodSearchResultItem(name = "Barebells Protein Riegel", brand = "Barebells", caloriesPer100g = 365.0, proteinPer100g = 36.0, carbsPer100g = 27.0, fatPer100g = 14.0, servingSizeDescription = "1 Riegel (55g)", pieceWeightGrams = 55f, pieceUnitName = "Riegel", hasPieceServing = true),
            FoodSearchResultItem(name = "Basmati Reis (ungekocht)", brand = "Oryza / Tilda", caloriesPer100g = 355.0, proteinPer100g = 8.5, carbsPer100g = 77.0, fatPer100g = 0.8, servingSizeDescription = "Portion (100g)", pieceWeightGrams = 100f, pieceUnitName = "Portion", hasPieceServing = true),
            FoodSearchResultItem(name = "Vollkorn Pasta / Nudeln", brand = "Barilla", caloriesPer100g = 350.0, proteinPer100g = 13.0, carbsPer100g = 65.0, fatPer100g = 2.5, servingSizeDescription = "Portion (100g)", pieceWeightGrams = 100f, pieceUnitName = "Portion", hasPieceServing = true),
            FoodSearchResultItem(name = "Frische Banane", brand = "Obst", caloriesPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 22.8, fatPer100g = 0.3, servingSizeDescription = "1 Stück (115g)", pieceWeightGrams = 115f, pieceUnitName = "Banane", hasPieceServing = true),
            FoodSearchResultItem(name = "Frischer Apfel", brand = "Obst", caloriesPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 13.8, fatPer100g = 0.2, servingSizeDescription = "1 Stück (150g)", pieceWeightGrams = 150f, pieceUnitName = "Apfel", hasPieceServing = true),
            FoodSearchResultItem(name = "Frische Vollmilch 3,8%", brand = "Weihenstephan", caloriesPer100g = 68.0, proteinPer100g = 3.4, carbsPer100g = 4.8, fatPer100g = 3.8, servingSizeDescription = "Glas (250ml)", pieceWeightGrams = 250f, pieceUnitName = "Glas (250ml)", hasPieceServing = true)
        )
    }

    // Free-Text Search combining Local Fuzzy Catalog & Open Food Facts Search API
    suspend fun searchFoods(query: String): List<FoodSearchResultItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext localFoodCatalog.take(15)
        }

        val results = mutableListOf<FoodSearchResultItem>()
        val seenNames = mutableSetOf<String>()

        // 1. Fuzzy Local Search (Instant offline + Typo resilient)
        val localMatches = localFoodCatalog
            .map { item ->
                val score = calculateFuzzyMatchScore(cleanQuery, item.name + " " + item.brand)
                Pair(item, score)
            }
            .filter { it.second > 0.35 }
            .sortedByDescending { it.second }
            .map { it.first }

        for (match in localMatches) {
            val key = normalizeForDedup(match.name)
            if (seenNames.add(key)) {
                results.add(match)
            }
        }

        // 2. Open Food Facts Live Search API (https://world.openfoodfacts.org/cgi/search.pl?search_terms=[QUERY]&search_simple=1&action=process&json=1)
        try {
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrls = listOf(
                "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=40",
                "https://de.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=25"
            )

            for (url in searchUrls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "EliteBulkCoach-Android/2.0 (bulkcoach@elitebulk.com)")
                        .header("Accept", "application/json")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotBlank()) {
                            val json = JSONObject(bodyStr)
                            val products = json.optJSONArray("products")
                            if (products != null && products.length() > 0) {
                                for (i in 0 until products.length()) {
                                    val prod = products.optJSONObject(i) ?: continue
                                    val name = prod.optString("product_name_de")
                                        .ifBlank { prod.optString("product_name") }
                                        .ifBlank { prod.optString("generic_name_de") }
                                        .ifBlank { prod.optString("product_name_en") }
                                        .ifBlank { prod.optString("generic_name", "") }

                                    if (name.isBlank() || name.length < 2) continue

                                    val brand = prod.optString("brands", prod.optString("brands_tags", ""))
                                    val nutriments = prod.optJSONObject("nutriments") ?: continue

                                    var energyKcal = nutriments.optDouble("energy-kcal_100g",
                                        nutriments.optDouble("energy-kcal",
                                            nutriments.optDouble("energy_100g", 0.0) / 4.184
                                        )
                                    ).takeIf { it > 0 } ?: 0.0

                                    if (energyKcal <= 0.0) {
                                        val kj = nutriments.optDouble("energy-kj_100g", nutriments.optDouble("energy-kj", 0.0))
                                        if (kj > 0) energyKcal = kj / 4.184
                                    }

                                    val protein = nutriments.optDouble("proteins_100g", nutriments.optDouble("proteins", 0.0))
                                    val carbs = nutriments.optDouble("carbohydrates_100g", nutriments.optDouble("carbohydrates", 0.0))
                                    val fat = nutriments.optDouble("fat_100g", nutriments.optDouble("fat", 0.0))

                                    if (energyKcal <= 0 && protein <= 0 && carbs <= 0 && fat <= 0) continue

                                    val servingSizeRaw = prod.optString("serving_size", "")
                                    val servingQuantity = prod.optDouble("serving_quantity", 0.0)
                                    val barcode = prod.optString("code", null)

                                    var pieceWeight = 100f
                                    var unitName = "Portion"
                                    var hasPiece = false

                                    if (servingQuantity > 0.0) {
                                        pieceWeight = servingQuantity.toFloat()
                                        hasPiece = true
                                        unitName = if (servingSizeRaw.isNotBlank()) servingSizeRaw else "Portion (${pieceWeight.toInt()}g)"
                                    } else if (servingSizeRaw.isNotBlank()) {
                                        val regex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:g|ml|gramm)""", RegexOption.IGNORE_CASE)
                                        val match = regex.find(servingSizeRaw)
                                        if (match != null) {
                                            val parsed = match.groupValues[1].replace(',', '.').toFloatOrNull()
                                            if (parsed != null && parsed > 0) {
                                                pieceWeight = parsed
                                                hasPiece = true
                                                unitName = servingSizeRaw
                                            }
                                        }
                                    }

                                    val searchItem = FoodSearchResultItem(
                                        name = name.trim(),
                                        brand = brand.trim(),
                                        caloriesPer100g = energyKcal,
                                        proteinPer100g = protein,
                                        carbsPer100g = carbs,
                                        fatPer100g = fat,
                                        servingSizeDescription = if (hasPiece) unitName else "100g",
                                        pieceWeightGrams = pieceWeight,
                                        pieceUnitName = unitName,
                                        hasPieceServing = hasPiece,
                                        isFromOpenFoodFacts = true,
                                        barcode = barcode
                                    )

                                    val dedupKey = normalizeForDedup(searchItem.name + searchItem.brand)
                                    if (seenNames.add(dedupKey)) {
                                        results.add(searchItem)
                                    }
                                }
                                break // Successfully retrieved from primary search endpoint
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Try next search endpoint
                }
            }
        } catch (_: Exception) {
            // Return whatever matches we got
        }

        results
    }

    private fun normalizeForDedup(text: String): String {
        return text.lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replace(Regex("[^a-z0-9]"), "")
    }

    // Fuzzy matching scoring algorithm with typo tolerance & substring weighting
    private fun calculateFuzzyMatchScore(query: String, target: String): Double {
        val qNorm = normalizeForDedup(query)
        val tNorm = normalizeForDedup(target)
        if (qNorm.isEmpty() || tNorm.isEmpty()) return 0.0

        // Exact match
        if (qNorm == tNorm) return 1.0

        // Direct containment
        if (tNorm.contains(qNorm)) {
            return 0.85 + (qNorm.length.toDouble() / tNorm.length.toDouble()) * 0.15
        }

        // Subsequence match (characters in order, e.g. "brezl" in "laugenbrezel")
        var qIdx = 0
        for (char in tNorm) {
            if (qIdx < qNorm.length && char == qNorm[qIdx]) {
                qIdx++
            }
        }
        if (qIdx == qNorm.length) {
            return 0.70 + (qNorm.length.toDouble() / tNorm.length.toDouble()) * 0.20
        }

        // Levenshtein similarity
        val maxLen = maxOf(qNorm.length, tNorm.length)
        val distance = levenshteinDistance(qNorm, tNorm.take(qNorm.length + 3))
        val similarity = 1.0 - (distance.toDouble() / maxLen.toDouble())
        return if (similarity > 0.5) similarity * 0.75 else 0.0
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    // Barcode Lookup via OpenFoodFacts or extended local database
    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult? = withContext(Dispatchers.IO) {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) return@withContext null

        // 1. Check local comprehensive preset database for instant offline resolution
        getLocalBarcodePreset(cleanCode)?.let { return@withContext it }

        // 2. Query OpenFoodFacts Multi-Endpoints (v2 World, v0 DE, v2 Search fallback)
        val endpoints = listOf(
            "https://world.openfoodfacts.org/api/v2/product/$cleanCode.json",
            "https://de.openfoodfacts.org/api/v0/product/$cleanCode.json",
            "https://world.openfoodfacts.net/api/v2/product/$cleanCode.json"
        )

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "EliteBulkCoach-Android/2.0 (bulkcoach@elitebulk.com)")
                    .header("Accept", "application/json")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val status = json.optInt("status", json.optInt("status_verbose", 0))
                        if ((status == 1 || json.optString("status_verbose") == "product found") && json.has("product")) {
                            val product = json.getJSONObject("product")
                            val name = product.optString("product_name_de")
                                .ifBlank { product.optString("product_name") }
                                .ifBlank { product.optString("generic_name_de") }
                                .ifBlank { product.optString("product_name_en") }
                                .ifBlank { product.optString("generic_name", "Gescannter Artikel") }
                            
                            val brand = product.optString("brands", product.optString("brands_tags", ""))
                            val nutriments = product.optJSONObject("nutriments")
                            
                            var energyKcal = nutriments?.optDouble("energy-kcal_100g",
                                nutriments.optDouble("energy-kcal",
                                    nutriments.optDouble("energy_100g", 0.0) / 4.184
                                )
                            )?.takeIf { it > 0 } ?: 0.0

                            if (energyKcal <= 0.0 && nutriments != null) {
                                val kj = nutriments.optDouble("energy-kj_100g", nutriments.optDouble("energy-kj", 0.0))
                                if (kj > 0) energyKcal = kj / 4.184
                            }

                            val protein = nutriments?.optDouble("proteins_100g", nutriments.optDouble("proteins", 0.0)) ?: 0.0
                            val carbs = nutriments?.optDouble("carbohydrates_100g", nutriments.optDouble("carbohydrates", 0.0)) ?: 0.0
                            val fat = nutriments?.optDouble("fat_100g", nutriments.optDouble("fat", 0.0)) ?: 0.0

                            // Parse serving info
                            val servingSizeRaw = product.optString("serving_size", "")
                            val servingQuantity = product.optDouble("serving_quantity", 0.0)
                            
                            var pieceWeight = 100f
                            var unitName = "Portion"
                            var hasPiece = false

                            if (servingQuantity > 0.0) {
                                pieceWeight = servingQuantity.toFloat()
                                hasPiece = true
                                unitName = if (servingSizeRaw.isNotBlank()) servingSizeRaw else "Portion (${pieceWeight.toInt()}g)"
                            } else if (servingSizeRaw.isNotBlank()) {
                                val regex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:g|ml|gramm)""", RegexOption.IGNORE_CASE)
                                val match = regex.find(servingSizeRaw)
                                if (match != null) {
                                    val parsedWeight = match.groupValues[1].replace(',', '.').toFloatOrNull()
                                    if (parsedWeight != null && parsedWeight > 0) {
                                        pieceWeight = parsedWeight
                                        hasPiece = true
                                        unitName = servingSizeRaw
                                    }
                                }
                            }

                            if (energyKcal > 0 || protein > 0 || carbs > 0 || fat > 0) {
                                return@withContext BarcodeLookupResult(
                                    productName = name,
                                    brand = brand,
                                    caloriesPer100g = energyKcal,
                                    proteinPer100g = protein,
                                    carbsPer100g = carbs,
                                    fatPer100g = fat,
                                    servingSizeDescription = if (hasPiece) unitName else "100g",
                                    pieceWeightGrams = pieceWeight,
                                    pieceUnitName = unitName,
                                    hasPieceServing = hasPiece
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }

        // 3. Fallback: Search Open Food Facts Search API with the barcode
        try {
            val searchResults = searchFoods(cleanCode)
            val bestMatch = searchResults.firstOrNull { it.barcode == cleanCode } ?: searchResults.firstOrNull()
            if (bestMatch != null && (bestMatch.caloriesPer100g > 0 || bestMatch.proteinPer100g > 0)) {
                return@withContext BarcodeLookupResult(
                    productName = bestMatch.name,
                    brand = bestMatch.brand,
                    caloriesPer100g = bestMatch.caloriesPer100g,
                    proteinPer100g = bestMatch.proteinPer100g,
                    carbsPer100g = bestMatch.carbsPer100g,
                    fatPer100g = bestMatch.fatPer100g,
                    servingSizeDescription = bestMatch.servingSizeDescription,
                    pieceWeightGrams = bestMatch.pieceWeightGrams,
                    pieceUnitName = bestMatch.pieceUnitName,
                    hasPieceServing = bestMatch.hasPieceServing
                )
            }
        } catch (_: Exception) {}

        null
    }

    private fun getLocalBarcodePreset(cleanCode: String): BarcodeLookupResult? {
        return when (cleanCode) {
            "4008400404127", "bulk1" -> BarcodeLookupResult(
                productName = "Zarte Haferflocken (Kölln)",
                brand = "Kölln",
                caloriesPer100g = 370.0,
                proteinPer100g = 13.5,
                carbsPer100g = 59.0,
                fatPer100g = 7.0,
                servingSizeDescription = "Portion (50g)",
                pieceWeightGrams = 50f,
                pieceUnitName = "Portion (50g)",
                hasPieceServing = true
            )
            "4014400900018", "bulk2" -> BarcodeLookupResult(
                productName = "Speisequark Magerstufe",
                brand = "Milbona / Gut & Günstig",
                caloriesPer100g = 68.0,
                proteinPer100g = 12.0,
                carbsPer100g = 4.0,
                fatPer100g = 0.2,
                servingSizeDescription = "Becher (250g)",
                pieceWeightGrams = 250f,
                pieceUnitName = "Becher (250g)",
                hasPieceServing = true
            )
            "4260389580012", "bulk3" -> BarcodeLookupResult(
                productName = "Designer Whey Protein (Vanille)",
                brand = "ESN",
                caloriesPer100g = 383.0,
                proteinPer100g = 78.0,
                carbsPer100g = 5.0,
                fatPer100g = 4.0,
                servingSizeDescription = "Messlöffel / Scoop (30g)",
                pieceWeightGrams = 30f,
                pieceUnitName = "Scoop (30g)",
                hasPieceServing = true
            )
            "4251877701234", "bulk4" -> BarcodeLookupResult(
                productName = "Barebells Protein Bar (Salty Peanut)",
                brand = "Barebells",
                caloriesPer100g = 365.0,
                proteinPer100g = 36.0,
                carbsPer100g = 27.0,
                fatPer100g = 14.0,
                servingSizeDescription = "Riegel (55g)",
                pieceWeightGrams = 55f,
                pieceUnitName = "Riegel (55g)",
                hasPieceServing = true
            )
            "4008400123456", "bulk5" -> BarcodeLookupResult(
                productName = "Vollkornbrot Saftig & Kraftvoll",
                brand = "Lieken Urkorn",
                caloriesPer100g = 210.0,
                proteinPer100g = 8.0,
                carbsPer100g = 38.0,
                fatPer100g = 1.5,
                servingSizeDescription = "Scheibe (45g)",
                pieceWeightGrams = 45f,
                pieceUnitName = "Scheibe (45g)",
                hasPieceServing = true
            )
            "4001234560000", "bulk6" -> BarcodeLookupResult(
                productName = "Bio Hühnerei (Klasse M)",
                brand = "Bio Ei",
                caloriesPer100g = 140.0,
                proteinPer100g = 12.5,
                carbsPer100g = 0.7,
                fatPer100g = 9.8,
                servingSizeDescription = "Ei / Stück (55g)",
                pieceWeightGrams = 55f,
                pieceUnitName = "Stück (55g)",
                hasPieceServing = true
            )
            "4000000000001", "bulk7" -> BarcodeLookupResult(
                productName = "Frische Banane",
                brand = "Bio Banane",
                caloriesPer100g = 89.0,
                proteinPer100g = 1.1,
                carbsPer100g = 22.8,
                fatPer100g = 0.3,
                servingSizeDescription = "Stück (ca. 115g)",
                pieceWeightGrams = 115f,
                pieceUnitName = "Stück (115g)",
                hasPieceServing = true
            )
            "4008400200019", "bulk8" -> BarcodeLookupResult(
                productName = "Leibniz Butterkeks",
                brand = "Bahlsen",
                caloriesPer100g = 435.0,
                proteinPer100g = 8.4,
                carbsPer100g = 75.0,
                fatPer100g = 11.0,
                servingSizeDescription = "Keks (8.5g)",
                pieceWeightGrams = 8.5f,
                pieceUnitName = "Keks (8.5g)",
                hasPieceServing = true
            )
            "4001234567890", "bulk9" -> BarcodeLookupResult(
                productName = "Erdnussbutter 100% Peanuts",
                brand = "Bulk / Gym Nutrition",
                caloriesPer100g = 595.0,
                proteinPer100g = 25.0,
                carbsPer100g = 14.0,
                fatPer100g = 49.0,
                servingSizeDescription = "Portion / EL (20g)",
                pieceWeightGrams = 20f,
                pieceUnitName = "Portion (20g)",
                hasPieceServing = true
            )
            "4008400404134" -> BarcodeLookupResult(
                productName = "Kölln Müsli Schoko & Hafer",
                brand = "Kölln",
                caloriesPer100g = 405.0,
                proteinPer100g = 10.0,
                carbsPer100g = 62.0,
                fatPer100g = 12.0,
                servingSizeDescription = "Portion (60g)",
                pieceWeightGrams = 60f,
                pieceUnitName = "Portion (60g)",
                hasPieceServing = true
            )
            "4061458000018" -> BarcodeLookupResult(
                productName = "Frische Vollmilch 3,8% Fett",
                brand = "Berchtesgadener Land / Weihenstephan",
                caloriesPer100g = 68.0,
                proteinPer100g = 3.4,
                carbsPer100g = 4.8,
                fatPer100g = 3.8,
                servingSizeDescription = "Glas (250ml)",
                pieceWeightGrams = 250f,
                pieceUnitName = "Glas (250ml)",
                hasPieceServing = true
            )
            "4000417025005" -> BarcodeLookupResult(
                productName = "Basmati Reis Premium",
                brand = "Oryza / Tilda",
                caloriesPer100g = 355.0,
                proteinPer100g = 8.5,
                carbsPer100g = 77.0,
                fatPer100g = 0.8,
                servingSizeDescription = "Portion (100g ungekocht)",
                pieceWeightGrams = 100f,
                pieceUnitName = "Portion (100g)",
                hasPieceServing = true
            )
            "4008400001239" -> BarcodeLookupResult(
                productName = "Hähnchenbrustfilet Natur",
                brand = "Wiesenhof / Bio Geflügel",
                caloriesPer100g = 110.0,
                proteinPer100g = 23.5,
                carbsPer100g = 0.0,
                fatPer100g = 1.5,
                servingSizeDescription = "Filet (150g)",
                pieceWeightGrams = 150f,
                pieceUnitName = "Filet (150g)",
                hasPieceServing = true
            )
            "4008400005678" -> BarcodeLookupResult(
                productName = "Skyr Natur Isländischer Art",
                brand = "Ísey Skyr / Arla",
                caloriesPer100g = 63.0,
                proteinPer100g = 11.0,
                carbsPer100g = 4.0,
                fatPer100g = 0.2,
                servingSizeDescription = "Becher (450g)",
                pieceWeightGrams = 450f,
                pieceUnitName = "Becher (450g)",
                hasPieceServing = true
            )
            "4008400998877" -> BarcodeLookupResult(
                productName = "Thunfisch Filets im eigenen Saft",
                brand = "Saupiquet / Edeka",
                caloriesPer100g = 105.0,
                proteinPer100g = 24.0,
                carbsPer100g = 0.0,
                fatPer100g = 0.8,
                servingSizeDescription = "Dose (150g Abtropfgewicht)",
                pieceWeightGrams = 150f,
                pieceUnitName = "Dose (150g)",
                hasPieceServing = true
            )
            else -> null
        }
    }

    private fun FoodItemEntity.toModel(): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            portionDescription = portionDescription,
            rawCalories = rawCalories,
            bufferedCalories = bufferedCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            mealType = mealType,
            dateString = dateString,
            isAiEstimated = isAiEstimated,
            barcode = barcode,
            timestamp = timestamp
        )
    }

    private fun FoodItem.toEntity(): FoodItemEntity {
        return FoodItemEntity(
            id = id,
            name = name,
            portionDescription = portionDescription,
            rawCalories = rawCalories,
            bufferedCalories = bufferedCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            mealType = mealType,
            dateString = dateString,
            isAiEstimated = isAiEstimated,
            barcode = barcode,
            timestamp = timestamp
        )
    }
}

data class FoodItemPreset(
    val name: String,
    val portion: String,
    val rawCalories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val defaultMealType: MealType
)

data class BarcodeLookupResult(
    val productName: String,
    val brand: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingSizeDescription: String = "100g",
    val pieceWeightGrams: Float = 100f,
    val pieceUnitName: String = "Portion",
    val hasPieceServing: Boolean = false
) {
    fun calculateForGrams(grams: Float): CalculatedNutrition {
        val safeGrams = grams.coerceAtLeast(0f)
        val factor = safeGrams / 100.0
        val rawKcal = (caloriesPer100g * factor).toInt()
        val protein = (proteinPer100g * factor).toFloat()
        val carbs = (carbsPer100g * factor).toFloat()
        val fat = (fatPer100g * factor).toFloat()
        val gramsDisplay = if (safeGrams % 1f == 0f) "${safeGrams.toInt()}g" else String.format(java.util.Locale.US, "%.1fg", safeGrams)
        return CalculatedNutrition(
            grams = safeGrams,
            calories = rawKcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            portionText = gramsDisplay
        )
    }

    fun calculateForPieces(count: Float): CalculatedNutrition {
        val safeCount = count.coerceAtLeast(0f)
        val totalGrams = (safeCount * pieceWeightGrams).coerceAtLeast(0f)
        val factor = totalGrams / 100.0
        val rawKcal = (caloriesPer100g * factor).toInt()
        val protein = (proteinPer100g * factor).toFloat()
        val carbs = (carbsPer100g * factor).toFloat()
        val fat = (fatPer100g * factor).toFloat()
        val countFormatted = if (safeCount % 1f == 0f) safeCount.toInt().toString() else String.format(java.util.Locale.US, "%.1f", safeCount)
        val portionText = "$countFormatted $pieceUnitName (${totalGrams.toInt()}g)"
        return CalculatedNutrition(
            grams = totalGrams,
            calories = rawKcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            portionText = portionText
        )
    }

    // Backward compatibility helpers
    val servingSize: String get() = servingSizeDescription
    val caloriesPerServing: Int get() = calculateForGrams(if (hasPieceServing) pieceWeightGrams else 100f).calories
    val proteinPerServing: Float get() = calculateForGrams(if (hasPieceServing) pieceWeightGrams else 100f).protein
    val carbsPerServing: Float get() = calculateForGrams(if (hasPieceServing) pieceWeightGrams else 100f).carbs
    val fatPerServing: Float get() = calculateForGrams(if (hasPieceServing) pieceWeightGrams else 100f).fat
}

data class FoodSearchResultItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val brand: String = "",
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingSizeDescription: String = "100g",
    val pieceWeightGrams: Float = 100f,
    val pieceUnitName: String = "Portion",
    val hasPieceServing: Boolean = false,
    val isFromOpenFoodFacts: Boolean = false,
    val barcode: String? = null
) {
    fun calculateForGrams(grams: Float): CalculatedNutrition {
        val safeGrams = grams.coerceAtLeast(0f)
        val factor = safeGrams / 100.0
        val rawKcal = (caloriesPer100g * factor).toInt()
        val protein = (proteinPer100g * factor).toFloat()
        val carbs = (carbsPer100g * factor).toFloat()
        val fat = (fatPer100g * factor).toFloat()
        val gramsDisplay = if (safeGrams % 1f == 0f) "${safeGrams.toInt()}g" else String.format(java.util.Locale.US, "%.1fg", safeGrams)
        return CalculatedNutrition(
            grams = safeGrams,
            calories = rawKcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            portionText = gramsDisplay
        )
    }

    fun calculateForPieces(count: Float): CalculatedNutrition {
        val safeCount = count.coerceAtLeast(0f)
        val totalGrams = (safeCount * pieceWeightGrams).coerceAtLeast(0f)
        val factor = totalGrams / 100.0
        val rawKcal = (caloriesPer100g * factor).toInt()
        val protein = (proteinPer100g * factor).toFloat()
        val carbs = (carbsPer100g * factor).toFloat()
        val fat = (fatPer100g * factor).toFloat()
        val countFormatted = if (safeCount % 1f == 0f) safeCount.toInt().toString() else String.format(java.util.Locale.US, "%.1f", safeCount)
        val portionText = "$countFormatted $pieceUnitName (${totalGrams.toInt()}g)"
        return CalculatedNutrition(
            grams = totalGrams,
            calories = rawKcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            portionText = portionText
        )
    }
}

data class CalculatedNutrition(
    val grams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val portionText: String
)

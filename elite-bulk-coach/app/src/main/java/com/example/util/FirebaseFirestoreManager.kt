package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseFirestoreManager {
    private const val TAG = "FirebaseFirestoreManager"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    fun getFirestore(context: Context): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: run {
                val app = try {
                    FirebaseApp.getInstance()
                } catch (_: Exception) {
                    FirebaseApp.initializeApp(context.applicationContext) ?: FirebaseApp.getInstance()
                }
                val instance = FirebaseFirestore.getInstance(app)
                firestoreInstance = instance
                instance
            }
        }
    }

    /**
     * Saves or updates a user document in Cloud Firestore under collection "users/{email}"
     */
    suspend fun saveUserAccount(
        context: Context,
        email: String,
        name: String,
        passwordHash: String = "",
        profile: UserProfile? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext false

        return@withContext try {
            val db = getFirestore(context)
            val userRef = db.collection("users").document(cleanEmail)

            val userData = mutableMapOf<String, Any>(
                "email" to cleanEmail,
                "name" to name.ifBlank { "Bulk Athlet" },
                "lastActive" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            if (passwordHash.isNotBlank()) {
                userData["passwordHash"] = passwordHash
            }

            if (profile != null) {
                userData["id"] = profile.id
                userData["profileImageUri"] = profile.profileImageUri ?: ""
                userData["heightCm"] = profile.heightCm
                userData["currentWeightKg"] = profile.currentWeightKg.toDouble()
                userData["startWeightKg"] = profile.startWeightKg.toDouble()
                userData["targetWeightKg"] = profile.targetWeightKg.toDouble()
                userData["weeklyWeightGainKg"] = profile.weeklyWeightGainKg.toDouble()
                userData["age"] = profile.age
                userData["gender"] = profile.gender
                userData["primaryGoal"] = profile.primaryGoal
                userData["dailyCalorieTarget"] = profile.dailyCalorieTarget
                userData["proteinTargetGrams"] = profile.proteinTargetGrams
                userData["carbsTargetGrams"] = profile.carbsTargetGrams
                userData["fatTargetGrams"] = profile.fatTargetGrams
                userData["isOnboarded"] = profile.isOnboarded
                userData["currentTrainingWeek"] = profile.currentTrainingWeek
                userData["isDeloadActive"] = profile.isDeloadActive
                userData["trainingSplit"] = profile.trainingSplit
            }

            userRef.set(userData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully saved user account in Firestore: $cleanEmail")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user account to Firestore for $cleanEmail", e)
            false
        }
    }

    /**
     * Checks if a user document exists in Firestore under collection "users/{email}"
     */
    suspend fun checkUserExists(context: Context, email: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext false

        return@withContext try {
            val db = getFirestore(context)
            val doc = db.collection("users").document(cleanEmail).get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence in Firestore for $cleanEmail", e)
            false
        }
    }

    /**
     * Retrieves user account data map from Firestore document "users/{email}"
     */
    suspend fun getUserDocument(context: Context, email: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext null

        return@withContext try {
            val db = getFirestore(context)
            val doc = db.collection("users").document(cleanEmail).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user document from Firestore for $cleanEmail", e)
            null
        }
    }
}

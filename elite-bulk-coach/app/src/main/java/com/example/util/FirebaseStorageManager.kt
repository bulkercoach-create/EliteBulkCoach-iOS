package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"
    const val STORAGE_BUCKET = "axon-app-2930f.firebasestorage.app"

    @Volatile
    private var storageInstance: FirebaseStorage? = null

    /**
     * Retrieves the FirebaseStorage instance explicitly bound to the storageBucket
     * from google-services.json ('axon-app-2930f.firebasestorage.app') using getStorage(app).
     */
    fun getStorage(context: Context): FirebaseStorage {
        return storageInstance ?: synchronized(this) {
            storageInstance ?: run {
                val app = try {
                    FirebaseApp.getInstance()
                } catch (_: Exception) {
                    FirebaseApp.initializeApp(context.applicationContext) ?: FirebaseApp.getInstance()
                }

                val storage = try {
                    FirebaseStorage.getInstance(app, "gs://$STORAGE_BUCKET")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed initializing Storage with gs://$STORAGE_BUCKET, attempting standard getStorage", e)
                    try {
                        FirebaseStorage.getInstance(app)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallback Storage initialization error", e2)
                        FirebaseStorage.getInstance()
                    }
                }
                storageInstance = storage
                storage
            }
        }
    }

    /**
     * Uploads an image file or Uri to Firebase Storage in the specified folder
     * and returns the remote HTTPS download URL.
     */
    suspend fun uploadImage(
        context: Context,
        localPathOrUri: String,
        folder: String = "uploads"
    ): String = withContext(Dispatchers.IO) {
        if (localPathOrUri.isBlank()) return@withContext localPathOrUri
        if (localPathOrUri.startsWith("http://") || localPathOrUri.startsWith("https://")) {
            return@withContext localPathOrUri // Already a remote storage URL
        }

        try {
            val storage = getStorage(context)
            val fileName = "${folder}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}.jpg"
            val ref: StorageReference = storage.reference.child("$folder/$fileName")

            val file = File(localPathOrUri.replace("file://", ""))
            val uploadTask = if (file.exists()) {
                ref.putFile(Uri.fromFile(file))
            } else {
                ref.putFile(Uri.parse(localPathOrUri))
            }

            uploadTask.await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded image to Firebase Storage bucket ($STORAGE_BUCKET): $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage upload to $STORAGE_BUCKET failed for $localPathOrUri: ${e.message}", e)
            localPathOrUri // Fallback to local path so user experience is never interrupted
        }
    }

    /**
     * Uploads a text or JSON payload string directly to a file in Firebase Storage.
     */
    suspend fun uploadJson(
        context: Context,
        jsonContent: String,
        storagePath: String
    ): String = withContext(Dispatchers.IO) {
        if (jsonContent.isBlank()) return@withContext ""
        try {
            val storage = getStorage(context)
            val ref: StorageReference = storage.reference.child(storagePath)
            val bytes = jsonContent.toByteArray(Charsets.UTF_8)

            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded JSON to Firebase Storage bucket ($STORAGE_BUCKET) at $storagePath: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage upload to $STORAGE_BUCKET failed for $storagePath: ${e.message}", e)
            ""
        }
    }

    /**
     * Downloads a JSON file string from Firebase Storage.
     */
    suspend fun downloadJson(
        context: Context,
        storagePath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val storage = getStorage(context)
            val ref: StorageReference = storage.reference.child(storagePath)
            val maxBytes: Long = 10 * 1024 * 1024 // 10MB
            val bytes = ref.getBytes(maxBytes).await()
            val content = String(bytes, Charsets.UTF_8)
            Log.d(TAG, "Successfully downloaded JSON from Firebase Storage bucket ($STORAGE_BUCKET) at $storagePath")
            content
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage download from $STORAGE_BUCKET failed for $storagePath: ${e.message}")
            null
        }
    }
}

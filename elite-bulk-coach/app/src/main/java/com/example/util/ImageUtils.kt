package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    private const val TAG = "EXIF_DEBUG"

    /**
     * Reads an image from Uri using ExifInterface(inputStream) to check EXIF TAG_ORIENTATION,
     * applies a precise Matrix transformation for ALL 8 EXIF orientation cases,
     * and returns a physically upright Bitmap.
     */
    fun getRotatedBitmap(context: Context, imageUri: Uri): Bitmap? {
        Log.d(TAG, "getRotatedBitmap called with Uri: $imageUri")

        // Step 1: Read EXIF orientation tag from an InputStream
        var orientation = ExifInterface.ORIENTATION_UNDEFINED
        try {
            openStream(context, imageUri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                Log.d(TAG, "EXIF TAG_ORIENTATION value for $imageUri: $orientation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF from $imageUri: ${e.message}", e)
        }

        // Step 2: Decode bitmap from a fresh InputStream
        val bitmapStream = openStream(context, imageUri) ?: run {
            Log.e(TAG, "Could not open InputStream for bitmap decoding: $imageUri")
            return null
        }

        val originalBitmap: Bitmap? = try {
            bitmapStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode Bitmap for $imageUri: ${e.message}", e)
            null
        }

        if (originalBitmap == null) {
            Log.e(TAG, "Decoded originalBitmap is null for $imageUri")
            return null
        }

        Log.d(TAG, "Original decoded bitmap dimensions: ${originalBitmap.width}x${originalBitmap.height}")

        // Step 3: Handle all 8 EXIF orientation cases with a precise Matrix
        val matrix = Matrix()
        var rotationDegrees = 0f
        var flipHorizontal = false
        var flipVertical = false

        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> {
                Log.d(TAG, "Case 1: ORIENTATION_NORMAL (1) - No rotation needed")
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                Log.d(TAG, "Case 2: ORIENTATION_FLIP_HORIZONTAL (2) - Flip horizontal")
                matrix.postScale(-1f, 1f)
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                Log.d(TAG, "Case 3: ORIENTATION_ROTATE_180 (3) - Rotate 180 degrees")
                matrix.postRotate(180f)
                rotationDegrees = 180f
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                Log.d(TAG, "Case 4: ORIENTATION_FLIP_VERTICAL (4) - Flip vertical")
                matrix.postRotate(180f)
                matrix.postScale(-1f, 1f)
                rotationDegrees = 180f
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                Log.d(TAG, "Case 5: ORIENTATION_TRANSPOSE (5) - Rotate 90 and flip horizontal")
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                rotationDegrees = 90f
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                Log.d(TAG, "Case 6: ORIENTATION_ROTATE_90 (6) - Rotate 90 degrees")
                matrix.postRotate(90f)
                rotationDegrees = 90f
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                Log.d(TAG, "Case 7: ORIENTATION_TRANSVERSE (7) - Rotate 270 and flip horizontal")
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                rotationDegrees = 270f
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                Log.d(TAG, "Case 8: ORIENTATION_ROTATE_270 (8) - Rotate 270 degrees")
                matrix.postRotate(270f)
                rotationDegrees = 270f
            }
            else -> {
                // ORIENTATION_UNDEFINED or unknown
                Log.d(TAG, "Case 0/Unknown: EXIF orientation is UNDEFINED ($orientation). Checking dimensions...")
                if (originalBitmap.width > originalBitmap.height) {
                    Log.d(TAG, "Image width (${originalBitmap.width}) > height (${originalBitmap.height}) without EXIF tag. Forcing 90 degree portrait rotation.")
                    matrix.postRotate(90f)
                    rotationDegrees = 90f
                }
            }
        }

        // Apply matrix if non-identity
        val resultBitmap = if (!matrix.isIdentity) {
            try {
                val transformed = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )
                Log.d(TAG, "Transformed bitmap: angle=$rotationDegrees, flipH=$flipHorizontal, flipV=$flipVertical, new size: ${transformed.width}x${transformed.height}")
                transformed
            } catch (e: Exception) {
                Log.e(TAG, "Error applying matrix transformation: ${e.message}", e)
                originalBitmap
            }
        } else {
            Log.d(TAG, "Matrix is identity. Image is already physically upright.")
            originalBitmap
        }

        return resultBitmap
    }

    /**
     * Helper overload accepting String uri or file path.
     */
    fun getRotatedBitmap(context: Context, uriOrPath: String): Bitmap? {
        if (uriOrPath.isBlank()) return null
        val uri = try {
            if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                Uri.parse(uriOrPath)
            } else {
                Uri.fromFile(File(uriOrPath.replace("file://", "").replace("file:", "")))
            }
        } catch (e: Exception) {
            Uri.parse(uriOrPath)
        }
        return getRotatedBitmap(context, uri)
    }

    /**
     * Reads photo from Uri/Path, rotates it EXIF-upright, saves it as a local upright JPEG file
     * in internal app storage, and returns the absolute path of the newly saved file.
     */
    fun processAndSaveImageToAppStorage(context: Context, uriOrPath: String, prefix: String = "photo"): String {
        if (uriOrPath.isBlank()) return uriOrPath
        return try {
            val bitmap = getRotatedBitmap(context, uriOrPath) ?: return uriOrPath
            val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Log.d(TAG, "Saved upright photo to internal storage: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error in processAndSaveImageToAppStorage: ${e.message}", e)
            uriOrPath
        }
    }

    /**
     * Takes a Bitmap (e.g. from camera preview), rotates if landscape,
     * saves to local upright JPEG file, and returns the file's absolute path.
     */
    fun processAndSaveBitmapToAppStorage(context: Context, bitmap: Bitmap, prefix: String = "photo"): String {
        return try {
            var targetBitmap = bitmap
            if (bitmap.width > bitmap.height) {
                Log.d(TAG, "Camera bitmap is landscape (${bitmap.width}x${bitmap.height}). Rotating 90 degrees to portrait.")
                val matrix = Matrix()
                matrix.postRotate(90f)
                targetBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Log.d(TAG, "Saved upright camera preview bitmap to internal storage: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error in processAndSaveBitmapToAppStorage: ${e.message}", e)
            ""
        }
    }

    private fun openStream(context: Context, uri: Uri): InputStream? {
        return try {
            val scheme = uri.scheme
            if (scheme == "content" || scheme == "file") {
                context.contentResolver.openInputStream(uri)
            } else {
                val path = uri.path ?: uri.toString()
                val cleanPath = path.replace("file://", "").replace("file:", "")
                val file = File(cleanPath)
                if (file.exists()) {
                    file.inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "openStream failed for $uri: ${e.message}")
            null
        }
    }
}

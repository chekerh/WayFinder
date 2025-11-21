package tn.esprit.wayfinder.utils

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object ImageCompressor {
    private const val MAX_WIDTH = 1280 // Reduced from 1920 for faster uploads
    private const val MAX_HEIGHT = 1280 // Reduced from 1920 for faster uploads
    private const val MAX_FILE_SIZE_KB = 300 // 300KB max file size (reduced from 1MB for faster uploads)
    private const val COMPRESSION_QUALITY = 70 // JPEG quality (reduced from 85 for smaller files)

    /**
     * Compress an image from URI and return a compressed File
     * @param context - Android context
     * @param uri - Image URI
     * @param outputFileName - Optional output filename
     * @return Compressed image file
     */
    /**
     * Open an input stream from a URI, handling both content:// and file:// URIs
     */
    private fun openInputStream(context: Context, uri: Uri): InputStream {
        return when (uri.scheme) {
            "file" -> {
                // For file:// URIs, use FileInputStream directly
                val file = File(uri.path ?: throw IllegalArgumentException("Invalid file URI: $uri"))
                if (!file.exists()) {
                    throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
                }
                FileInputStream(file)
            }
            "content" -> {
                // For content:// URIs, use ContentResolver
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
            }
            else -> {
                throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
            }
        }
    }

    suspend fun compressImage(
        context: Context,
        uri: Uri,
        outputFileName: String? = null
    ): File = withContext(Dispatchers.IO) {
        // Step 1: Read image dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        val inputStream1 = try {
            openInputStream(context, uri)
        } catch (e: SecurityException) {
            android.util.Log.e("ImageCompressor", "Security exception opening stream: ${e.message}", e)
            throw IllegalArgumentException("Cannot access image. Please grant permission or try selecting the image again.", e)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error opening stream: ${e.message}", e)
            throw IllegalArgumentException("Cannot open input stream for URI: $uri", e)
        }
        
        inputStream1.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calculate sample size to reduce memory usage
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
        options.inJustDecodeBounds = false

        // Step 2: Decode bitmap with calculated sample size
        val inputStream2 = try {
            openInputStream(context, uri)
        } catch (e: SecurityException) {
            android.util.Log.e("ImageCompressor", "Security exception opening stream: ${e.message}", e)
            throw IllegalArgumentException("Cannot access image. Please grant permission or try selecting the image again.", e)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error opening stream: ${e.message}", e)
            throw IllegalArgumentException("Cannot open input stream for URI: $uri", e)
        }
        
        val bitmap = inputStream2.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
                ?: throw IllegalArgumentException("Failed to decode bitmap")
        }

        // Handle orientation
        val orientedBitmap = handleOrientation(context, uri, bitmap)
        if (orientedBitmap != bitmap) {
            bitmap.recycle()
        }

        // Resize if still too large
        val resizedBitmap = resizeBitmap(orientedBitmap, MAX_WIDTH, MAX_HEIGHT)
        if (resizedBitmap != orientedBitmap) {
            orientedBitmap.recycle()
        }

        // Compress to file
        val outputFile = File(
            context.cacheDir,
            outputFileName ?: "compressed_${System.currentTimeMillis()}.jpg"
        )

        var quality = COMPRESSION_QUALITY
        var outputStream: FileOutputStream? = null

        try {
            do {
                outputStream = FileOutputStream(outputFile)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                outputStream = null

                val fileSizeKB = outputFile.length() / 1024

                if (fileSizeKB > MAX_FILE_SIZE_KB && quality > 40) {
                    quality -= 15 // More aggressive quality reduction
                    outputFile.delete()
                } else {
                    break
                }
            } while (quality > 40)

            // If still too large, resize more aggressively with multiple attempts
            var currentBitmap = resizedBitmap
            var currentScale = 0.8f
            while (outputFile.length() / 1024 > MAX_FILE_SIZE_KB && currentScale >= 0.5f) {
                val newWidth = (currentBitmap.width * currentScale).toInt()
                val newHeight = (currentBitmap.height * currentScale).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)
                if (currentBitmap != resizedBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = scaledBitmap
                outputFile.delete()

                outputStream = FileOutputStream(outputFile)
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream) // Lower quality for aggressive resize
                outputStream.close()
                
                if (outputFile.length() / 1024 > MAX_FILE_SIZE_KB) {
                    currentScale -= 0.1f
                }
            }
            currentBitmap.recycle()

            outputFile
        } catch (e: Exception) {
            outputStream?.close()
            resizedBitmap.recycle()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw e
        }
    }

    /**
     * Compress multiple images in parallel
     */
    suspend fun compressImages(
        context: Context,
        uris: List<Uri>
    ): List<File> = withContext(Dispatchers.IO) {
        uris.mapIndexed { index, uri ->
            try {
                compressImage(context, uri, "compressed_${System.currentTimeMillis()}_$index.jpg")
            } catch (e: Exception) {
                android.util.Log.e("ImageCompressor", "Failed to compress image $index: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Calculate inSampleSize to reduce memory usage
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Resize bitmap if it exceeds max dimensions
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Handle image orientation based on EXIF data
     */
    private fun handleOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = try {
                openInputStream(context, uri)
            } catch (e: SecurityException) {
                android.util.Log.w("ImageCompressor", "Security exception reading EXIF: ${e.message}")
                return bitmap
            } catch (e: Exception) {
                android.util.Log.w("ImageCompressor", "Error reading EXIF: ${e.message}")
                return bitmap
            }
            
            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(bitmap, horizontal = true)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(bitmap, vertical = true)
                    else -> bitmap
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ImageCompressor", "Failed to handle orientation: ${e.message}")
            bitmap
        }
    }

    /**
     * Rotate bitmap by degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Flip bitmap horizontally or vertically
     */
    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean = false, vertical: Boolean = false): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            if (vertical) {
                postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }
}


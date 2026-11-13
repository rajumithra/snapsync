package com.snapsync.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object ImageProcessor {

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun generateThumbnail(sourceFile: File, outputFile: File, maxDimension: Int = 400): Boolean {
        return scaleAndSave(sourceFile, outputFile, maxDimension, quality = 80)
    }

    fun generatePreview(sourceFile: File, outputFile: File, maxDimension: Int = 1920): Boolean {
        return scaleAndSave(sourceFile, outputFile, maxDimension, quality = 85)
    }

    private fun scaleAndSave(sourceFile: File, outputFile: File, maxDimension: Int, quality: Int): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return false

            var inSampleSize = 1
            val maxSide = maxOf(width, height)
            while ((maxSide / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return false

            // Correct orientation from EXIF
            val exif = ExifInterface(sourceFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val matrix = Matrix()
            if (rotationAngle != 0f) {
                matrix.postRotate(rotationAngle)
            }

            // Calculate precise scaling
            val currentMax = maxOf(originalBitmap.width, originalBitmap.height)
            if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                matrix.postScale(scale, scale)
            }

            val finalBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
            )

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
            }

            if (finalBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            finalBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


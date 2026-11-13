package com.snapsync.app.util

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExifData(
    val make: String? = null,
    val model: String? = null,
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val lensModel: String? = null,
    val dateTaken: Date? = null,
    val width: Int? = null,
    val height: Int? = null
)

object ExifExtractor {

    fun extract(file: File): ExifData {
        if (!file.exists() || file.length() == 0L) {
            return ExifData()
        }

        return try {
            val exif = ExifInterface(file.absolutePath)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).let { if (it > 0) it else null }
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
            val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).let { if (it > 0) it else null }
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).let { if (it > 0) it else null }

            val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            var dateTaken: Date? = null
            if (dateStr != null) {
                try {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    dateTaken = sdf.parse(dateStr)
                } catch (e: Exception) {
                    // Ignore date parse failure
                }
            }

            ExifData(
                make = make?.trim(),
                model = model?.trim(),
                iso = iso,
                shutterSpeed = if (exposureTime != null) "${exposureTime}s" else null,
                aperture = if (fNumber != null) "f/$fNumber" else null,
                focalLength = if (focal != null) "${focal}mm" else null,
                lensModel = lens?.trim(),
                dateTaken = dateTaken ?: Date(file.lastModified()),
                width = width,
                height = height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExifData(dateTaken = Date(file.lastModified()))
        }
    }
}


package nl.esites.easyimagepicker

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.FileOutputStream


object ImageCompressor {

    fun compressImageWithResolver(
        inputImageUri: Uri,
        outputFilePath: String,
        contentResolver: ContentResolver,
        maxImageDimension: Int,
        compressionQuality: Int
    ): Boolean {
        var inputStream = contentResolver.openInputStream(inputImageUri) ?: return false

        val dbo = BitmapFactory.Options()
        dbo.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, dbo)
        inputStream.close()

        val exif = ExifInterface(inputStream)
        val rotation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationInDegrees = exifToDegrees(rotation)

        inputStream = contentResolver.openInputStream(inputImageUri) ?: return false

        var srcBitmap = BitmapFactory.decodeStream(inputStream)
        srcBitmap = resizeBitmap(srcBitmap, maxImageDimension)
        inputStream.close()

        if (rotationInDegrees > 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationInDegrees.toFloat())
            srcBitmap = Bitmap.createBitmap(
                srcBitmap, 0, 0, srcBitmap.width,
                srcBitmap.height, matrix, true
            )
        }

        val outputFileOutputStream = FileOutputStream(outputFilePath)
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputFileOutputStream)
        outputFileOutputStream.close()

        return true
    }

    fun compressImageWithPath(
        inputImagePath: String,
        outputFilePath: String,
        maxImageDimension: Int,
        compressionQuality: Int
    ): Boolean {
        val dbo = BitmapFactory.Options()
        dbo.inJustDecodeBounds = true
        BitmapFactory.decodeFile(inputImagePath, dbo)

        val exif = ExifInterface(inputImagePath)
        val rotation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = exifToDegrees(rotation)

        var srcBitmap = BitmapFactory.decodeFile(inputImagePath)
        srcBitmap = resizeBitmap(srcBitmap, maxImageDimension)

        if (rotationInDegrees > 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationInDegrees.toFloat())
            srcBitmap = Bitmap.createBitmap(
                srcBitmap, 0, 0, srcBitmap.width,
                srcBitmap.height, matrix, true
            )
        }
        val outputFileOutputStream = FileOutputStream(outputFilePath)
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputFileOutputStream)
        outputFileOutputStream.close()

        return true
    }

    private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        if (source.height >= source.width) {
            if (source.height <= maxLength) { // if image already smaller than the required height
                return source
            }
            val aspectRatio = source.width.toDouble() / source.height.toDouble()
            val targetWidth = (maxLength * aspectRatio).toInt()
            return Bitmap.createScaledBitmap(source, targetWidth, maxLength, true)
        } else {
            if (source.width <= maxLength) { // if image already smaller than the required height
                return source
            }
            val aspectRatio = source.height.toDouble() / source.width.toDouble()
            val targetHeight = (maxLength * aspectRatio).toInt()
            return Bitmap.createScaledBitmap(source, maxLength, targetHeight, true)
        }
    }

    private fun exifToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
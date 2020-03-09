package nl.esites.easyimagepicker

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.FileOutputStream
import kotlin.math.max


object ImageCompressor {

    fun compressImageWithResolver(
        inputImageUri: Uri,
        outputFilePath: String,
        contentResolver: ContentResolver,
        maxImageDimension: Int
    ): Boolean {
        var inputStream = contentResolver.openInputStream(inputImageUri) ?: return false

        val dbo = BitmapFactory.Options()
        dbo.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, dbo)
        inputStream.close()

        val exif = ExifInterface(inputStream)
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = exifToDegrees(rotation)

        val rotatedWidth: Int
        val rotatedHeight: Int

        if (rotationInDegrees == 90 || rotationInDegrees == 270) {
            rotatedWidth = dbo.outHeight
            rotatedHeight = dbo.outWidth
        } else {
            rotatedWidth = dbo.outWidth
            rotatedHeight = dbo.outHeight
        }

        var srcBitmap: Bitmap
        inputStream = contentResolver.openInputStream(inputImageUri) ?: return false

        if (rotatedWidth > maxImageDimension || rotatedHeight > maxImageDimension) {
            val widthRatio = rotatedWidth.toFloat() / maxImageDimension.toFloat()
            val heightRatio =rotatedHeight.toFloat() / maxImageDimension.toFloat()
            val maxRatio = max(widthRatio, heightRatio)

            val options = BitmapFactory.Options()
            options.inSampleSize = maxRatio.toInt()

            srcBitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return false
        } else {
            srcBitmap = BitmapFactory.decodeStream(inputStream)
        }
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
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputFileOutputStream)
        outputFileOutputStream.close()

        return true
    }

    private fun exifToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    fun compressImageWithPath(
        inputImagePath: String,
        outputFilePath: String,
        maxImageDimension: Int
    ): Boolean {
        val dbo = BitmapFactory.Options()
        dbo.inJustDecodeBounds = true
        BitmapFactory.decodeFile(inputImagePath)

        val exif = ExifInterface(inputImagePath)
        val rotation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = exifToDegrees(rotation)

        val rotatedWidth: Int
        val rotatedHeight: Int

        if (rotationInDegrees == 90 || rotationInDegrees == 270) {
            rotatedWidth = dbo.outHeight
            rotatedHeight = dbo.outWidth
        } else {
            rotatedWidth = dbo.outWidth
            rotatedHeight = dbo.outHeight
        }

        var srcBitmap: Bitmap

        if (rotatedWidth > maxImageDimension || rotatedHeight > maxImageDimension) {
            val widthRatio = rotatedWidth.toFloat() / maxImageDimension.toFloat()
            val heightRatio = rotatedHeight.toFloat() / maxImageDimension.toFloat()
            val maxRatio = max(widthRatio, heightRatio)

            val options = BitmapFactory.Options()
            options.inSampleSize = maxRatio.toInt()

            srcBitmap = BitmapFactory.decodeFile(inputImagePath, options)
        } else {
            srcBitmap = BitmapFactory.decodeFile(inputImagePath)
        }

        if (rotationInDegrees > 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationInDegrees.toFloat())
            srcBitmap = Bitmap.createBitmap(
                srcBitmap, 0, 0, srcBitmap.width,
                srcBitmap.height, matrix, true
            )
        }

        val outputFileOutputStream = FileOutputStream(outputFilePath)
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputFileOutputStream)
        outputFileOutputStream.close()

        return true
    }
}
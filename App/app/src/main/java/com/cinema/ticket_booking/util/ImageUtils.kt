package com.cinema.ticket_booking.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    @JvmStatic
    @Throws(Exception::class)
    fun compressImage(context: Context, uri: Uri): File {
        var inputStream = context.contentResolver.openInputStream(uri)

        // 1. Decode bounds to find dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // 2. Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 1080, 1080)
        options.inJustDecodeBounds = false

        // 3. Decode with inSampleSize
        inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close() ?: throw Exception("Không thể đọc ảnh")
        if (bitmap == null) throw Exception("Không thể đọc ảnh")

        // 4. Compress to JPEG
        val tempFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        val bos = ByteArrayOutputStream()

        var quality = 80
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)

        // 5. Ensure file is under 4MB (Backend limit is 5MB)
        while (bos.toByteArray().size > 4 * 1024 * 1024 && quality > 10) {
            bos.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        }

        val fos = FileOutputStream(tempFile)
        fos.write(bos.toByteArray())
        fos.flush()
        fos.close()

        return tempFile
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

package com.cinema.ticket_booking.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    public static File compressImage(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        
        // 1. Decode bounds to find dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        if (inputStream != null) inputStream.close();

        // 2. Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 1080, 1080);
        options.inJustDecodeBounds = false;

        // 3. Decode with inSampleSize
        inputStream = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        if (inputStream != null) inputStream.close();
        if (bitmap == null) throw new Exception("Không thể đọc ảnh");

        // 4. Compress to JPEG
        File tempFile = new File(context.getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        int quality = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        
        // 5. Ensure file is under 4MB (Backend limit is 5MB)
        while (bos.toByteArray().length > 4 * 1024 * 1024 && quality > 10) {
            bos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        }

        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(bos.toByteArray());
        fos.flush();
        fos.close();
        
        return tempFile;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}

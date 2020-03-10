package com.swrve.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SwrveImageScaler {

    // Requires options to have the image size
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static BitmapResult decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight, int minSampleSize) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            int bitmapWidth = options.outWidth;
            int bitmapHeight = options.outHeight;

            // Calculate inSampleSize
            options.inSampleSize = Math.max(calculateInSampleSize(options, reqWidth,
                    reqHeight), minSampleSize);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return new BitmapResult(BitmapFactory.decodeFile(filePath, options), bitmapWidth, bitmapHeight);
        } catch (OutOfMemoryError exp) {
            SwrveLogger.e(Log.getStackTraceString(exp));
        } catch (Exception exp) {
            SwrveLogger.e(Log.getStackTraceString(exp));
        }

        return null;
    }

    public static Bitmap decodeSampledBitmapFromStream(InputStream stream, int reqWidth, int reqHeight, int minSampleSize, String fileUrlForMessage, File cacheDir) {
        OutputStream outStream = null;
        Bitmap bitmap = null;
        try {
            File targetFile = File.createTempFile("notification-image", null, cacheDir);
            outStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.close();

            SwrveImageScaler.BitmapResult bitmapResult = SwrveImageScaler.decodeSampledBitmapFromFile(targetFile.getAbsolutePath(), reqWidth, reqHeight, minSampleSize);
            if (bitmapResult != null) {
                bitmap = bitmapResult.getBitmap();
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception downloading notification image:%s", e, fileUrlForMessage);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (Exception e) {
                    SwrveLogger.e("Exception closing stream for downloading notification image.", e);
                }
            }
        }

        return bitmap;
    }

    public static class BitmapResult {
        private Bitmap bitmap;
        private int width;
        private int height;

        public BitmapResult(Bitmap bitmap, int width, int height) {
            this.bitmap = bitmap;
            this.width = width;
            this.height = height;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}

package com.swrve.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SwrvePushMediaHelper {

    /**
     * Bitmap image media. Returns null if URL is broken
     **/
    protected Bitmap downloadBitmapImageFromUrl(final String mediaUrl) {
        Bitmap bitmap = null;
        try {
            URL url = null;
            if (SwrveHelper.isNotNullOrEmpty(mediaUrl)) {
                try {
                    url = new URL(mediaUrl);
                    url.toURI();
                    SwrveLogger.i("Downloading push image from: %s", mediaUrl);

                } catch (Exception ex) {
                    SwrveLogger.e("Push image from: %s is an invalid url: ", ex, mediaUrl);
                    url = null;
                }
            }

            if (url != null) {

                try {
                    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                    httpConnection.setDoInput(true);
                    httpConnection.connect();
                    httpConnection.setConnectTimeout(10000); //set timeout to 10 seconds

                    InputStream input = httpConnection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);

                } catch (java.net.SocketTimeoutException e) {
                    SwrveLogger.e("Push Image URL: %s has timed out.", e, mediaUrl);
                }
            }

        } catch (Exception e) {
            SwrveLogger.e("Push image has failed to download.", e);
        }

        return bitmap;
    }
}

package io.converser.android.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.converser.android.BuildConfig;

/**
 * @author Jason Connery
 */
public class ImageDownloader implements Runnable {

    private WeakReference<ImageView> imageView;
    private String url;

    public ImageDownloader(ImageView imageView, String url) {

        this.imageView = new WeakReference<ImageView>(imageView);
        this.url = url;
    }

    /**
     * Convenience method to gather all bytes from a stream
     *
     * @param sizeHint
     * @param is
     * @return
     */
    protected static byte[] convertStreamToByteArray(int sizeHint, InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((sizeHint > 0 ? sizeHint : 0));
        byte[] buffer = new byte[1024];
        try {
            int rb = 0;
            do {
                rb = is.read(buffer, 0, buffer.length);

                if (rb > 0) {
                    bos.write(buffer, 0, rb);
                }
            } while (rb > 0);

        } catch (IOException ex) {
            // Done attempting to read
        }

        if (bos.size() > 0) {
            return bos.toByteArray();
        } else {
            return null;
        }
    }

    @Override
    public void run() {

        if (imageView.get() == null) {
            // it's already gone. Dont worry about it
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d("AsyncImageView", "Going to download now");
        }

        byte[] imageBytes = downloadImageBytes(url);

        if (imageBytes != null) {
            Bitmap bm = getImageBitmap(imageBytes);

            // We checked a minute ago, but between then and now after dling image, image view may be gone
            if (imageView.get() != null) {
                imageView.get().post(new ImageSetter(imageView.get(), bm));
            }
        } else {
            // What to do if no image?
        }

    }

    protected byte[] downloadImageBytes(String url) {

        byte[] result = null;
        HttpURLConnection urlConnection = null;

        try {

            URL uurl = new URL(url);
            urlConnection = (HttpURLConnection) uurl.openConnection();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                int sizeHint = urlConnection.getContentLength();
                result = convertStreamToByteArray(sizeHint, urlConnection.getInputStream());
                if (BuildConfig.DEBUG) {
                    Log.d("AsyncImageView", "Image downloaded " + url);
                }
            } else {
                Log.e("AsyncImageView", "Error downloading image, http code : " + urlConnection.getResponseCode() + " : " + urlConnection.getResponseMessage());
            }
            urlConnection.disconnect();
            urlConnection = null;
            uurl = null;
            return result;

        } catch (MalformedURLException e) {
            Log.e("AsyncImageView", "Bad url " + url, e);
            return null;
        } catch (IOException e) {
            Log.e("AsyncImageView", "IO Error " + url, e);
            return null;
        }
    }

    /**
     * Convert a byte array to a bitmap, using some default options.
     *
     * @param imageBytes
     * @return
     */
    protected Bitmap getImageBitmap(byte[] imageBytes) {

        if (imageBytes != null && imageBytes.length > 0) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            byte[] byteArrayForBitmap = new byte[16 * 1024];
            opts.inTempStorage = byteArrayForBitmap;
            opts.inPurgeable = true;
            Bitmap tmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, opts);
            return tmp;
        }

        // No bytes. Turn null.
        return null;

    }

    /**
     * A Runnable that can be posted to the UI thread in order to set an image
     *
     * @author Jason Connery
     */
    private static class ImageSetter implements Runnable {

        private ImageView imageView;
        private Bitmap bitmap;

        public ImageSetter(ImageView imageView, Bitmap b) {
            this.imageView = imageView;
            this.bitmap = b;
        }

        @Override
        public void run() {

            if (imageView != null) {

                imageView.setImageBitmap(bitmap);
            } else {

                bitmap = null;
                imageView = null;
            }

        }
    }
}

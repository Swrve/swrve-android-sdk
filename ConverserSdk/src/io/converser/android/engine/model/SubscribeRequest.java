package io.converser.android.engine.model;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class SubscribeRequest {

    private String ident;
    private Device device;
    private AppInfo app;
    private String claim;

    public SubscribeRequest(Context context, String appId, String version) {
        device = new Device(context);
        app = new AppInfo(appId, version);
    }


    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }


    public String getIdentity() {
        return ident;
    }


    public void setIdentity(String identity) {
        this.ident = identity;
    }


    public static class Device {

        private String os;
        private String version;
        private String model;

        private Tokens tokens;
        private DisplayInfo display;


        /**
         * Create a new device object, setting the os, version, and model automaticaly
         */
        public Device(Context context) {
            this.os = "Android";
            this.version = Build.VERSION.RELEASE;
            this.model = Build.MANUFACTURER + " " + Build.MODEL;

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            this.display = new DisplayInfo(displayMetrics.density,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels,
                    displayMetrics.densityDpi);
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Tokens getTokens() {
            return tokens;
        }

        public void setTokens(Tokens tokens) {
            this.tokens = tokens;
        }

        public DisplayInfo getDisplay() {
            return display;
        }

        public static class Tokens {

            private String push;
            private String c2dm;

            public Tokens() {

            }

            public Tokens(String push, String c2dm) {
                this.push = push;
                this.c2dm = c2dm;
            }

            public String getC2dm() {
                return c2dm;
            }
        }
    }

    public static class DisplayInfo {

        private float density;
        private int widthPixels;
        private int heightPixels;
        private int densityDpi;

        public DisplayInfo(float density, int widthPixels, int heightPixels, int densityDpi) {
            super();
            this.density = density;
            this.widthPixels = widthPixels;
            this.heightPixels = heightPixels;
            this.densityDpi = densityDpi;
        }

    }

    public static class AppInfo {
        private String id;
        private String version;

        public AppInfo(String id, String version) {
            super();
            this.id = id;
            this.version = version;
        }


    }
}

package com.swrve.sdk.uiabtesting;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

public class SherifComponent {

    private static final String LOG_TAG = "SHERIFF";

	protected Activity activity;
	protected WebSocket webSocket;
	protected Timer timer;
	
	protected String previousSupportedTypes;
	protected int statusBarHeight;
	private Map<Class, JSONObject> viewAttributes = new HashMap<Class, JSONObject>();

	public SherifComponent(JSONObject jsonQa, Activity activity) {
		this.activity = activity;
        startUIABTesting();
	}

	private void startUIABTesting() {
        AsyncHttpClient.getDefaultInstance().websocket("ws://192.168.1.130:9000/deviceSocket", null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception e, final WebSocket webSocket) {
                SherifComponent.this.webSocket = webSocket;
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception e) {
                        int k = 0;
                        k++;
                    }
                });

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                        byte[] array = byteBufferList.getAllByteArray();
                        String message = new String(array);
                        Log.d("WEBSOCKET", message);
                        byteBufferList.recycle();

                        processMessage(message);
                    }
                });

                // Send initial screenshot
                sendScreenshot();

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(final String message) {
                        processMessage(message);
                    }
                });
            }
        });
		
		// start timer
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	sendScreenshot();
            }
        }, 0, 5000);
	}

    private void processMessage(String message) {
        try {
            Log.e(LOG_TAG, "COMMAND: " + message);
            String commandSelect = "#select:";
            String commandSet = "#set:";
            if (message.startsWith(commandSelect)) {
                // Extract x and y
                String msgData = message.substring(commandSelect.length());
                String data[] = msgData.split(",");
                int x = (int)(Float.parseFloat(data[0]));
                int y = (int)(Float.parseFloat(data[1]) + statusBarHeight);
                String supportedTypesRaw = data[2];
                if (previousSupportedTypes != null && !previousSupportedTypes.equals(supportedTypesRaw)) {
                    viewAttributes.clear();
                }

                Set<Class> supportedTypes = new HashSet<Class>();
                for(String type : supportedTypesRaw.split("#")) {
                    try {
                        supportedTypes.add(getClassForName(type));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                View v = findViewIn(x, y);
                String event = "#view:";
                if (v != null) {
                    event += describeView(v, supportedTypes);
                }
                webSocket.send(event.getBytes());
            } else if (message.startsWith(commandSet)) {
                String msgData = message.substring(commandSet.length());
                String data[] = msgData.split(",");
                final String elementId = data[0];
                final String attribute = data[1];
                final String attributeType = data[2];
                final byte[] attributeValueBytes = Base64.decode(data[3], Base64.DEFAULT);
                final String attributeValue = new String(attributeValueBytes, "UTF-8");

                final View v = getRoot().findViewById(Integer.parseInt(elementId));
                if (v != null) {
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            setViewAttribute(v, attribute, attributeType, attributeValue);
                        }
                    });
                }
            }
        } catch(Exception exp) {
            exp.printStackTrace();
        }
    }
	
	private static Class getClassForName(String name) throws ClassNotFoundException {
        if (name.equals("byte")) return byte.class;
        if (name.equals("short")) return short.class;
        if (name.equals("int")) return int.class;
        if (name.equals("long")) return long.class;
        if (name.equals("char")) return char.class;
        if (name.equals("float")) return float.class;
        if (name.equals("double")) return double.class;
        if (name.equals("boolean")) return boolean.class;
        if (name.equals("void")) return void.class;

        return Class.forName(name);
	}

    private void setViewAttribute(View v, String attribute, String attributeType, String attributeValue) {
        // Hack for colors
        if (attribute.equals("BackgroundColor")) {
            if(attributeValue.equalsIgnoreCase("black")) {
                v.setBackgroundColor(v.getContext().getResources().getColor(Color.BLACK));
            }

            if(attributeValue.equalsIgnoreCase("red")) {
                v.setBackgroundColor(v.getContext().getResources().getColor(Color.RED));
            }

            if(attributeValue.equalsIgnoreCase("gray")) {
                v.setBackgroundColor(v.getContext().getResources().getColor(Color.GRAY));
            }

            sendScreenshot();return;
        }

        try {
            boolean processed = false;
            boolean executed = false;
            Class viewClass = v.getClass();
            String methodName = "set" + attribute;
            Method method = viewClass.getMethod(methodName, getClassForName(attributeType));
            Class[] paramTypes = method.getParameterTypes();
            if (method.getName().equals(methodName) && paramTypes.length == 1) {
                // Set value of view
                try {
                    Class paramType = paramTypes[0];
                    Object val = convertToType(attributeValue, paramType);

                    if (!(val == null && paramType.isPrimitive())) {
                        method.invoke(v, val);
                        executed = true;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                processed = true;
            }

            if (executed) {
                sendScreenshot();
            }
        }catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    private Object convertToType(String attributeValue, Class c) {
        if (c.isAssignableFrom(int.class)) {
            return (int)Integer.parseInt(attributeValue);
        } else if (c.isAssignableFrom(String.class)) {
            return attributeValue;
        }
        return null;
    }

    private String describeView(View v, Set<Class> supportedTypes) {
        Class viewClass = v.getClass();
        JSONObject description = new JSONObject();
        try {
            description.put("name", v.getId());
            description.put("class", viewClass.getName());

            // Get view attributes
            JSONObject attributes = null;
            if (viewAttributes.containsKey(viewClass)) {
                attributes = viewAttributes.get(viewClass);
            } else {
                // Generate view attributes
                attributes = new JSONObject();
                Method[] methods = viewClass.getMethods();
                for(int i = 0; i< methods.length; i++) {
                    Method method = methods[i];
                    Class[] paramTypes = method.getParameterTypes();
                    if (method.getName().startsWith("set") && paramTypes.length == 1) {
                        String attributeName = method.getName().substring(3);
                        Class paramType = paramTypes[0];

                        boolean supported = false;
                        for(Class c : supportedTypes) {
                            if (paramType.isAssignableFrom(c)) {
                                supported = true;
                                JSONObject attDetails = new JSONObject();
                                attDetails.put("type", paramType.getName());
                                attributes.put(attributeName, attDetails);
                                break;
                            }
                        }
                    }
                }
            }

            // Get values
            Iterator<String> itKey = attributes.keys();
            while(itKey.hasNext()) {
                String attributeName = itKey.next();
                try {
                    Method method = viewClass.getMethod("get" + attributeName, null);
                    if (method != null) {
                        Object ret = method.invoke(v, null);
                        if (ret != null) {
                            JSONObject attDetails = attributes.getJSONObject(attributeName);
                            attDetails.put("value", ret.toString());
                        }
                    }
                } catch(Exception exp) {
                    exp.printStackTrace();
                }
            }

            if (attributes != null) {
                description.put("attributes", attributes);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return description.toString();
    }

    private void getYOffset() {
        // Get status bar height
        Rect rectgle = new Rect();
        Window window = activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
        statusBarHeight = rectgle.top;
    }

    private ViewGroup getRoot() {
        return (ViewGroup)activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }
    private View findViewIn(int x, int y) {
        return findViewIn(getRoot(), x, y);
    }

    private View findViewIn(ViewGroup vg, int x, int y) {
        generateViewId(vg);

        // First children
        View result = null;
        int child = vg.getChildCount();
        for(int i = 0; i < child && (result == null); i++) {
            View children = vg.getChildAt(i);
            generateViewId(children);
            if (children instanceof ViewGroup) {
                result = findViewIn((ViewGroup)children, x, y);
            } else {
                if (viewIsIn(children, x, y)) {
                    result = children;
                }
            }
        }

        // Then itself
        if (result == null && viewIsIn(vg, x, y)) {
            result = vg;
        }

        return result;
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private void generateViewId(View v) {
        if (v.getId() < 0) {
            if (Build.VERSION.SDK_INT < 17) {
                for (;;) {
                    final int result = sNextGeneratedId.get();
                    int newValue = result + 1;
                    if (newValue > 0x00FFFFFF)
                        newValue = 1; // Roll over to 1, not 0.
                    if (sNextGeneratedId.compareAndSet(result, newValue)) {
                        v.setId(result);
                        return;
                    }
                }
            } else {
                try {
                    Method m = View.class.getMethod("generateViewId", null);
                    Object result = m.invoke(null, null);
                    v.setId(Integer.parseInt(result.toString()));
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean viewIsIn(View v, int x, int y) {
        int[] pos = new int[2];
        v.getLocationInWindow(pos);
        int vx = pos[0];
        int vy = pos[1];

        // Check top left
        if (x >= vx && y >= vy) {
            // Check width and height
            vx = vx + v.getWidth();
            vy = vy + v.getHeight();
            return (x <= vx && y <= vy);
        }

        return false;
    }
	
	private void sendScreenshot() {
		new Thread(new Runnable() {
            public void run() {
            	try {
	            	getYOffset();
					View content = activity.getWindow().getDecorView();
					content.setDrawingCacheEnabled(true);
				    Bitmap originalScreen = content.getDrawingCache();
				    
				    if (originalScreen != null) {
					    int y = statusBarHeight;
					    Bitmap finalBitmap = Bitmap.createBitmap(originalScreen, 0, y, originalScreen.getWidth(), originalScreen.getHeight() - y);
					    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
					    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
					    byte[] byteArray = byteArrayOutputStream .toByteArray();
                        if (webSocket != null) {
                            webSocket.send(byteArray);
                        }
				    }
			    } catch(Exception exp) {
			    	exp.printStackTrace();
			    }
            }
		}).start();
	}

	public void close() {
		timer.cancel();
        if (webSocket != null) {
            webSocket.close();
        }
	}
}
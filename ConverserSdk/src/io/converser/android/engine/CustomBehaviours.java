package io.converser.android.engine;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import io.converser.android.engine.DefaultBehaviours;

/**
 * @author Shane Moore
 *         <p/>
 *         This is the CustomBehaviours class. In certain cases, developers may
 *         want to change certain behaviours the sdk does. EG, what happens when
 *         a user clicks a call button or wants to visit an external website
 *         using a specific browser. This class should help you do that!
 *         <p/>
 *         To use this class, simply override any of the DefaultBehaviours
 *         methods and insert your own logic into the method body.
 *         <p/>
 *         Please note that if you change of the default behaviours then you
 *         must troubleshoot it alone.
 */
public class CustomBehaviours extends DefaultBehaviours {
    public CustomBehaviours(Activity a, Context c) {
        super(a, c);
    }

    // Sample of overriding a method
    @Override
    public void openDialer(Uri telUri, Activity activity) {
        // Some networking code here to tell another server a call was made.
        // Do some validation on the tel uri like only accepting numbers from a
        // certain area
        super.openDialer(telUri, activity);
    }
}

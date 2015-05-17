package org.stuartresearch.radio91x;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by jake on 5/16/15.
 */
public class NotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Drop user to MainActivity
        finish();
    }
}

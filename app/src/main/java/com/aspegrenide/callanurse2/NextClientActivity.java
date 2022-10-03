package com.aspegrenide.callanurse2;

import static com.aspegrenide.callanurse2.MainActivity.TOKEN_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.aspegrenide.callanurse2.GlassGestureDetector.Gesture;

public class NextClientActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener {
    private static final String TAG = "MAIN_ACT";
    private GlassGestureDetector glassGestureDetector;
    private String videoCallToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_client);

        glassGestureDetector = new GlassGestureDetector(this, this);

        // check if there is a known token to use for video call
        Bundle extras = getIntent().getExtras();
        Log.d(TAG, "extras = " + extras);
        if (extras != null) {
            videoCallToken = extras.getString(TOKEN_KEY);
            //The key argument here must match that used in the other activity
            Log.d(TAG, "videoCallToken received" + videoCallToken);
        }

    }

    @Override
    public boolean onGesture(Gesture gesture) {
        switch (gesture) {
            case TAP:
                Log.d(TAG, "tap");
                return true;
            case SWIPE_FORWARD:
                Log.d(TAG, "swipe forward");
                // open video
                callVideoActivity();
                return true;
            case SWIPE_BACKWARD:
                Log.d(TAG, "swipe backward");
                return true;
            case TWO_FINGER_SWIPE_DOWN:
                Log.d(TAG, "two finger swipe down");
                this.finish();
                return true;
            default:
                return false;
        }
    }

    private void callVideoActivity() {
        Log.d(TAG, "call video activity with token as extras");
        Intent intent = new Intent(NextClientActivity.this, VideoActivity.class);
        intent.putExtra(TOKEN_KEY, videoCallToken);
        startActivity(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (glassGestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }


}

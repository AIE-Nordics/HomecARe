package com.aspegrenide.callanurse2;

import static com.aspegrenide.callanurse2.MainActivity.TOKEN_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aspegrenide.callanurse2.GlassGestureDetector.Gesture;

public class NextClientActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener {
    private static final String TAG = "MAIN_ACT";
    private GlassGestureDetector glassGestureDetector;
    private String videoCallToken;

    private TextView tvClientName;
    private TextView tvTask;
    private TextView tvTaskWhen;
    private ImageView imgClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_client);

        tvClientName = findViewById(R.id.tvClientName);
        tvTask = findViewById(R.id.tvTask);
        tvTaskWhen = findViewById(R.id.tvTaskWhen);
        imgClient = findViewById(R.id.imgClient);

        tvClientName.setText("Ingrid Pålsson");
        tvTaskWhen.setText("12:15, om tio minuter");
        tvTask.setText("Laga lunch och ta medicin [tap for list]");
        imgClient.setImageResource(R.drawable.oldwoman1);

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
                // open make note
                callMakeNoteActivity();
                return true;
            case SWIPE_BACKWARD:
                Log.d(TAG, "swipe backward");
                // go back to main
                callMainActivity();
                return true;
            case SWIPE_DOWN:
                Log.d(TAG, "swipe down");
                this.finish();
                return true;
            default:
                return false;
        }
    }

    private void callMainActivity() {
        Log.d(TAG, "call main activity with token as extras");
        Intent intent = new Intent(NextClientActivity.this, MainActivity.class);
        intent.putExtra(TOKEN_KEY, videoCallToken);
        startActivity(intent);
    }

    private void callMakeNoteActivity() {
        Log.d(TAG, "call next client activity");
        Intent intent = new Intent(NextClientActivity.this, MakeNoteActivity.class);
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

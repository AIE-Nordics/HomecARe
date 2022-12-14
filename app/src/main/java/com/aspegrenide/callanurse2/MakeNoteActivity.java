package com.aspegrenide.callanurse2;

import static com.aspegrenide.callanurse2.MainActivity.TOKEN_KEY;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/*
Make a note using voice recognition
https://medium.com/voice-tech-podcast/android-speech-to-text-tutorial-8f6fa71606ac

*/

public class MakeNoteActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener{
    // used to handle token in intents
    public static final String TOKEN_KEY = "TOKEN_KEY";
    private String videoCallToken;
    private static final String TAG = "MAIN_ACT";

    private static final int REQUEST_CODE = 999;
    public static final Integer RecordAudioRequestCode = 1;

    private GlassGestureDetector glassGestureDetector;
    private TextView tvSavedNotes;
    private TextView tvNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_note);

        tvNote = findViewById(R.id.tvNote);
        tvSavedNotes = findViewById(R.id.tvSavedNotes);

        Log.d(TAG, "Prepare for gestures");
        glassGestureDetector = new GlassGestureDetector(this, this);

        // check if there is a known token to use for video call
        Bundle extras = getIntent().getExtras();
        Log.d(TAG, "extras = " + extras);
        if (extras != null) {
            videoCallToken = extras.getString(TOKEN_KEY);
            //The key argument here must match that used in the other activity
            Log.d(TAG, "videoCallToken received" + videoCallToken);
        }

        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            final List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d(TAG, "results: " + results.toString());
            if (results != null && results.size() > 0 && !results.get(0).isEmpty()) {
                updateUI(results.get(0));
            }
        } else {
            Log.d(TAG, "Result not OK");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return glassGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        Log.d(TAG, "onGesture");
        switch (gesture) {
            case TAP_AND_HOLD:
                Log.d(TAG, "tap and hold");
                saveNote();
                return true;
            case TAP:
                Log.d(TAG, "tap");
                //toggleRecording();
                requestVoiceRecognition();
                return true;
            case SWIPE_FORWARD:
                Log.d(TAG, "swipe forward");
                // open video
                callVideoActivity();
                return true;
            case SWIPE_BACKWARD:
                Log.d(TAG, "swipe backward");
                callNextClientActivity();
                return true;
            case SWIPE_DOWN:
                Log.d(TAG, "swipe down");
                this.finish();
                return true;
            default:
                return false;
        }
    }

    private void saveNote() {
        Toast.makeText(this, "Sparar notering: " + tvNote.getText(), Toast.LENGTH_SHORT).show();

        CharSequence org = tvSavedNotes.getText();
        tvSavedNotes.setText(tvNote.getText() + " : " + org);
        tvNote.setText("");
    }


    private void requestVoiceRecognition() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv_SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv_SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        startActivityForResult(intent, REQUEST_CODE);
    }

    private void updateUI(String result) {
        tvNote.setText(result);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }


    private void callNextClientActivity() {
        Log.d(TAG, "call next client activity");
        Intent intent = new Intent(MakeNoteActivity.this, NextClientActivity.class);
        intent.putExtra(TOKEN_KEY, videoCallToken);
        startActivity(intent);
    }

    // swipe forward opens video, provide the token to videocall as well
    private void callVideoActivity() {
        Log.d(TAG, "call video activity with token: " + videoCallToken);
        Intent intent = new Intent(MakeNoteActivity.this, VideoActivity.class);
        intent.putExtra(TOKEN_KEY, videoCallToken);
        startActivity(intent);
    }
}


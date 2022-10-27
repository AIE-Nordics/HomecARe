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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

    public static final Integer RecordAudioRequestCode = 1;
    private boolean RECORDING_ACTIVE = false;
    private SpeechRecognizer speechRecognizer;
    final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    private GlassGestureDetector glassGestureDetector;

    private ImageView imgMic;
    private TextView tvRecordingStatus;
    private TextView tvNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_note);

        imgMic = findViewById(R.id.imgMic);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);
        tvNote = findViewById(R.id.tvNote);

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

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech");
                tvNote.setText("");
                tvRecordingStatus.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {
                //Log.d(TAG, "onRmsChanged");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Log.d(TAG, "onBufferReceived");

            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech");
                imgMic.setImageResource(R.drawable.mic_off);
                tvRecordingStatus.setText("Processing...");

            }

            @Override
            public void onError(int i) {
                Log.d(TAG, "onError " + i);
                tvRecordingStatus.setText("Tap to record");
                tvNote.setText("Not recognized");

            }

            @Override
            public void onResults(Bundle bundle) {
                Log.d(TAG, "onResults");
                imgMic.setImageResource(R.drawable.mic_off);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                tvNote.setText(data.get(0));
                Log.d(TAG, "data" + data);
                tvRecordingStatus.setText("Tap to record");
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.d(TAG, "onPartialResults");

            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.d(TAG, "onEvent");

            }
        });
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
                toggleRecording();
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
        Toast.makeText(this, "Saving note: " + tvNote.getText(), Toast.LENGTH_LONG).show();
    }

    private void toggleRecording() {
        Log.d(TAG, "toggleRecording");
        if(RECORDING_ACTIVE) {
            Log.d(TAG, "is recording, deactivate");
            // is recording, deactivate
            speechRecognizer.stopListening();
            RECORDING_ACTIVE = false;
        } else {
            Log.d(TAG, "startListening");
            speechRecognizer.startListening(speechRecognizerIntent);
            imgMic.setImageResource(R.drawable.microphone);
            tvRecordingStatus.setText("Listening...");

        }
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


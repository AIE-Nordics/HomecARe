package com.aspegrenide.callanurse2;

import static com.aspegrenide.callanurse2.MainActivity.TOKEN_KEY;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.aspegrenide.callanurse2.GlassGestureDetector.Gesture;

public class VideoActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener {
    private static final String TAG = "VIDEO_ACT";
    private GlassGestureDetector glassGestureDetector;

    // Fill the App ID of your project generated on Agora Console.
    private String appId = "d8378ebda07041e98ac4cbe08e41d04e";
    // Fill the channel name.
    private String channelName = "callanurse2";
    // Fill the temp token generated on Agora Console.
    private String videoCallToken = null;  //"007eJxTYLgsdtP2eOL0F0cumv9P2yXHk/tf3jHTNrH2zg1vvr5L6XwKDCkWxuYWqUkpiQbmBiaGqZYWickmyUmpBhapJoYpBiap2nWWyavOWyXffrWeiZEBAkF8bobkxJycxLzSouJUIwYGAL7QJZ0=";

    private RtcEngine mRtcEngine;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        // check if there is a known token to use for video call
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            videoCallToken = extras.getString(TOKEN_KEY);
            //The key argument here must match that used in the other activity
            Log.d(TAG, "videoCallToken received" + videoCallToken);
        }


        Log.d(TAG, "=========== + + + onCreate + + + ===============");
        glassGestureDetector = new GlassGestureDetector(this, this);

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        initializeAndJoinChannel();
    }

    @Override
    public boolean onGesture(Gesture gesture) {
        switch (gesture) {
            case TAP:
                Log.d(TAG, "tap");
                initializeAndJoinChannel();
                return true;
            case SWIPE_FORWARD:
                Log.d(TAG, "swipe forward");
                return true;
            case SWIPE_BACKWARD:
                Log.d(TAG, "swipe backward");
                leave();
                callNextClientActivity();
               // this.finish();
                return true;
            default:
                return false;
        }
    }

    private void callNextClientActivity() {
        Log.d(TAG, "call next client activity");
        Intent intent = new Intent(VideoActivity.this, NextClientActivity.class);
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

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the remote host joining the channel to get the uid of the host.
        public void onUserJoined(int uid, int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Call setupRemoteVideo to set the remote video view after getting uid from the onUserJoined callback.
                setupRemoteVideo(uid);
            }
        });
        }
    };

    private void initializeAndJoinChannel() {
        Log.d(TAG, "enter function initializeAndJoinChannel");

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
        }else{
            Toast.makeText(this, "Could not start videostream", Toast.LENGTH_LONG).show();
            return;
        }
        //already started? return
        Log.d(TAG, "already started, in that case return. Check mRtcEngine(" + mRtcEngine + ")");
        if(mRtcEngine != null) {
            Toast.makeText(this, "Already started", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "not started it seems, create!");
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Check the error.");
        }
        // By default, video is disabled, and you need to call enableVideo to start a video stream.
        mRtcEngine.enableVideo();
        // Start local preview.
        mRtcEngine.startPreview();

        Log.d(TAG, "start local preview");

        FrameLayout container = findViewById(R.id.local_video_view_container);
        // Ceate a SurfaceView object and add it as a child to the FrameLayout.
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        // Pass the SurfaceView object to Agora so that it renders the local video.
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));

        ChannelMediaOptions options = new ChannelMediaOptions();
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        // For a video call scenario, set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        mRtcEngine.joinChannel(videoCallToken, channelName, 0, options);
    }

    protected void onDestroy() {
        super.onDestroy();
        leave();
    }

    private void leave() {
        Log.d(TAG, "init leave");
        //already started? return
        if(mRtcEngine == null) {
            Toast.makeText(this, "Already stopped", Toast.LENGTH_SHORT).show();
            return;
        }

        mRtcEngine.stopPreview();
        mRtcEngine.leaveChannel();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            mRtcEngine = null;
        }).start();
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }
}

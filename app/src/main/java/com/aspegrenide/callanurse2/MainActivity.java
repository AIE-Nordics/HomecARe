package com.aspegrenide.callanurse2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

/*
This client runs on the Google Glass. It connects
and access services on a mobile phone otherwise unavailable
1) the glass and the hansdset must be paired
2) connect to the handset
3) send serial
*/

public class MainActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener{
    // used to handle token in intents
    public static final String TOKEN_KEY = "TOKEN_KEY";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final int REQUEST_BT_CONNECT_PERMISSION = 3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MAIN_ACT";
    private static final int DEACTIVE = 0;
    private static final int ACTIVE = 1;
    private static final int CONNECTED = 2;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ConnectedThread mConnectedThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;

    private String videoCallToken;

    ImageView imgBtStatus;
    TextView tvBtState;
    ImageView imgServerState;
    TextView tvServerState;

    private GlassGestureDetector glassGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Prepare for gestures");
        glassGestureDetector = new GlassGestureDetector(this, this);

        imgBtStatus = findViewById(R.id.imgBtStatus);
        tvBtState = findViewById(R.id.tvBt);
        imgServerState = findViewById(R.id.imgServerState);
        tvServerState = findViewById(R.id.tvServer);

        // check BT
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (bluetoothAdapter.isEnabled()) {
            imgBtStatus.setImageResource(R.drawable.bton);
            tvBtState.setText("Active");
        }

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        // update the icon for server
        setServerState(DEACTIVE, null);

        setupConnection();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return glassGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        Log.d(TAG, "tap");
        switch (gesture) {
            case TAP:
                Log.d(TAG, "tap");
                return true;
            case SWIPE_FORWARD:
                Log.d(TAG, "swipe forward");
                // open video
                callNextClientActivity();
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

    // swipe forward opens NextClient, provide the token to videocall as well
    private void callNextClientActivity() {
        Log.d(TAG, "call video activity with token: " + videoCallToken);
        Intent intent = new Intent(MainActivity.this, NextClientActivity.class);
        intent.putExtra(TOKEN_KEY, videoCallToken);
        startActivity(intent);
    }

    //web // 007eJxTYOCb4yO7LL71UE6ZbeDCaZ+X9IcLXgvfUsXC+LFXxfvBTlUFhhQLY3OL1KSURANzAxPDVEuLxGST5KRUA4tUE8MUA5NUfk/r5ImzbJKnOHAwMzJAIIjPzeCcmJOTmFdaVJxqxMAAAOW5IbE=
    //log // 007eJxTYOCb4yO7LL71UE6ZbeDCaZ+X9IcLXgvfUsXC+LFXxfvBTlUFhhQLY3OL1KSURANzAxPDVEuLxGST5KRUA4tUE8MUA5NUfk/r5ImzbJKnOHAwMzJAIIjPzeCcmJOTmFdaVJxqxMAAAOW5IbE=
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        tvBtState.setText("Deactive");
                        imgBtStatus.setImageResource(R.drawable.btoff);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        tvBtState.setText("Deactivating ...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        tvBtState.setText("Active");
                        imgBtStatus.setImageResource(R.drawable.bton);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        tvBtState.setText("Activating ...");
                        break;
                }
            }
        }
    };

    private void setServerState(int state, String deviceName) {
        switch (state) {
            case DEACTIVE:
                tvServerState.setText("Not active");
                imgServerState.setImageResource(R.drawable.shh);
                break;
            case ACTIVE:
                tvServerState.setText("Active");
                imgServerState.setImageResource(R.drawable.ok);
                break;
            case CONNECTED:
                imgServerState.setImageResource(R.drawable.handshake);
                tvServerState.setText("Connected to " + deviceName);
                break;
        }
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private void setupConnection() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.e(TAG, "" + pairedDevices.size());
        if (pairedDevices.size() > 0) {
            Object[] devices = pairedDevices.toArray();
            BluetoothDevice device = (BluetoothDevice) devices[0];
            Log.e(TAG, "PAIRED WITH " + device.getName());

            ConnectThread connect = new ConnectThread(device, MY_UUID_INSECURE);
            connect.start();
        }
    }

    public void sendMessage(String message) {
        byte[] bytes = message.getBytes(Charset.defaultCharset());
        mConnectedThread.write(bytes);
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
        }
        String deviceName = mmSocket.getRemoteDevice().getName();
        String serial = mmSocket.getRemoteDevice().toString();
        Log.d(TAG, "remotedevice to string:" + serial);
        // looking for , deviceName

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setServerState(CONNECTED, deviceName);
            }
        });

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void btnSendOnClick(View view) {
        sendMessage("hi");
    }

    public void btnRestartOnClick(View view) {
        setServerState(DEACTIVE, null);
        mConnectedThread.cancel();
        setupConnection();
    }


    // Kanske inte behövs
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null ;
            // Create a new listening server socket
            try{
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
                }
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }
            mmServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;

            try{
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setServerState(ACTIVE, null);
                    }
                });
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            //talk about this is in the 3rd
            if(socket != null){
                connected(socket);
            }
            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
                }
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }

            //will talk about this in the 3rd video
            connected(mmSocket);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    handleInput(incomingMessage);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // view_data.setText(incomingMessage);
                            Log.d(TAG, "incomingMessage: " + incomingMessage);
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // check if it is a token to the videocall and extract it
    private void handleInput(String incomingMessage) {
        if(incomingMessage.startsWith("TOKEN")){
            videoCallToken = incomingMessage.substring(5);
            Log.d(TAG, "handleInput: " + videoCallToken);

            this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "token found: " + videoCallToken, Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
}
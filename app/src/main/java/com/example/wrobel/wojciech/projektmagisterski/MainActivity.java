package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.logging.StreamHandler;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 100;
    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private TextView mConnectionStatus;
    private TextView mRPM_TV;
    private TextView mSpeed_TV;
    protected static BluetoothIO mBluetoothIO;
    protected static BluetoothSocket mBluetoothSocket;

    // Message types accessed from the BluetoothIO Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Helpers ids for Handler
    public static final int RPM = 10;
    public static final int SPEED = 11;

    // Key names accesses from the BluetoothIO Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast_message";
    public static final String FORMATED_VALUE = "value_message";

    private final Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case BluetoothIO.STATE_CONNECTING:
                        mConnectionStatus.setText(getString(R.string.connecting));
                        mConnectionStatus.setBackgroundColor(Color.YELLOW);
                        break;

                        case BluetoothIO.STATE_CONNECTED:
                            mConnectionStatus.setText(R.string.status_connected + " " + mConnectedDeviceName);
                            mConnectionStatus.setBackgroundColor(Color.GREEN);
//                            sendDefaultCommands();
                            break;

                        case BluetoothIO.STATE_LISTEN:
                        case BluetoothIO.STATE_NONE:
                            mConnectionStatus.setText(R.string.not_connected);
                            mConnectionStatus.setBackgroundColor(Color.RED);
                        default:
                            break;
                    }
                    break;

                case MESSAGE_TOAST:
                    String messageToShow = msg.getData().getString(TOAST);
                    Toast.makeText(MainActivity.this, messageToShow, Toast.LENGTH_SHORT).show();
                case MESSAGE_READ:
                    switch (msg.arg1) {
                        case RPM:
                            String rpm = msg.getData().getString(FORMATED_VALUE);
                            mRPM_TV.setText(rpm);
                            break;
                        case SPEED:
                            String speed = msg.getData().getString(FORMATED_VALUE);
                            mSpeed_TV.setText(speed);
                    }
                    break;
            }
        }
    };
    private String mConnectedDeviceName;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                Intent scanForDevices = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(scanForDevices, REQUEST_CONNECT_DEVICE);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        if(null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, CameraFragment.newInstance()).commit();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast toast =  Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        mRPM_TV = findViewById(R.id.RPM_textView);
        mSpeed_TV = findViewById(R.id.speed_tv);
        mConnectionStatus = findViewById(R.id.connectionState);

        startBluetoothIO();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String text;
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Toast toast = Toast.makeText(this, "Got device with address: " + address, Toast.LENGTH_LONG);
                    toast.show();

                    BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                    mConnectedDeviceName = mBluetoothDevice.getName();
                    mBluetoothIO.connect(mBluetoothDevice);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    text = "Bluetooth turned on";
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                } else if(resultCode == RESULT_CANCELED) {
                    text = "Bluetooth must be turned on to use the app";
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    private void startBluetoothIO() {
        // Start BluetoothIO
        if (mBluetoothIO == null) {
            mBluetoothIO = new BluetoothIO(this, mMsgHandler);
        }
        // STATE_NONE only if Bluetooth not started yet
        if (mBluetoothIO.getState() == BluetoothIO.STATE_NONE) {
            mBluetoothIO.start();
        }
    }

    private void enableBT(){
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        enableBT();
    }
}

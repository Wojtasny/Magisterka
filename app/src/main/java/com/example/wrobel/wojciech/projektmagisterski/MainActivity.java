package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.enums.AvailableCommandNames;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int REQUEST_ENABLE_BT = 100;

    private static Context appContext;
    private BluetoothAdapter mBluetoothAdapter;
    static BluetoothIO mBluetoothIO;

    // Message types accessed from the BluetoothIO Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Helpers ids for Handler
    public static final String FORMATTED_VALUE = "value_message";
    public static final String FORMATTED_VALUE_CLASS_NAME = "value_class_name";
    private TextView mUpdateTV;
    private LinearLayout mControlSection;
    private LinearLayout mEngineSection;
    private LinearLayout mFuelSection;
    private LinearLayout mPressureSection;
    private LinearLayout mTemperatureSection;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    String address = BTProperties.getInstance().getBTAddress();
                    String name = BTProperties.getInstance().getBTName();
                    Toast.makeText(this, "Trying to connect to: " + name, Toast.LENGTH_SHORT).show();
                    BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                    mBluetoothIO.connect(mBluetoothDevice);
                } else {
                    Toast.makeText(this, "Something went wrong, try to choose device again", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    Toast.makeText(this, "Bluetooth turned on", Toast.LENGTH_SHORT).show();
                } else if(resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Bluetooth must be turned on to use the app", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    public static class MyMsgHandler extends Handler {
        private final WeakReference<MainActivity> mTarget;

        MyMsgHandler(MainActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity target = mTarget.get();
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothIO.STATE_CONNECTING:
                            // Connecting
                            break;

                        case BluetoothIO.STATE_CONNECTED:
                            // Connected
                            break;

                        case BluetoothIO.STATE_LISTEN:
                        case BluetoothIO.STATE_NONE:

                        default:
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    String value = msg.getData().getString(FORMATTED_VALUE);
                    String val = value.equals("") ? value : "No Data";
                    String valueClassName = msg.getData().getString(FORMATTED_VALUE_CLASS_NAME);
                    String actualClassName = getCommandClass(valueClassName);
                    Log.d(TAG, "handleMessage: "+ value + " " + actualClassName);
                    target.updateTV(value, actualClassName);
                    break;
            }
        }

        private String getCommandClass(String valueClassName) {
            for(AvailableCommandNames commandNames: AvailableCommandNames.values()) {
                Log.d(TAG, "getCommandClass: "+ commandNames.getValue()+" and name "+ commandNames.name());
                if(commandNames.getValue().equals(valueClassName)){
                    return commandNames.name();
                }
            }
            return null;
        }
    }

    public void updateTV(String val, String className){
        int resID = appContext.getResources().getIdentifier(className, "id", appContext.getPackageName());
        Log.d(TAG, "updateTV: " + resID);
        if(resID != 0) {
            runOnUiThread(() -> {
                mUpdateTV = (TextView)findViewById(resID);
                mUpdateTV.setText(val);
            });
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        setContentView(R.layout.activity_main);
//    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onNavigationItemSelected: ");

        if (id == R.id.nav_camera) {
            Intent cameraActivity = new Intent(this, CameraActivity.class);
            startActivity(cameraActivity);
        } else if (id == R.id.action_connect_bt) {
            Log.d(TAG, "onNavigationItemSelected: in bluetooth");
            Intent scanForDevices = new Intent(this, DeviceListActivity.class);
            startActivityForResult(scanForDevices, REQUEST_CONNECT_DEVICE);

        } else if (id == R.id.nav_manage) {
            // empty
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        appContext = getApplicationContext();
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);


        MyMsgHandler mMsgHandler = new MyMsgHandler(this);

        // Start BluetoothIO
        if(mBluetoothIO == null) {
            mBluetoothIO = new BluetoothIO(this, mMsgHandler);
        }

        // STATE_NONE only if Bluetooth not yet started
        if(mBluetoothIO.getState() == BluetoothIO.STATE_NONE) {
            mBluetoothIO.start();
        }
        mControlSection = (LinearLayout) findViewById(R.id.control_section_readings);
        mEngineSection = (LinearLayout) findViewById(R.id.engine_section_readings);
        mFuelSection = (LinearLayout) findViewById(R.id.fuel_section_readings);
        mPressureSection = (LinearLayout) findViewById(R.id.pressure_section_readings);
        mTemperatureSection = (LinearLayout) findViewById(R.id.temperature_section_readings);

    }

    public void toggle_control_readings (View view){
        mControlSection.setVisibility(mControlSection.isShown() ? View.GONE : View.VISIBLE);
    }

    public void toggle_engine_readings (View view){
        mEngineSection.setVisibility(mEngineSection.isShown() ? View.GONE : View.VISIBLE);
    }

    public void toggle_fuel_readings (View view){
        mFuelSection.setVisibility(mFuelSection.isShown() ? View.GONE : View.VISIBLE);
    }

    public void toggle_pressure_readings (View view){
        mPressureSection.setVisibility(mPressureSection.isShown() ? View.GONE : View.VISIBLE);
    }
    public void toggle_temperature_readings (View view){
        mTemperatureSection.setVisibility(mTemperatureSection.isShown() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
//        if(!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "App can't run without camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

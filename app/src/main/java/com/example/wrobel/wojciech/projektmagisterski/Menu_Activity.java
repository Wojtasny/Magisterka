package com.example.wrobel.wojciech.projektmagisterski;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.enums.AvailableCommandNames;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Menu_Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int REQUEST_ENABLE_BT = 100;
    private static final String TAG = "Menu_Activity";

    private static Context appContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothIO mBluetoothIO;
    private TextureView mTextureView;

    // Message types accessed from the BluetoothIO Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Helpers ids for Handler
    public static final String FORMATTED_VALUE = "value_message";
    public static final String FORMATTED_VALUE_CLASS_NAME = "value_class_name";
    public static final int RPM = 10;
    public static final int SPEED = 11;
    private DrawerLayout mDrawerLayout;
    private TextView mUpdateTV;
    private LinearLayout mControlSection;
    private LinearLayout mEngineSection;
    private LinearLayout mFuelSection;
    private LinearLayout mPressureSection;
    private LinearLayout mTemperatureSection;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Toast.makeText(getApplicationContext(), "surfaceListener", Toast.LENGTH_SHORT).show();
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: tutaj");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: tutaj");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: tutaj");
        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private String mCameraId;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0,0);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,0);
        ORIENTATIONS.append(Surface.ROTATION_270,0);
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * rhs.getHeight() /
                    (long) rhs.getWidth() * lhs.getHeight());
        }
    }

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
        private final WeakReference<Menu_Activity> mTarget;

        MyMsgHandler(Menu_Activity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            Menu_Activity target = mTarget.get();
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
//                    switch (msg.arg1) {
//                        case RPM:
////                            String rpm = msg.getData().getString(FORMATED_VALUE);
////                            mRPM_TV.setText(rpm);
//                            break;
//                        case SPEED:
////                            String speed = msg.getData().getString(FORMATED_VALUE);
////                            mSpeed_TV.setText(speed);
//                    }
//                    break;
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

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        setContentView(R.layout.activity_menu);
        if(mTextureView.isAvailable()) {
            Log.d(TAG, "onResume:  byłem tutaj");
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();
//        setContentView(R.layout.activity_menu);
        setContentView(R.layout.content_menu_);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace start of the whole bloody thing", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        mDrawerLayout.addDrawerListener(toggle);
//        toggle.syncState();
//
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);

        MyMsgHandler mMsgHandler = new MyMsgHandler(this);

        // Start BluetoothIO
        if(mBluetoothIO == null) {
            mBluetoothIO = new BluetoothIO(this, mMsgHandler);
        }

        // STATE_NONE only if Bluetooth not yet started
        if(mBluetoothIO.getState() == BluetoothIO.STATE_NONE) {
            mBluetoothIO.start();
        }
//        mControlSection = (LinearLayout)findViewById(R.id.control_section_readings);
//        mControlSection.setVisibility(View.GONE);
//        mEngineSection = (LinearLayout) findViewById(R.id.engine_section_readings);
//        mEngineSection.setVisibility(View.GONE);
//        mFuelSection = (LinearLayout) findViewById(R.id.fuel_section_readings);
//        mFuelSection.setVisibility(View.GONE);
//        mPressureSection = (LinearLayout) findViewById(R.id.pressure_section_readings);
//        mPressureSection.setVisibility(View.GONE);
//        mTemperatureSection = (LinearLayout) findViewById(R.id.temperature_section_readings);
//        mTemperatureSection.setVisibility(View.GONE);
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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_bluetooth) {
            Intent scanForDevices = new Intent(this, DeviceListActivity.class);
            startActivityForResult(scanForDevices, REQUEST_CONNECT_DEVICE);

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraID;
                return;
            }
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
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

    private void connectCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "App requires access to camera", Toast.LENGTH_LONG).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getApplicationContext(),
                                    "Something went wrong, unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }
    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option: choices) {
            if(option.getHeight() == option.getWidth()* height /width &&
                option.getWidth()>= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
}

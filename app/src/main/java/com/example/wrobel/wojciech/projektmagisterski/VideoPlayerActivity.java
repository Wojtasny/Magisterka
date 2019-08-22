package com.example.wrobel.wojciech.projektmagisterski;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.github.pires.obd.enums.AvailableCommandNames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "VideoPlayerActivity";
    VideoView mVideoView;
    String mVideoPath;
    String mReadingsPath;
    float percentage;
    private TextView mUpdateTV;
    private ArrayList<String> mReadings = new ArrayList<>();
    private static Context appContext;

    // Helpers ids for Handler
    public static final String FORMATTED_VALUE = "value_message";
    public static final String FORMATTED_VALUE_CLASS_NAME = "value_class_name";
    public static final int MESSAGE_READ = 2;

    private ReaderThread mReaderThread;
    private File readingsFile;
    private MyMultiOBDCommand myMultiOBDCommand;
    private LinearLayout mControlSection;
    private LinearLayout mEngineSection;
    private LinearLayout mFuelSection;
    private LinearLayout mPressureSection;
    private LinearLayout mTemperatureSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        appContext = getApplicationContext();
        Bundle b = getIntent().getExtras();
        assert b != null;
        mVideoPath = b.getString("path");
        assert mVideoPath != null;
        mReadingsPath = mVideoPath.replace("VIDEO","READINGS").replace("mp4","txt");
        readingsFile = new File(mReadingsPath);

        ReadingsHandler readingsHandler = new ReadingsHandler(this);
        myMultiOBDCommand = new MyMultiOBDCommand(readingsHandler);
        mVideoView = (VideoView)findViewById(R.id.videoView);
        readFileToMemory();
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideoView);
        Uri uri = Uri.parse(mVideoPath);
        mVideoView.setMediaController(mediaController);
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();
        mVideoView.start();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mReaderThread = new ReaderThread();
                mReaderThread.start();
            }
        });
        mControlSection = (LinearLayout) findViewById(R.id.control_section_readings);
        mEngineSection = (LinearLayout) findViewById(R.id.engine_section_readings);
        mFuelSection = (LinearLayout) findViewById(R.id.fuel_section_readings);
        mPressureSection = (LinearLayout) findViewById(R.id.pressure_section_readings);
        mTemperatureSection = (LinearLayout) findViewById(R.id.temperature_section_readings);
    }

    private void readFileToMemory() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(mReadingsPath));
            String line;
            while((line = br.readLine()) != null){
                mReadings.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        if(mReaderThread != null) {
            mReaderThread.interrupt();
        }
        super.onPause();
    }

    private class ReaderThread extends Thread {
        final float duration = mVideoView.getDuration();
        float currentPosition;

        public void run() {
            while (true) {
                currentPosition = mVideoView.getCurrentPosition();
                percentage = (currentPosition/duration);
                Log.d(TAG, "run: "+ percentage + "%");
                myMultiOBDCommand.parseReadings(mReadings.get((int)(mReadings.size()*percentage)));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    public static class ReadingsHandler extends Handler {
        private final WeakReference<VideoPlayerActivity> mTarget;

        ReadingsHandler(VideoPlayerActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoPlayerActivity target = mTarget.get();
            if(msg.what == MESSAGE_READ) {
                String value = msg.getData().getString(FORMATTED_VALUE);
                String valueClassName = msg.getData().getString(FORMATTED_VALUE_CLASS_NAME);
                String actualClassName = getCommandClass(valueClassName);
                Log.d(TAG, "handleMessage: "+ value + " " + actualClassName);
                target.updateTV(value, actualClassName);
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
}

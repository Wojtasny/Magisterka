package com.example.wrobel.wojciech.projektmagisterski;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.enums.AvailableCommandNames;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoGalleryActivity extends Activity {


    public static final String TAG = "VIDEO_GALLERY_ACTIVITY";
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERAMISSION_RESULT = 1;

    private List<File> mVideoList = new ArrayList<>();
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_gallery);
        checkReadExternalStoragePermission();
        getVideosFromStorage();

        mListView = (ListView)findViewById(R.id.video_list_view);
        mListView.setAdapter(new MyThumbnailAdapter(VideoGalleryActivity.this, R.layout.video_row, mVideoList));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(VideoGalleryActivity.this, VideoPlayerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("path", (mVideoList.get(position)).getAbsolutePath());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }


    private void getVideosFromStorage() {
        if(!mVideoList.isEmpty()) {
            mVideoList.clear();
        }
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()+"/ProjektMagisterski";
        File directory = new File(path);
        //apply a filter
        File [] files = directory.listFiles((dir, name) -> {
            String [] absPath = name.split("/");
            String actualName = absPath[absPath.length-1];
            return actualName.startsWith("VIDEO_") && actualName.endsWith(".mp4");
                });


        mVideoList.addAll(Arrays.asList(files));
        Log.d(TAG, "onCreate: "+ mVideoList);
    }

    private void checkReadExternalStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED) {
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "app needs to be able to read videos", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERAMISSION_RESULT);
        }

    }

    public class MyThumbnailAdapter extends ArrayAdapter<File> {

        public MyThumbnailAdapter(Context context, int textViewResourceId,
                                  List<File> listOfVideos) {
            super(context, textViewResourceId, listOfVideos);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(row==null){
                LayoutInflater inflater=getLayoutInflater();
                row=inflater.inflate(R.layout.video_row, parent, false);
            }

            TextView textfilePath = (TextView)row.findViewById(R.id.FilePath);
            textfilePath.setText((mVideoList.get(position)).getName());
            ImageView imageThumbnail = (ImageView)row.findViewById(R.id.Thumbnail);

            Bitmap bmThumbnail;
            bmThumbnail = (Bitmap) ThumbnailUtils.createVideoThumbnail((mVideoList.get(position)).getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
            imageThumbnail.setImageBitmap(bmThumbnail);

            return row;
        }
    }
}

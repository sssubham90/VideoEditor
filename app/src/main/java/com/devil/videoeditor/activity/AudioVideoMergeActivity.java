package com.devil.videoeditor.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import com.devil.videoeditor.R;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioVideoMergeActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_GALLERY_AUDIO = 100;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 200;
    private VideoView videoView;
    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;
    private static final String TAG = "DEVIL";
    private static final String FILEPATH = "filepath";
    private Uri selectedAudioUri;
    private Uri selectedVideoUri;
    private String filePath;
    private CheckBox choice;
    private ScrollView mainlayout;
    private MediaController controller;
    private int stopPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_video_merge);
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
        TextView uploadAudio = findViewById(R.id.uploadAudio);
        TextView uploadVideo = findViewById(R.id.uploadVideo);
        TextView merge = findViewById(R.id.merge);
        videoView = findViewById(R.id.videoView);
        mainlayout = findViewById(R.id.mainlayout);
        choice = findViewById(R.id.choice);
        mainlayout = findViewById(R.id.mainlayout);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        loadFFMpegBinary();

        uploadAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23)
                    getAudioPermission();
                else
                    uploadAudio();

            }
        });
        uploadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23)
                    getVideoPermission();
                else
                    uploadVideo();

            }
        });
        merge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedAudioUri == null )
                    Snackbar.make(mainlayout, "Please upload a audio", 4000).show();
                else if(selectedVideoUri == null)
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
                else
                    mergeAudioVideo();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPosition = videoView.getCurrentPosition(); //stopPosition is an int
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.seekTo(stopPosition);
        videoView.start();
    }

    private void getAudioPermission() {
        String[] params = null;
        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);
        List<String> permissions = new ArrayList<>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(writeExternalStorage);
        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(readExternalStorage);

        if (!permissions.isEmpty()) {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0) {
            ActivityCompat.requestPermissions(AudioVideoMergeActivity.this,
                    params,
                    100);
        }
        else{
            uploadAudio();
        }
    }

    private void getVideoPermission() {
        String[] params = null;
        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);
        List<String> permissions = new ArrayList<>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(writeExternalStorage);
        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(readExternalStorage);

        if (!permissions.isEmpty()) {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0) {
            ActivityCompat.requestPermissions(AudioVideoMergeActivity.this,
                    params,
                    200);
        }
        else{
            uploadVideo();
        }
    }

    /**
     * Handling response for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    uploadAudio();
                }
            }
            break;
            case 200: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    uploadVideo();
                }
            }
            break;
        }
    }

    /**
     * Opening gallery for uploading video and audio
     */
    private void uploadAudio() {
        try {
            Intent intent = new Intent();
            intent.setType("audio/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQUEST_TAKE_GALLERY_AUDIO);
        } catch (Exception ignored) {

        }
    }

    private void uploadVideo() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_AUDIO) {
                selectedAudioUri = data.getData();
            }
            else if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                selectedVideoUri = data.getData();
                videoView.setVideoURI(selectedVideoUri);
                videoView.start();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        controller = new MediaController(AudioVideoMergeActivity.this);
                        controller.setAnchorView(videoView);
                        videoView.setMediaController(controller);
                        mp.setLooping(true);
                    }
                });
            }
        }
    }

    /**
     * Load FFmpeg binary
     */
    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                Log.d(TAG, "ffmpeg : era nulo");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "ffmpeg : correct Loaded");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "EXception no controlada : " + e);
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(AudioVideoMergeActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AudioVideoMergeActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    /**
     * Command for extracting audio from video
     */
    private void mergeAudioVideo() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );

        String filePrefix = "merge";
        String fileExtn = ".mp4";
        String audioRealPath = selectedAudioUri.getPath();
        String videoRealPath = selectedVideoUri.getPath();
        File dest = new File(moviesDir, filePrefix + fileExtn);

        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        Log.d(TAG, "src: audio:" + audioRealPath + "& video:" + videoRealPath);
        Log.d(TAG, "dest: " + dest.getAbsolutePath());
        filePath = dest.getAbsolutePath();
        final String[] complexCommand1 = {"-i", videoRealPath, "-i", audioRealPath, "-c", "copy", "-map", "0:v:0", "-map", "1:a:0", "-shortest", filePath};
        final String[] complexCommand2 = {"-y", "-i", videoRealPath, "-i", audioRealPath, "-c:v", "copy", "-c:a", "aac","-filter_complex", "[0:a][1:a]amerge=inputs=2[a]",
                "-map", "0:v:0", "-map", "[a]", "-shortest", filePath};

        final String[] complexCommand3 = {"-i", videoRealPath, "-i", audioRealPath, "-c", "copy", "-map", "0:v:0", "-map", "1:a:0", filePath};
        final String[] complexCommand4 = {"-y", "-i", videoRealPath, "-i", audioRealPath, "-c:v", "copy", "-c:a", "aac","-filter_complex", "[0:a][1:a]amerge=inputs=2[a]",
                "-map", "0:v:0", "-map", "[a]", filePath};
        MediaPlayer mp;
        mp = MediaPlayer.create(this, Uri.parse(audioRealPath));
        int audioDuration = mp.getDuration();
        mp.reset();
        mp = MediaPlayer.create(this, Uri.parse(videoRealPath));
        int videoDuration = mp.getDuration();
        mp.reset();
        mp.release();
        if(audioDuration<videoDuration){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Audio Is Short")
                    .setPositiveButton("Trim Video To Audio Length", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(choice.isChecked())
                                execFFmpegBinary(complexCommand2);
                            else
                                execFFmpegBinary(complexCommand1);
                        }
                    })
                    .setNegativeButton("Mute Remaining Video", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(choice.isChecked())
                                execFFmpegBinary(complexCommand4);
                            else
                                execFFmpegBinary(complexCommand3);
                        }
                    });
            builder.create();
            builder.show();
        }
        else{
            if(choice.isChecked())
                execFFmpegBinary(complexCommand2);
            else
                execFFmpegBinary(complexCommand1);
        }
    }

    /**
     * Executing ffmpeg binary
     */
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : " + s);
                    Intent intent = new Intent(AudioVideoMergeActivity.this, VideoPreviewActivity.class);
                    intent.putExtra(FILEPATH, filePath);
                    startActivity(intent);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));
                    progressDialog.setMessage("progress : " + s);
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + Arrays.toString(command));
                    progressDialog.dismiss();

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }
}
package com.devil.videoeditor.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.devil.videoeditor.R;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioVideoMergeActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_GALLERY_AUDIO = 100;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 200;
    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;
    private static final String TAG = "DEVIL";
    private static final String FILEPATH = "filepath";
    private Uri selectedAudioUri;
    private Uri selectedVideoUri;
    private String filePath;
    private CheckBox choice;
    private ScrollView mainlayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_video_merge);
        TextView uploadAudio = findViewById(R.id.uploadAudio);
        TextView uploadVideo = findViewById(R.id.uploadVideo);
        final TextView merge = findViewById(R.id.merge);
        mainlayout = findViewById(R.id.mainlayout);
        choice = findViewById(R.id.choice);
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
        String audioRealPath = getPath(AudioVideoMergeActivity.this, selectedAudioUri);
        String videoRealPath = getPath(AudioVideoMergeActivity.this, selectedVideoUri);
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

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     */
    private String getPath(final Context context, final Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri.
     */
    private String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
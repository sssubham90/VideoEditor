package com.devil.videoeditor.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReverseVideoActivity extends AppCompatActivity {


    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;
    private VideoView videoView;
    private RangeSeekBar<Integer> rangeSeekBar;
    private Runnable r;
    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;
    private Uri selectedVideoUri;
    private static final String TAG = "DEVIL";
    private static final String POSITION = "position";
    private static final String FILEPATH = "filepath";
    private int choice = 0;
    private int stopPosition;
    private ScrollView mainlayout;
    private TextView tvLeft, tvRight;
    private String filePath;
    private int duration;
    private Context mContext;
    private String[] lastReverseCommand;
    private MediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reverse_video);
        mContext = this;
        TextView uploadVideo = findViewById(R.id.uploadVideo);
        TextView reverseVideo = findViewById(R.id.reverseVideo);

        tvLeft = findViewById(R.id.tvLeft);
        tvRight = findViewById(R.id.tvRight);
        videoView = findViewById(R.id.videoView);
        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        mainlayout = findViewById(R.id.mainlayout);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        rangeSeekBar.setEnabled(true);
        loadFFMpegBinary();

        uploadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23)
                    getPermission();
                else
                    uploadVideo();

            }
        });
        reverseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedVideoUri != null) {
                    final Dialog dialog = showSingleOptionTextDialog(mContext);
                    TextView tvDialogHeading = dialog.findViewById(R.id.tvDialogHeading);
                    TextView tvDialogText = dialog.findViewById(R.id.tvDialogText);
                    TextView tvDialogSubmit = dialog.findViewById(R.id.tvDialogSubmit);
                    tvDialogHeading.setText("Process in Progress");
                    tvDialogText.setText(R.string.dialogMessage);
                    tvDialogSubmit.setText("Okay");
                    tvDialogSubmit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String yourRealPath = getPath(ReverseVideoActivity.this, selectedVideoUri);
                            splitVideoCommand(yourRealPath);
                            dialog.dismiss();
                        }

                    });
                    dialog.show();

                } else
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
            }
        });
    }

    private void getPermission() {
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
            ActivityCompat.requestPermissions(ReverseVideoActivity.this,
                    params,
                    100);
        } else
            uploadVideo();
    }

    /**
     * Handling response for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            uploadVideo();
        }
    }

    /**
     * Opening gallery for uploading video
     */
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                selectedVideoUri = data.getData();
                videoView.setVideoURI(selectedVideoUri);
                videoView.start();


                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        // TODO Auto-generated method stub
                        controller = new MediaController(ReverseVideoActivity.this);
                        controller.setAnchorView(videoView);
                        videoView.setMediaController(controller);
                        duration = mp.getDuration() / 1000;
                        tvLeft.setText("00:00:00");

                        tvRight.setText(getTime(mp.getDuration() / 1000));
                        mp.setLooping(true);
                        rangeSeekBar.setRangeValues(0, duration);
                        rangeSeekBar.setSelectedMinValue(1);
                        rangeSeekBar.setSelectedMaxValue(duration);
                        rangeSeekBar.setEnabled(true);

                        rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
                            @Override
                            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                                videoView.seekTo(minValue * 1000);
                                tvLeft.setText(getTime((Integer)bar.getSelectedMinValue()));
                                tvRight.setText(getTime((Integer)bar.getSelectedMaxValue()));
                            }
                        });

                        final Handler handler = new Handler();
                        handler.postDelayed(r = new Runnable() {
                            @Override
                            public void run() {

                                if (videoView.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue() * 1000)
                                    videoView.seekTo(rangeSeekBar.getSelectedMinValue() * 1000);
                                handler.postDelayed(r, 100);
                            }
                        }, 100);
                    }
                });
            }
        }
    }

    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
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
        new AlertDialog.Builder(ReverseVideoActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReverseVideoActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    /**
     * Command for segmenting video
     */
    private void splitVideoCommand(String path) {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );
        String filePrefix = "split_video";
        String fileExtn = ".mp4";

        File dir = new File(moviesDir, ".VideoSplit");
        if (dir.exists())
            deleteDir(dir);
        dir.mkdir();
        File dest = new File(dir, filePrefix + "%03d" + fileExtn);

        String[] complexCommand = {"-i", path, "-c:v", "libx264", "-crf", "22", "-map", "0", "-segment_time", "6", "-g", "9", "-sc_threshold", "0", "-force_key_frames", "expr:gte(t,n_forced*6)", "-f", "segment", dest.getAbsolutePath()};
        execFFmpegBinary(complexCommand);
    }

    /**
     * Command for reversing segmented videos
     */
    private void reverseVideoCommand() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );
        File srcDir = new File(moviesDir, ".VideoSplit");
        File[] files = srcDir.listFiles();
        String filePrefix = "reverse_video";
        String fileExtn = ".mp4";
        File destDir = new File(moviesDir, ".VideoPartsReverse");
        if (destDir.exists())
            deleteDir(destDir);
        destDir.mkdir();
        for (int i = 0; i < files.length; i++) {
            File dest = new File(destDir, filePrefix + i + fileExtn);
            String command[] = {"-i", files[i].getAbsolutePath(), "-vf", "reverse", "-af", "areverse", dest.getAbsolutePath()};
            if (i == files.length - 1)
                lastReverseCommand = command;
            execFFmpegBinary(command);
        }


    }

    /**
     * Command for concating reversed segmented videos
     */
    private void concatVideoCommand() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );
        File srcDir = new File(moviesDir, ".VideoPartsReverse");
        File[] files = srcDir.listFiles();
        if (files != null && files.length > 1) {
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder filterComplex = new StringBuilder();
        filterComplex.append("-filter_complex,");

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                stringBuilder.append("-i" + ",").append(files[i].getAbsolutePath()).append(",");
                filterComplex.append("[").append(i).append(":v").append(i).append("] [").append(i).append(":a").append(i).append("] ");

            }
            filterComplex.append("concat=n=").append(files.length).append(":v=1:a=1 [v] [a]");
        }
        String[] inputCommand = stringBuilder.toString().split(",");
        String[] filterCommand = filterComplex.toString().split(",");

        String filePrefix = "reverse_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();
        String[] destinationCommand = {"-map", "[v]", "-map", "[a]", dest.getAbsolutePath()};
        execFFmpegBinary(combine(inputCommand, filterCommand, destinationCommand));
    }

    public static String[] combine(String[] arg1, String[] arg2, String[] arg3) {
        String[] result = new String[arg1.length + arg2.length + arg3.length];
        System.arraycopy(arg1, 0, result, 0, arg1.length);
        System.arraycopy(arg2, 0, result, arg1.length, arg2.length);
        System.arraycopy(arg3, 0, result, arg1.length + arg2.length, arg3.length);
        return result;
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
                    if (choice == 8) {
                        choice = 9;
                        reverseVideoCommand();
                    }
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));
                    if (choice == 8)
                        progressDialog.setMessage("progress : splitting video " + s);
                    else if (choice == 9)
                        progressDialog.setMessage("progress : reversing splitted videos " + s);
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
                    if (choice != 8 && choice != 9 && choice != 10) {
                        progressDialog.dismiss();
                    }

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }


    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String aChildren : children) {
                    boolean success = deleteDir(new File(dir, aChildren));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
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

                    // TODO handle non-primary volumes
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

    private Dialog showSingleOptionTextDialog(Context mContext) {
        Dialog textDialog = new Dialog(mContext, R.style.DialogAnimation);
        if(textDialog.getWindow()!=null){
        textDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        textDialog.setContentView(R.layout.dialog_singleoption_text);
        textDialog.setCancelable(false);
        return textDialog;
    }

}
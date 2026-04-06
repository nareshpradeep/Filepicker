package com.digitral.filepicker;


import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import com.digitral.filepicker.FilePicker;
import com.digitral.filepicker.R;
import com.digitral.filepicker.callback.FilePickerCallback;
import com.digitral.filepicker.controls.ActivityResultHandler;
import com.digitral.filepicker.utils.FileUtils;
import com.digitral.filepicker.utils.ImageCompression;
import com.digitral.filepicker.utils.TraceUtils;
import com.digitral.filepicker.utils.Utils;
import com.google.common.io.ByteStreams;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class FilePickerImpl extends FilePicker {

    private static final int LAUNCH_CAMERA = 100;

    private static final int LAUNCH_GALLERY = 200;

    private static final int PICK_PDF = 300;

    private static final int PICK_DOCS = 400;

    private static final int PICK_AUDIO = 500;

    private static final int PICK_VIDEO = 600;
    public static final int PERMISSION_DENIED = 700;
    private static final int LAUNCH_MAP = 800;

    public static final int REQ_OPEN_APP_SEETINGS = 900;

    private static final String[] READ_PERMISSIONS =
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    private static final String FILE_PATH = "filePath";

    private static final String FILE_NAME = "fileName";
    public static final String URI_VAL = "uri";

    private AppCompatActivity activity;

    private ComponentActivity activityComponent;

    //private AppCompatActivity appCompatActivity;

    private FilePickerCallback callback;

    private Uri pathUri;

    private boolean cropEnabled;

    private boolean compressEnabled;

    private boolean waterMarkEnabled;

    private int fileSize;

    private int requestCode;

    private String mediaPath;

    private String requiredFilePath;

    private String waterMarkText;

    private Object returnObject;

    public static boolean filePickerIsInProcess = false;

    /*public FilePickerPermissionLauncher permissionLauncher;

    public FilePickerIntentLauncher activityLauncher;*/

    public ActivityResultHandler<String[], Map<String, Boolean>> permissionLauncher;

    public ActivityResultHandler<Intent, ActivityResult> activityLauncher;

    public ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    public static LifecycleOwner lifecycleOwner;

    public static boolean settingsPopupOpened = false;

    private boolean permissionDeniedOnce = false;

    private static String fileProviderAuthority(Context context) {
        return context.getApplicationContext().getPackageName() + ".fileprovider";
    }

    private FilePickerImpl(AppCompatActivity activity, FilePickerCallback callback, ActivityResultCaller caller) {

        this.callback = callback;

        this.activity = activity;

        this.permissionLauncher = ActivityResultHandler.registerPermissionForResult(caller);

        this.activityLauncher = ActivityResultHandler.registerActivityForResult(caller);

        // Register on the activity, not on {@code caller}. If {@code caller} is a Fragment that is
        // dismissed before the picker returns (e.g. PhotoOptionBottomSheet), fragment-scoped
        // registration may never deliver the result.
        this.pickMediaLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        TraceUtils.logE("Ooredoo", "In pickMedia::");
                        this.onGalleryFileSelected(RESULT_OK, new Intent(), uri);
                    }
                });


        FilePickerImpl.filePickerIsInProcess = true;

//        handleResponseForMedia();

    }

    public static FilePicker newInstance(AppCompatActivity context, FilePickerCallback callback, ActivityResultCaller caller) {

        return new FilePickerImpl(context, callback, caller);
    }

    public static void setLifecycleOwner(LifecycleOwner lOwner) {
        lifecycleOwner = lOwner;
    }

    public boolean rotateImageIfRequired(String imagePath) {

        int degrees = 0;

        try {

            ExifInterface exif = new ExifInterface(imagePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;

                default:
                    // no rotation required
                    return false;
            }

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

        BitmapFactory.Options decodeBounds = new BitmapFactory.Options();

        decodeBounds.inJustDecodeBounds = true;

        int numPixels = decodeBounds.outWidth * decodeBounds.outHeight;

        int maxPixels = 2048 * 1536; // requires 12 MB heap

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inSampleSize = (numPixels >= maxPixels) ? 2 : 1;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        if (bitmap == null) {

            return false;
        }

        Matrix matrix = new Matrix();

        matrix.setRotate(degrees);

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);

        FileOutputStream out;

        try {

            out = new FileOutputStream(imagePath);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

            bitmap.recycle();

        } catch (Exception t) {

            TraceUtils.logException(t);

        }

        return degrees > 0;
    }

    @Override
    public FilePicker cropEnabled(boolean cropEnabled) {
        this.cropEnabled = cropEnabled;
        return this;
    }

    @Override
    public FilePicker compressEnabled(boolean compressEnabled) {
        this.compressEnabled = compressEnabled;
        return this;
    }

    @Override
    public FilePicker requiredFilePath(String requiredFilePath) {
        this.requiredFilePath = requiredFilePath;
        return this;
    }

    @Override
    public FilePicker compressSize(int fileSizeInKB) {
        this.fileSize = fileSizeInKB;
        return this;
    }

    @Override
    public FilePicker waterMarkEnabled(boolean waterMarkEnabled) {

        this.waterMarkEnabled = waterMarkEnabled;

        if (waterMarkEnabled) {

            if (TextUtils.isEmpty(waterMarkText)) {

                waterMarkText = getPackageName();

            }

        }

        return this;

    }

    @Override
    public FilePicker waterMarkText(String waterMarkText) {

        this.waterMarkText = waterMarkText;

        return this;

    }

    private void camera() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Uri outputFileUri = getCaptureImageOutputUri();

            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            activityLauncher.launch(captureIntent, result -> {

                onActivityResult(LAUNCH_CAMERA, result.getResultCode(), result.getData());

            });

//            activity.startActivityForResult(captureIntent, LAUNCH_CAMERA);


        } catch (Exception t) {

            TraceUtils.logThrowable(t);

        }

    }

    private void gallery() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Intent intent = new Intent();

            intent.setType("image/*");

            intent.setAction(Intent.ACTION_PICK);

            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

            activityLauncher.launch(intent, result -> {

                onActivityResult(LAUNCH_GALLERY, result.getResultCode(), result.getData());

            });

//            activity.startActivityForResult(intent, LAUNCH_GALLERY);

        } catch (Exception t) {

            TraceUtils.logThrowable(t);

        }

    }

    private void pdf() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("application/pdf");

            Intent chooserIntent = Intent.createChooser(intent, "Pick pdf from");

            activityLauncher.launch(chooserIntent, result -> {

                onActivityResult(PICK_PDF, result.getResultCode(), result.getData());

            });

//            activity.startActivityForResult(Intent.createChooser(intent, "Pick pdf from"),PICK_PDF);

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    private void docs() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("application/msword");

            Intent chooserIntent = Intent.createChooser(intent, "Pick doc from");

            activityLauncher.launch(chooserIntent, result -> {

                onActivityResult(PICK_DOCS, result.getResultCode(), result.getData());

            });

//            activity.startActivityForResult(Intent.createChooser(intent, "Pick doc from"),
//                    PICK_DOCS);

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    @Override
    public void launchCamera(int requestCode, Object returnObject) {

        this.requestCode = requestCode;
        this.returnObject = returnObject;

        if (!validateFilePath()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                camera();
            } else {
                if (!ensurePermissionLauncher()) {
                    return;
                }
                permissionLauncher.launch(
                        new String[]{Manifest.permission.CAMERA},
                        result -> {
                            if (Utils.allPermissionGranted(result)) {
                                camera();
                            } else {
                                Toast.makeText(
                                                activity,
                                                activity.getResources().getString(R.string.cpr),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }
        }

    }

    @Override
    public void launchMap(int requestCode, Object returnObject) {

        this.requestCode = requestCode;
        this.returnObject = returnObject;

        if (!validateFilePath()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!ensurePermissionLauncher()) {
                return;
            }
            permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, result -> {

                if (Utils.allPermissionGranted(result)) {

                    callback.onResult(this.requestCode, 0, null, this.returnObject);

                    resetFilePickerProcess();

                } else {

                        Toast.makeText(activity, activity.getResources().getString(R.string.lpr), Toast.LENGTH_SHORT).show();

                }

            });
        }

    }

    public void launchGallery(int requestCode, Object returnObject) {

        this.requestCode = requestCode;
        this.returnObject = returnObject;

        if (!validateFilePath()) {

            return;

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            pickMedia();

        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                String[] permissions = getPermissions();

                permissionLauncher.launch(permissions, result -> {

                    if (Utils.allPermissionGranted(result)) {

                        if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                pickMedia();
                                return;
                            }
                        }

                        gallery();

                    } else {

                        Toast.makeText(activity, activity.getResources().getString(R.string.rwpr), Toast.LENGTH_SHORT).show();

                        callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);

                        resetFilePickerProcess();

                    }
                });


            }

        }

    }

    public void pickMedia() {
        if (!ensurePickMediaLauncher()) {
            return;
        }
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        pickMediaLauncher.launch(request);
    }


    private static String[] getPermissions() {

        String[] permissions = {};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            permissions = new String[]{Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, Manifest.permission.READ_MEDIA_IMAGES};

        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {

            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};

        } else {

            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return permissions;
    }


    private boolean ensurePermissionLauncher() {
        if (permissionLauncher == null) {
            TraceUtils.logE("FilePicker", "permissionLauncher null — call setPermissionLauncher from Activity");
            Toast.makeText(activity, "File picker not initialized (permission)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean ensurePickMediaLauncher() {
        if (pickMediaLauncher == null) {
            TraceUtils.logE("FilePicker", "pickMediaLauncher null — call setPickMediaLauncher from Activity");
            Toast.makeText(activity, "File picker not initialized (photo picker)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean ensureActivityLauncher() {
        if (activityLauncher == null) {
            TraceUtils.logE("FilePicker", "activityLauncher null — call setActivityLauncher from Activity");
            Toast.makeText(activity, "File picker not initialized (activity)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void pickPdf(int requestCode, Object returnObject) {

        this.requestCode = requestCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!ensurePermissionLauncher()) {
                return;
            }
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA}, result -> {

                if (Utils.allPermissionGranted(result)) {

                    pdf();

                } else {

                    Toast.makeText(activity, activity.getResources().getString(R.string.rpr), Toast.LENGTH_SHORT).show();
                    callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                    resetFilePickerProcess();
                }

            });
        }

    }

    private void audio() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("audio/*");

            Intent chooserIntent = Intent.createChooser(intent, "Pick audio from");

            activityLauncher.launch(chooserIntent, result -> {

                onActivityResult(PICK_AUDIO, result.getResultCode(), result.getData());

            });

            /*activity.startActivityForResult(Intent.createChooser(intent, "Pick audio from"),
                    PICK_AUDIO);*/

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    @Override
    public void pickAudio(int requestCode) {

        this.requestCode = requestCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            activity.requestPermissions(READ_PERMISSIONS, PICK_AUDIO);

        } else {

            audio();

        }

    }

    private void video() {

        try {

            if (!ensureActivityLauncher()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("video/*");

            activityLauncher.launch(intent, result -> {

                onActivityResult(PICK_VIDEO, result.getResultCode(), result.getData());

            });

//            activity.startActivityForResult(Intent.createChooser(intent, "Pick video from"),
//                    PICK_VIDEO);

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    @Override
    public void pickVideo(int requestCode) {

        this.requestCode = requestCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ActivityCompat.requestPermissions(activity, READ_PERMISSIONS, PICK_VIDEO);

        } else {

            video();

        }

    }

    @Override
    public void setCallback(FilePickerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void clear() {
        this.callback = null;
        this.pathUri = null;
        this.cropEnabled = true;
        this.compressEnabled = false;
        this.waterMarkEnabled = false;
        this.fileSize = 0;
        this.mediaPath = "";
        this.requestCode = -1;
        this.requiredFilePath = "";
        this.returnObject = null;
        this.waterMarkText = "";
        resetFilePickerProcess();
    }

    @Override
    public void pickImage(FilePickerCallback callback) {
        this.callback = callback;
        PhotoOptionBottomSheet sheet = PhotoOptionBottomSheet.newInstance();

        sheet.setOptionListener(new PhotoOptionBottomSheet.OptionListener() {
            @Override
            public void onCameraClick() {
                clear();
                if (compressEnabled) {
                    compressSize(80);
                }
                launchCamera(LAUNCH_CAMERA,null);
            }

            @Override
            public void onGalleryClick() {
                clear();
                if (compressEnabled) {
                    compressSize(80);
                }
                launchGallery(LAUNCH_GALLERY, null);
            }

            @Override
            public void onRemoveClick() {
                // optional
            }

            @Override
            public void onLocationClick() {
                // optional
            }
        });

        sheet.show(activity.getSupportFragmentManager(), "FilePicker");
    }

    /*@Override
    public void setActivityLauncher(FilePickerIntentLauncher launcher) {
        this.activityLauncher = launcher;
    }

    @Override
    public void setPermissionLauncher(FilePickerPermissionLauncher launcher) {
        this.permissionLauncher = launcher;
    }

    @Override
    public void setPickMediaLauncher(ActivityResultLauncher<PickVisualMediaRequest> launcher) {
        this.pickMediaLauncher = launcher;
    }*/

    @Override
    public void pickDocs(int requestCode) {

        this.requestCode = requestCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            activity.requestPermissions(READ_PERMISSIONS, PICK_DOCS);

        } else {

            docs();

        }

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == LAUNCH_CAMERA) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                camera();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.cpr),
                        Toast.LENGTH_SHORT).show();

            }

        } else if (requestCode == LAUNCH_GALLERY) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                gallery();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.rwpr), Toast.LENGTH_SHORT).show();

                callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                resetFilePickerProcess();

            }

        } else if (requestCode == LAUNCH_MAP) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                callback.onResult(this.requestCode, 0, null, this.returnObject);
                resetFilePickerProcess();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.lpr), Toast.LENGTH_SHORT).show();

            }

        } else if (requestCode == PICK_PDF) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                pdf();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.rpr),
                        Toast.LENGTH_SHORT).show();
                callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                resetFilePickerProcess();

            }

        } else if (requestCode == PICK_AUDIO) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {
                audio();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.rpr),
                        Toast.LENGTH_SHORT).show();
                callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                resetFilePickerProcess();
            }

        } else if (requestCode == PICK_VIDEO) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                video();

            } else {

                Toast.makeText(activity,
                        activity.getResources().getString(R.string.rpr),
                        Toast.LENGTH_SHORT).show();

                callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                resetFilePickerProcess();

            }

        } else if (requestCode == PICK_DOCS) {

            if (Utils.showGrantPermissionsMessage(grantResults, permissions, false, activity)) {

                docs();

            } else {

                Toast.makeText(activity, activity.getResources().getString(R.string.rpr),
                        Toast.LENGTH_SHORT).show();

                callback.onResult(this.requestCode, PERMISSION_DENIED, null, this.returnObject);
                resetFilePickerProcess();

            }

        }

    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {

            if (data == null) {

                data = new Intent();

                data.putExtra(FILE_PATH, "");

                data.putExtra(FILE_NAME, "");

            }

            if (resultCode == RESULT_OK) {

                switch (requestCode) {

                    case LAUNCH_CAMERA: {

                        if (cropEnabled) {

                            Uri uri = getCaptureImageOutputUri();

                            boolean rotated = rotateImageIfRequired(mediaPath);

                            cropImage(uri, rotated);

                        } else if (compressEnabled) {

                            File file = new File(mediaPath);

                            if (fileSize == 0) {

                                fileSize = (int) file.length();

                            }

                            fileSize = (int) file.length();

                            TraceUtils.logE("WS", "WS: Filesize before: " + fileSize);

                            String filePath = new ImageCompression(activity).compressImage(file.getAbsolutePath(), fileSize, 2);

                            data.putExtra(FILE_PATH, filePath);

                            data.putExtra(FILE_NAME, Utils.getFileName(filePath));

                            writeWaterMarkIfRequired(Utils.decodeImage(filePath, activity), activity, filePath, waterMarkText);

                            copyFileIfRequired(data, filePath);

                            File file1 = new File(filePath);
                            fileSize = (int) file1.length();

                            TraceUtils.logE("WS", "WS: Filesize after: " + fileSize);

                            callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                            resetFilePickerProcess();

                        } else {

                            data.putExtra(FILE_PATH, mediaPath);

                            data.putExtra(FILE_NAME, Utils.getFileName(mediaPath));

                            writeWaterMarkIfRequired(Utils.decodeImage(mediaPath, activity), activity, mediaPath, waterMarkText);

                            callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                            resetFilePickerProcess();

                        }

                    }

                    break;

                    case LAUNCH_GALLERY:

                        Uri _uri = data.getData();

                        onGalleryFileSelected(resultCode, data, _uri);

                        break;

                    case PICK_PDF:
                        Uri _uriPdf = data.getData();
                    {
                        File file1 = FileUtils.getFile(activity, _uriPdf);
                        if (file1 != null) {
                            data.putExtra(FILE_PATH, file1.getAbsolutePath());
                            data.putExtra(FILE_NAME, Utils.getFileName(file1.getAbsolutePath()));

                        }
                    }
                    callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                    resetFilePickerProcess();

                    break;

                    case PICK_DOCS: {
                        Uri _uriDocs = data.getData();
                        {
                            File file1 = FileUtils.getFile(activity, _uriDocs);
                            if (file1 != null) {
                                data.putExtra(FILE_PATH, file1.getAbsolutePath());
                                data.putExtra(FILE_NAME, Utils.getFileName(file1.getAbsolutePath()));

                            }
                        }
                        callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                        resetFilePickerProcess();
                    }
                    break;

                    case PICK_AUDIO: {
                        Uri _audioData = data.getData();
                        {
                            File file1 = FileUtils.getFile(activity, _audioData);
                            if (file1 != null) {
                                data.putExtra(FILE_PATH, file1.getAbsolutePath());
                                data.putExtra(FILE_NAME, Utils.getFileName(file1.getAbsolutePath()));

                            }
                        }
                        callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                        resetFilePickerProcess();
                        break;
                    }

                    case PICK_VIDEO: {
                        Uri _audioData = data.getData();
                        {
                            File file1 = FileUtils.getFile(activity, _audioData);
                            if (file1 != null) {
                                data.putExtra(FILE_PATH, file1.getAbsolutePath());
                                data.putExtra(FILE_NAME, Utils.getFileName(file1.getAbsolutePath()));

                            }
                        }
                        resetFilePickerProcess();
                        callback.onResult(this.requestCode, resultCode, data, this.returnObject);

                    }
                    break;

                    case UCrop.REQUEST_CROP:

                        data.putExtra(FILE_PATH, getPath(pathUri));

                        data.putExtra(FILE_NAME, Utils.getFileName(getPath(pathUri)));
                        data.putExtra(URI_VAL, pathUri);

                        callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                        resetFilePickerProcess();

                        break;

                }

            } else {

                callback.onResult(this.requestCode, resultCode, null, this.returnObject);
                resetFilePickerProcess();

            }

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    @Override
    public void onGalleryFileSelected(int resultCode, Intent data, Uri _uri) {

        if (_uri == null) {
            callback.onResult(this.requestCode, Activity.RESULT_CANCELED, data, this.returnObject);
            resetFilePickerProcess();
            return;
        }
        data.setData(_uri);

        String path = getPath(_uri);

        if (cropEnabled) {

            cropImage(_uri, false);

        } else if (compressEnabled) {

            TraceUtils.logE("WS", "WS: Filesize before: " + fileSize);

            try {
                String sourcePath = path;
                if (TextUtils.isEmpty(sourcePath) || !new File(sourcePath).isFile()) {
                    File cached = copyPickVisualUriToCache(_uri);
                    if (cached == null) {
                        TraceUtils.logE("FilePicker", "compress: could not read picked URI");
                        callback.onResult(this.requestCode, Activity.RESULT_CANCELED, data, this.returnObject);
                        resetFilePickerProcess();
                        return;
                    }
                    sourcePath = cached.getAbsolutePath();
                }
                String filePath = new ImageCompression(activity).compressImage(sourcePath, fileSize, 2);

                data.putExtra(FILE_PATH, filePath);

                data.putExtra(FILE_NAME, Utils.getFileName(filePath));

                writeWaterMarkIfRequired(Utils.decodeImage(filePath, activity), activity, filePath, waterMarkText);

                callback.onResult(this.requestCode, resultCode, data, this.returnObject);

                resetFilePickerProcess();
            } catch (Exception e) {
                TraceUtils.logException(e);
                callback.onResult(this.requestCode, Activity.RESULT_CANCELED, data, this.returnObject);
                resetFilePickerProcess();
            }

        } else {

            Uri _urifILES = _uri;


            ParcelFileDescriptor parcelFileDescriptor = null;

            try {
                parcelFileDescriptor = activity.getContentResolver().openFileDescriptor(_urifILES, "r", null);

                if (parcelFileDescriptor != null) {

                    FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                    String cacheName = FileUtils.getFilePath(activity, _urifILES);
                    if (TextUtils.isEmpty(cacheName)) {
                        cacheName = "gallery_" + System.currentTimeMillis() + ".jpg";
                    }
                    cacheName = cacheName.replaceAll("[\\\\/]+", "_");
                    File file = new File(activity.getCacheDir(), cacheName);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    //                                IOUtils.copyStream(inputStream, outputStream);
                    ByteStreams.copy(inputStream, outputStream);
                    outputStream.close();
                    inputStream.close();

                    data.putExtra(FILE_PATH, file.getAbsolutePath());
                    data.putExtra(FILE_NAME, Utils.getFileName(file.getAbsolutePath()));

                }

                callback.onResult(this.requestCode, resultCode, data, this.returnObject);
                resetFilePickerProcess();

            } catch (IOException e) {
                TraceUtils.logException(e);
                callback.onResult(this.requestCode, Activity.RESULT_CANCELED, data, this.returnObject);
                resetFilePickerProcess();
            } finally {
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e) {
                        TraceUtils.logException(e);
                    }
                }
            }

        }
    }

    @Nullable
    private File copyPickVisualUriToCache(@NonNull Uri sourceUri) throws IOException {
        try (ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(sourceUri, "r", null)) {
            if (pfd == null) {
                return null;
            }
            String cacheName = FileUtils.getFilePath(activity, sourceUri);
            if (TextUtils.isEmpty(cacheName)) {
                cacheName = "gallery_" + System.currentTimeMillis() + ".jpg";
            }
            cacheName = cacheName.replaceAll("[\\\\/]+", "_");
            File file = new File(activity.getCacheDir(), cacheName);
            try (FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
                 FileOutputStream out = new FileOutputStream(file)) {
                ByteStreams.copy(in, out);
            }
            return file;
        }
    }


    public Uri getCaptureImageOutputUri() {

        Uri outputFileUri = null;

        File getImage = activity.getExternalCacheDir();

        if (getImage != null) {

            File profileMediaPath;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                profileMediaPath = new File(getImage + "/profilepic.jpeg");

                outputFileUri = FileProvider.getUriForFile(activity, fileProviderAuthority(activity), new File(getImage + "/profilepic.jpeg"));

            } else {

                profileMediaPath = new File(getImage.getPath(), "profilepic.jpeg");

                outputFileUri = Uri.fromFile(new File(getImage.getPath(), "profilepic.jpeg"));

            }

            mediaPath = profileMediaPath.getAbsolutePath();

        }

        return outputFileUri;

    }

    private String getPath(Uri uri) {

        if (uri == null) {

            return null;

        }

//        String[] projection = {MediaStore.Images.Media._ID};
        String[] projection = {MediaStore.Images.Media.DATA};

            Cursor cursor = activity.getContentResolver()
                    .query(uri, projection, null, null, null);
        try {

            if (cursor != null) {

                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                cursor.moveToFirst();

                return cursor.getString(column_index);

            }
        } catch (IllegalArgumentException e) {
            TraceUtils.logException(e);
        } finally {
            if (cursor!=null && !cursor.isClosed()) {
                cursor.close();
            }

        }

        return uri.getPath();

    }


    private void cropImage(Uri fileUri, boolean rotated) {

        try {

            Uri stableUri = ensureStableCropSource(fileUri);

            File file1 = new File(activity.getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");

            pathUri = Uri.fromFile(file1);
            //pathUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file1);

            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            TraceUtils.logE("WS", "WS: rotated: " + rotated);
            TraceUtils.logE("WS", "WS: rotated: " + width + "x" + height);


//            Crop.of(fileUri, pathUri).asSquare().start(activity);

            //Crop.of(stableUri, pathUri).withMaxSize(width, height).start(activity);
//            Crop.of(fileUri, pathUri).withAspect(height, width).start(activity);
//            Crop.of(fileUri, pathUri).withAspect(2.12, 1).start(activity);

            /*Intent cropIntent = Crop.of(stableUri, pathUri).withMaxSize(width, height).getIntent(activity);
            if (!ensureActivityLauncher()) {
                return;
            }
            // Do not use Crop.start(activity): it relies on deprecated Activity#onActivityResult,
            // which many hosts never forward. Use the same ActivityResult pipeline as camera/gallery.
            activityLauncher.launch(cropIntent, result ->
                    onActivityResult(Crop.REQUEST_CROP, result.getResultCode(), result.getData()));*/


            Intent intent = UCrop.of(stableUri, pathUri)
                    .withMaxResultSize(width, height)
                    .getIntent(activity);

            activityLauncher.launch(intent, result ->
                    onActivityResult(UCrop.REQUEST_CROP, result.getResultCode(), result.getData()));



        } catch (ActivityNotFoundException anfe) {

            String errorMessage = "Whoops - your device doesn't support the crop action!";

            Toast toast = Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT);

            toast.show();

        } catch (Exception e) {
            TraceUtils.logException(e);
            Toast.makeText(activity, "Error starting crop:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @NonNull
    private Uri ensureStableCropSource(@NonNull Uri src) {
        try {
            final String mime = activity.getContentResolver().getType(src);
            final boolean isLikelyRegionDecoderFriendly =
                    "image/jpeg".equalsIgnoreCase(mime) ||
                            "image/jpg".equalsIgnoreCase(mime) ||
                            "image/png".equalsIgnoreCase(mime) ||
                            "image/webp".equalsIgnoreCase(mime);

            final File cacheDir = activity.getExternalCacheDir() != null ?
                    activity.getExternalCacheDir() :
                    activity.getCacheDir();

            if (cacheDir == null) {
                TraceUtils.logE("Crop", "Cache directory is null");
                return src;
            }

            if (isLikelyRegionDecoderFriendly && "file".equalsIgnoreCase(src.getScheme())) {
                // Already a local file Uri with a common format.
                TraceUtils.logE("Crop", "Using original file URI (already local and compatible)");
                return src;
            }

            final File outFile = new File(cacheDir, "crop_src_" + System.currentTimeMillis() + ".jpg");

            if (isLikelyRegionDecoderFriendly) {
                // Copy the bytes as-is into a local file; avoid full decode if we can.
                try (InputStream in = activity.getContentResolver().openInputStream(src);
                     OutputStream out = new FileOutputStream(outFile)) {

                    if (in == null) {
                        TraceUtils.logE("Crop", "Cannot open input stream for copying");
                        return src;
                    }

                    ByteStreams.copy(in, out);
                    TraceUtils.logE("Crop", "Copied compatible image to cache: " + outFile.getPath());
                }

                Uri fileProviderUri = FileProvider.getUriForFile(
                        activity,
                        fileProviderAuthority(activity),
                        outFile
                );

                // Grant permissions for the new URI
                activity.grantUriPermission(
                        activity.getPackageName(),
                        fileProviderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );

                return fileProviderUri;
            }

            // For HEIC/HEIF/AVIF/unknown: decode and re-encode to JPEG.
            TraceUtils.logE("Crop", "Converting incompatible format to JPEG: " + mime);

            final Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(activity.getContentResolver(), src);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, s) ->
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                );
            } else {
                try (InputStream in = activity.getContentResolver().openInputStream(src)) {
                    if (in == null) {
                        TraceUtils.logE("Crop", "Cannot open input stream for decoding");
                        return src;
                    }
                    bitmap = BitmapFactory.decodeStream(in);
                }
            }

            if (bitmap == null) {
                TraceUtils.logE("Crop", "Failed to decode bitmap from source");
                return src;
            }

            try (OutputStream out = new FileOutputStream(outFile)) {
                boolean compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
                if (!compressed) {
                    TraceUtils.logE("Crop", "Failed to compress bitmap to JPEG");
                    bitmap.recycle(); // Clean up
                    return src;
                }
                TraceUtils.logE("Crop", "Successfully converted to JPEG: " + outFile.getPath());
            } finally {
                bitmap.recycle(); // ✅ Important: recycle bitmap to prevent memory leaks
            }

            Uri fileProviderUri = FileProvider.getUriForFile(
                    activity,
                    fileProviderAuthority(activity),
                    outFile
            );

            // Grant permissions for the new URI
            activity.grantUriPermission(
                    activity.getPackageName(),
                    fileProviderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            return fileProviderUri;

        } catch (Exception e) {
            TraceUtils.logException(e);
            TraceUtils.logE("Crop", "Error ensuring stable crop source, using original URI");
            return src;
        }
    }

    private boolean validateFilePath() {

        if (TextUtils.isEmpty(requiredFilePath)) {

            return true;

        } else {

            File f = new File(requiredFilePath);

            return f != null;

        }

    }

    public Bitmap writeWaterMarkIfRequired(Bitmap toEdit,
                                           Context context, String mediaPath,
                                           String waterMarkText) {
        try {

            if (!waterMarkEnabled) {
                return null;
            }

            Bitmap dest = Bitmap.createBitmap(toEdit.getWidth(),
                    toEdit.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas cs = new Canvas(dest);
            Paint imgPaint = new Paint();
            imgPaint.setStyle(Paint.Style.FILL);
            imgPaint.setStrokeJoin(Paint.Join.ROUND);

            Paint tPaint = new Paint();
            tPaint.setStyle(Paint.Style.FILL);
            tPaint.setStrokeJoin(Paint.Join.ROUND);

            tPaint.setTextSize(context.getResources().getDimensionPixelSize(
                    R.dimen.watermark_font_size_for_image));
            tPaint.setColor(Color.RED);
            tPaint.setAlpha(255);

            tPaint.setTextAlign(Paint.Align.LEFT);
            float height = tPaint.measureText("Y");
            float leftMargin = tPaint.measureText("Y");
            cs.drawBitmap(toEdit, 0, 0, imgPaint);
            float startWidth;
            float startHeight;
            startWidth = leftMargin;
            startHeight = dest.getHeight() - (12 * height);
            cs.drawText(waterMarkText, startWidth,
                    startHeight + (8 * height), tPaint);

            dest.compress(Bitmap.CompressFormat.JPEG, 100,
                    new FileOutputStream(mediaPath));
            return dest;
        } catch (FileNotFoundException e) {
            TraceUtils.logException(e);
        }
        return null;
    }

    private String getPackageName() {

        PackageManager pm = activity.getApplicationContext().getPackageManager();

        ApplicationInfo ai;

        try {

            ai = pm.getApplicationInfo(activity.getPackageName(), 0);

        } catch (final Exception e) {

            ai = null;

        }

        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

    }

    private void copyFileIfRequired(Intent data, String path) {

        try {

            if (!TextUtils.isEmpty(requiredFilePath)) {

                Utils.copyFile(activity, path, requiredFilePath);

                data.putExtra(FILE_PATH, requiredFilePath);

                data.putExtra(FILE_NAME, requiredFilePath);

            } else {

                data.putExtra(FILE_PATH, path);

                data.putExtra(FILE_NAME, Utils.getFileName(path));

            }

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }

    public void resetFilePickerProcess() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> FilePickerImpl.filePickerIsInProcess = false, 2000);
    }

    /*public void showAppSettingsPopup() {

        if (!settingsPopupOpened) {

            settingsPopupOpened = true;

            ((BaseActivity)activity).showCustomMOAPopUp(R.layout.popup_message_moa, R.drawable.ic_info_not_subscribed, "", activity.getResources().getString(R.string.aotayd), REQ_OPEN_APP_SEETINGS, activity.getResources().getString(R.string.settings), activity.getResources().getString(R.string.notnow), new IDialogCallbacks() {

                @Override
                public void onOK(int aRequestId, Object aObject) {

                    if (aRequestId == REQ_OPEN_APP_SEETINGS) {

                        gotoAppSettingsPage();

                        settingsPopupOpened = false;
                    }
                }

                @Override
                public void onCancel(int aRequestId, Object aObject) {
                    if (aRequestId ==  REQ_OPEN_APP_SEETINGS) {
                        settingsPopupOpened = false;
                    }
                }
            }, null, false, true);
        }

    }*/


    public void gotoAppSettingsPage() {

        try {

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);

        } catch (Exception e) {

            TraceUtils.logException(e);

        }

    }



}

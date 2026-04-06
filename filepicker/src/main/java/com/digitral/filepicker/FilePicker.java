package com.digitral.filepicker;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;

import com.digitral.filepicker.callback.FilePickerCallback;

public abstract class FilePicker {

    public static final int REQ_CODE_CAMERA = 100;

    public static final int REQ_CODE_GALLERY = 200;

    public static final int REQ_CODE_PICK_PDF = 300;

    public static final int REQ_CODE_LOCATION = 400;

    public abstract void launchCamera(int requestCode, Object returnObject);

    public abstract void launchMap(int requestCode, Object returnObject);

    public abstract void launchGallery(int requestCode, Object returnObject);

    public abstract void pickPdf(int requestCode, Object returnObject);

    public abstract void pickAudio(int requestCode);

    public abstract void onActivityResult(int requestCode, int resultCode, Intent data);

    public abstract void onGalleryFileSelected(int resultCode, Intent data, Uri _uri);

    public abstract void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

    public abstract void pickDocs(int requestCode);

    public abstract FilePicker cropEnabled(boolean cropEnabled);

    public abstract FilePicker compressEnabled(boolean compressEnabled);

    public abstract FilePicker compressSize(int fileSizeInKB);

    public abstract FilePicker waterMarkEnabled(boolean waterMarkEnabled);

    public abstract FilePicker waterMarkText(String waterMarkText);

    public abstract FilePicker requiredFilePath(String requiredFilePath);

    public abstract void pickVideo(int requestCode);

    public abstract void setCallback(FilePickerCallback callback);

    public abstract void clear();

    //public abstract void showAppSettingsPopup();

    //public abstract voiker(FilePickerIntentLauncher launcher);

    //public abstract void setPermissionLauncher(FilePickerPermissionLauncher launcher);

    //public abstract void setPickMediaLauncher(ActivityResultLauncher<PickVisualMediaRequest> launcher);

    public abstract void pickImage(FilePickerCallback callback);

}

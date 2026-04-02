package com.digitral.filepicker;

import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;

import com.digitral.filepicker.controls.ActivityResultHandler;

import java.util.Map;

/**
 * Builds {@link FilePickerIntentLauncher} / {@link FilePickerPermissionLauncher} from an
 * {@link ActivityResultCaller} (e.g. {@code ComponentActivity}) so consuming apps only wire
 * standard AndroidX activity-result registration — no dependency on {@code controls} types.
 */
public final class FilePickerLaunchers {

    private FilePickerLaunchers() {
    }

    /**
     * Registers {@link androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult}
     * on {@code caller} and returns a launcher suitable for {@link FilePicker#setActivityLauncher}.
     */
    @NonNull
    public static FilePickerIntentLauncher intentLauncher(@NonNull ActivityResultCaller caller) {
        return fromIntentHandler(ActivityResultHandler.registerActivityForResult(caller));
    }

    /**
     * Registers {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions}
     * on {@code caller} and returns a launcher suitable for {@link FilePicker#setPermissionLauncher}.
     */
    @NonNull
    public static FilePickerPermissionLauncher permissionLauncher(@NonNull ActivityResultCaller caller) {
        return fromPermissionHandler(ActivityResultHandler.registerPermissionForResult(caller));
    }

    /**
     * Wraps an existing handler (e.g. if you already registered one in your base activity).
     */
    @NonNull
    public static FilePickerIntentLauncher fromIntentHandler(
            @NonNull ActivityResultHandler<Intent, ActivityResult> handler) {
        return (intent, callback) -> handler.launch(
                intent,
                callback == null ? null : callback::onActivityResult);
    }

    /**
     * Wraps an existing permission handler.
     */
    @NonNull
    public static FilePickerPermissionLauncher fromPermissionHandler(
            @NonNull ActivityResultHandler<String[], Map<String, Boolean>> handler) {
        return (permissions, callback) -> handler.launch(
                permissions,
                callback == null ? null : callback::onResult);
    }
}

package com.digitral.filepicker;

import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.digitral.filepicker.controls.ActivityResultHandler;

import java.util.Map;

/**
 * Builds {@link FilePickerIntentLauncher} / {@link FilePickerPermissionLauncher} from an
 * {@link ActivityResultCaller} (e.g. {@code ComponentActivity}) so consuming apps only wire
 * standard AndroidX activity-result registration — no dependency on {@code controls} types.
 * <p>
 * <b>Lifecycle:</b> {@code intentLauncher} / {@code permissionLauncher} call
 * {@code registerForActivityResult}, which AndroidX only allows <em>before</em> the owner is
 * {@link androidx.lifecycle.Lifecycle.State#STARTED STARTED}. Wire launchers in {@code onCreate}
 * (or {@code Fragment.onCreate}) and call {@link FilePicker#setActivityLauncher} /
 * {@link FilePicker#setPermissionLauncher} there — not the first time the user taps a button.
 */
public final class FilePickerLaunchers {

    private FilePickerLaunchers() {
    }

    /**
     * Registers {@link androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult}
     * on {@code caller} and returns a launcher suitable for {@link FilePicker#setActivityLauncher}.
     * <p>
     * From a {@link ComponentActivity} (e.g. {@code AppCompatActivity}) or {@link Fragment}, pass
     * {@code this} — no cast needed.
     * A cast to {@link ActivityResultCaller} only appears if your variable is not typed as a subtype
     * (e.g. {@link ComponentActivity}, {@link Fragment}, or {@link ActivityResultCaller} itself).
     */
    @NonNull
    public static FilePickerIntentLauncher intentLauncher(@NonNull ActivityResultCaller caller) {
        return fromIntentHandler(ActivityResultHandler.registerActivityForResult(caller));
    }

    /** Same as {@link #intentLauncher(ActivityResultCaller)}; use {@code this} from your activity. */
    @NonNull
    public static FilePickerIntentLauncher intentLauncher(@NonNull AppCompatActivity activity) {
        return intentLauncher((ActivityResultCaller) activity);
    }

    /** Same as {@link #intentLauncher(ActivityResultCaller)}; use {@code this} from your fragment. */
    @NonNull
    public static FilePickerIntentLauncher intentLauncher(@NonNull Fragment fragment) {
        return intentLauncher((ActivityResultCaller) fragment);
    }

    /**
     * Registers {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions}
     * on {@code caller} and returns a launcher suitable for {@link FilePicker#setPermissionLauncher}.
     *
     * @see #intentLauncher(ActivityResultCaller) for notes on {@code ActivityResultCaller} and casts.
     */
    @NonNull
    public static FilePickerPermissionLauncher permissionLauncher(@NonNull ActivityResultCaller caller) {
        return fromPermissionHandler(ActivityResultHandler.registerPermissionForResult(caller));
    }

    /** Same as {@link #permissionLauncher(ActivityResultCaller)}; use {@code this} from your activity. */
    @NonNull
    public static FilePickerPermissionLauncher permissionLauncher(@NonNull AppCompatActivity activity) {
        return permissionLauncher((ActivityResultCaller) activity);
    }

    /** Same as {@link #permissionLauncher(ActivityResultCaller)}; use {@code this} from your fragment. */
    @NonNull
    public static FilePickerPermissionLauncher permissionLauncher(@NonNull Fragment fragment) {
        return permissionLauncher((ActivityResultCaller) fragment);
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

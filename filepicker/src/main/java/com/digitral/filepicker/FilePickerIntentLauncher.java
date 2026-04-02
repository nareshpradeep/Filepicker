package com.digitral.filepicker;

import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Public API for starting activities for result. Host apps implement or obtain this via
 * {@link FilePickerLaunchers} — you do not need {@code com.digitral.filepicker.controls.*}.
 */
public interface FilePickerIntentLauncher {

    /**
     * @param callback Invoked when the started activity returns; may be null if you do not need the result.
     */
    void launch(@NonNull Intent intent, @Nullable Callback callback);

    interface Callback {
        void onActivityResult(@NonNull ActivityResult result);
    }
}

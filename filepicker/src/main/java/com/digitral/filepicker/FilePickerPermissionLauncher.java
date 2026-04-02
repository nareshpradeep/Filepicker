package com.digitral.filepicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Public API for requesting multiple runtime permissions. Obtain via {@link FilePickerLaunchers}.
 */
public interface FilePickerPermissionLauncher {

    /**
     * @param callback Invoked with per-permission grant results; may be null.
     */
    void launch(@NonNull String[] permissions, @Nullable Callback callback);

    interface Callback {
        void onResult(@NonNull Map<String, Boolean> result);
    }
}

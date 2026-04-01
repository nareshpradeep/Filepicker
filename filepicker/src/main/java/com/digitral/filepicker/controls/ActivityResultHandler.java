package com.digitral.filepicker.controls;

import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.util.Map;

public class ActivityResultHandler<Input, Result> {

    @NonNull
    public static <Input, Result> ActivityResultHandler<Input, Result> registerForActivityResult(
            @NonNull ActivityResultCaller caller,
            @NonNull ActivityResultContract<Input, Result> contract,
            @Nullable OnActivityResult<Result> onActivityResult) {
        return new ActivityResultHandler<>(caller, contract, onActivityResult);
    }


    @NonNull
    public static <Input, Result> ActivityResultHandler<Input, Result> registerForActivityResult(
            @NonNull ActivityResultCaller caller,
            @NonNull ActivityResultContract<Input, Result> contract) {
        return registerForActivityResult(caller, contract, null);
    }


    @NonNull
    public static ActivityResultHandler<Intent, ActivityResult> registerActivityForResult(
            @NonNull ActivityResultCaller caller) {
        return registerForActivityResult(caller, new ActivityResultContracts.StartActivityForResult());
    }

    @NonNull
    public static ActivityResultHandler<String[], Map<String, Boolean>> registerPermissionForResult(
            @NonNull ActivityResultCaller caller) {
        return registerForActivityResult(caller, new ActivityResultContracts.RequestMultiplePermissions());
    }

    /**
     * Callback interface
     */
    public interface OnActivityResult<O> {

        void onActivityResult(O result);
    }

    private final ActivityResultLauncher<Input> launcher;
    @Nullable
    private OnActivityResult<Result> onActivityResult;

    private ActivityResultHandler(@NonNull ActivityResultCaller caller,
                                  @NonNull ActivityResultContract<Input, Result> contract,
                                  @Nullable OnActivityResult<Result> onActivityResult) {
        this.onActivityResult = onActivityResult;
        this.launcher = caller.registerForActivityResult(contract, this::callOnActivityResult);
    }

    public void setOnActivityResult(@Nullable OnActivityResult<Result> onActivityResult) {
        this.onActivityResult = onActivityResult;
    }


    public void launch(Input input, @Nullable OnActivityResult<Result> onActivityResult) {
        if (onActivityResult != null) {
            this.onActivityResult = onActivityResult;
        }
        launcher.launch(input);
    }


    private void callOnActivityResult(Result result) {
        if (onActivityResult != null) {
            //OoredooApp.contactPageActive = false;
            onActivityResult.onActivityResult(result);
        }
    }
}
package com.digitral.filepicker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.digitral.filepicker.callback.FilePickerCallback;
import com.digitral.filepicker.utils.TraceUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/*public class PhotoOptionBottomSheet extends BottomSheetDialogFragment implements OnClickListener {

    public static final int TYPE_LOCATION = 3;
    private Activity activity;

    private OnClickListener onlClickListener;

    private String title;

    private int type = 0;

    FilePickerCallback callback;

    public static PhotoOptionBottomSheet newInstance(FilePickerCallback callback) {

        PhotoOptionBottomSheet sheet = new PhotoOptionBottomSheet();
        sheet.setCallback(callback);
        return sheet;
    }

    private void setCallback(FilePickerCallback callback) {
        this.callback = callback;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View optionsDialog = inflater.inflate(R.layout.bottom_options_menu, container, false);

        try {

            if (type == TYPE_LOCATION) {

                optionsDialog.findViewById(R.id.ll_remove).setVisibility(View.GONE);
                optionsDialog.findViewById(R.id.ll_location).setVisibility(View.VISIBLE);

            } else if (type != 0) {

                getDialog().setCanceledOnTouchOutside(true);
                getDialog().setCancelable(true);
                TextView tvCamera = optionsDialog.findViewById(R.id.tvCamera);
                TextView tvGallery = optionsDialog.findViewById(R.id.tv_gallery);
                tvCamera.setText(getString(R.string.camera));
                tvGallery.setText(getString(R.string.gallery));

            } else {
                getDialog().setCanceledOnTouchOutside(true);
                getDialog().setCancelable(true);
            }

            TextView tvTitle = optionsDialog.findViewById(R.id.tvTitle);
            if (!TextUtils.isEmpty(title)) {
                tvTitle.setText(title);
            }

            optionsDialog.findViewById(R.id.ll_gallery).setOnClickListener(this);
            optionsDialog.findViewById(R.id.ll_camera).setOnClickListener(this);
            optionsDialog.findViewById(R.id.ll_remove).setOnClickListener(this);
            optionsDialog.findViewById(R.id.ll_location).setOnClickListener(this);

            *//*String imgPath = AppPreferences.getInstance(activity).getFromStore("profilepic_path");
            if (TextUtils.isEmpty(imgPath) || type != 0) {
                (optionsDialog.findViewById(R.id.ll_remove)).setVisibility(View.GONE);
            } else {
                    (optionsDialog.findViewById(R.id.ll_remove)).setVisibility(View.VISIBLE);
            }*//*

        } catch (Exception e) {
            TraceUtils.logException(e);
        }

        return optionsDialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } catch (Exception e) {
            TraceUtils.logException(e);
        }
    }

    public String getString(String key, String defaultValue, Bundle bundle) {
        final String s = bundle.getString(key);
        return (s == null) ? defaultValue : s;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        } catch (Exception e) {
            TraceUtils.logException(e);
        }
    }

    public void setOnclickListener(OnClickListener onClickListener) {
        this.onlClickListener = onClickListener;
    }

    @Override
    public void onClick(View view) {

        try {

            //onlClickListener.onClick(view);
            if(view.getId() == R.id.ll_camera) {
                launchCameraNew(FilePicker.REQ_CODE_CAMERA, null, callback, true, false);
            }

            if(view.getId() == R.id.ll_gallery) {
                launchGalleryNew(FilePicker.REQ_CODE_GALLERY, null, callback, true, false);
            }

            dismiss();

        } catch (Exception e) {
            TraceUtils.logException(e);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void launchGalleryNew(
            int requestCode,
            Object returnObject,
            FilePickerCallback callback,
            boolean cropEnabled,
            boolean compressEnabled
    ) {
        System.gc();

        // initFilePicker(callback);

        if (mFilePicker != null) {
            mFilePicker.compressEnabled(compressEnabled);

            if (compressEnabled) {
                mFilePicker.compressSize(80);
            }

            mFilePicker.cropEnabled(cropEnabled);
            mFilePicker.launchGallery(requestCode, returnObject);
        }
    }

    public void launchCameraNew(
            int requestCode,
            Object returnObject,
            FilePickerCallback callback,
            boolean cropEnabled,
            boolean compressEnabled
    ) {
        System.gc();

        // initFilePicker(callback);

        if (mFilePicker != null) {
            mFilePicker.compressEnabled(compressEnabled);

            if (compressEnabled) {
                mFilePicker.compressSize(80);
            }

            mFilePicker.cropEnabled(cropEnabled);
            mFilePicker.launchCamera(requestCode, returnObject);
        }
    }

}*/


public class PhotoOptionBottomSheet extends BottomSheetDialogFragment
        implements View.OnClickListener {

    public static final int TYPE_LOCATION = 3;

    private String title;
    private int type = 0;

    // 🔥 Listener for communicating user action
    public interface OptionListener {
        void onCameraClick();
        void onGalleryClick();
        void onRemoveClick();
        void onLocationClick();
    }

    private OptionListener optionListener;

    // ✅ Factory method (fixed)
    public static PhotoOptionBottomSheet newInstance() {
        return new PhotoOptionBottomSheet();
    }


    public void setOptionListener(OptionListener listener) {
        this.optionListener = listener;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        View view = inflater.inflate(R.layout.bottom_options_menu, container, false);

        try {

            if (type == TYPE_LOCATION) {
                view.findViewById(R.id.ll_remove).setVisibility(View.GONE);
                view.findViewById(R.id.ll_location).setVisibility(View.VISIBLE);
            } else {
                if (getDialog() != null) {
                    getDialog().setCanceledOnTouchOutside(true);
                    getDialog().setCancelable(true);
                }

                TextView tvCamera = view.findViewById(R.id.tvCamera);
                TextView tvGallery = view.findViewById(R.id.tv_gallery);

                tvCamera.setText(getString(R.string.camera));
                tvGallery.setText(getString(R.string.gallery));
            }

            TextView tvTitle = view.findViewById(R.id.tvTitle);
            if (!TextUtils.isEmpty(title)) {
                tvTitle.setText(title);
            }

            // Click listeners
            view.findViewById(R.id.ll_gallery).setOnClickListener(this);
            view.findViewById(R.id.ll_camera).setOnClickListener(this);
            view.findViewById(R.id.ll_remove).setOnClickListener(this);
            view.findViewById(R.id.ll_location).setOnClickListener(this);

        } catch (Exception e) {
            TraceUtils.logException(e);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow()
                        .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        } catch (Exception e) {
            TraceUtils.logException(e);
        }
    }

    @Override
    public void onClick(View view) {
        try {

            if (optionListener == null) return;

            int id = view.getId();

            if (id == R.id.ll_camera) {
                optionListener.onCameraClick();
            } else if (id == R.id.ll_gallery) {
                optionListener.onGalleryClick();
            } else if (id == R.id.ll_remove) {
                optionListener.onRemoveClick();
            } else if (id == R.id.ll_location) {
                optionListener.onLocationClick();
            }

            dismiss();

        } catch (Exception e) {
            TraceUtils.logException(e);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Avoid crash on some API levels
    }
}

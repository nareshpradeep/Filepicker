package com.digitral.filepicker.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.digitral.filepicker.utils.TraceUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class Utils {

    public static boolean showGrantPermissionsMessage(int[] grantPermissions, String[] permissions, boolean showToast, final Context context) {
        return grantPermissions[0] == PackageManager.PERMISSION_GRANTED;
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static void copyFile(Context context, String src, String dst) {
        if (TextUtils.equals(src, dst)) {
            return;
        }

        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Toast.makeText(
                    context,
                    "Failed to copy " + src + " to " + dst + ": "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    TraceUtils.logException(e);
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    TraceUtils.logException(e);
                }
            }
        }
    }

    public static Bitmap decodeImage(String imagePath, Context activityContext) {

        System.gc();

        BitmapFactory.Options decodeBounds = new BitmapFactory.Options();
        decodeBounds.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imagePath, decodeBounds);
        int numPixels = decodeBounds.outWidth * decodeBounds.outHeight;
        // int maxPixels = 1024 * 768; // requires 6 MB heap
        int density = activityContext.getResources().getDisplayMetrics().densityDpi;

        int maxPixels = 2048 * 1536;
        if (density == DisplayMetrics.DENSITY_MEDIUM) {
            maxPixels = 1024 * 768;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = (numPixels >= maxPixels) ? 2 : 1;

        return BitmapFactory.decodeFile(imagePath, options);

    }

    public static boolean allPermissionGranted(Map<String, Boolean> result) {
        boolean isAllAllowed = true;
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }

        }
        return isAllAllowed;
    }

}

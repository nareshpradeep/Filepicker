package com.digitral.filepicker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;

import androidx.exifinterface.media.ExifInterface;

import com.digitral.filepicker.utils.TraceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCompression {

    private Context context;

    private static final float maxHeight = 600.0f;

    private static final float maxWidth = 600.0f;

    public ImageCompression(Context context) {
        this.context = context;
    }

    public String compressImage(String imagePath, int size, int sampleSize) {

        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(imagePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        float imgRatio = (float) actualWidth / (float) actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }

        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
        options.inDither = false;


        try {
            bmp = BitmapFactory.decodeFile(imagePath, options);
        } catch (OutOfMemoryError exception) {
            TraceUtils.logThrowable(exception);
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565);
        } catch (Throwable exception) {
            TraceUtils.logThrowable(exception);
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        if (scaledBitmap != null) {
            Canvas canvas = new Canvas(scaledBitmap);
            canvas.setMatrix(scaleMatrix);
            canvas.drawBitmap(bmp, middleX - (float) bmp.getWidth() / 2, middleY - (float) bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        }
        if (bmp != null) {
            bmp.recycle();
        }

        ExifInterface exif;
        try {
            exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            if (scaledBitmap != null)
                scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        } catch (Throwable t) {
            TraceUtils.logThrowable(t);
        }

        FileOutputStream out = null;
        String filepath = getFilename();

        try {
            out = new FileOutputStream(filepath);

            //write the compressed bitmap at the destination specified by filename.
            if (scaledBitmap != null)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);

        } catch (Throwable t) {
            TraceUtils.logThrowable(t);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    TraceUtils.logException(e);
                }
            }
        }

        File file = new File(filepath);
        long lengthInKb = file.length() / 1024; //in kb

        if (lengthInKb > size) {
            filepath = new ImageCompression(context).compressImage(file.getAbsolutePath(), size, sampleSize * 2);
        }

        return filepath;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = (float) width * height;
        final float totalReqPixelsCap = (float) (reqWidth * reqHeight * 2);

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private String getFilename() {
        File mediaStorageDir = new File(context.getExternalFilesDir(null)
                 + "/Files/Compressed");

        /*File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getApplicationContext().getPackageName()
                + "/Files/Compressed");*/

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }

        String mImageName = "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
        String uriString = (mediaStorageDir.getAbsolutePath() + "/" + mImageName);

        return uriString;

    }

}
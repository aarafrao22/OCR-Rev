package com.example.ocr;

import android.content.Context;

public class StorageUtil {

    public static String getInternalStoragePath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }
}

package com.manna.codec.utils;

import android.content.Context;
import android.os.Environment;

public class FileUtils {
    /**
     * 读取缓存目录
     *
     * @param context :context
     * @return String
     */
    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            if (context.getExternalCacheDir() == null) {
                return "";
            }
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }
}

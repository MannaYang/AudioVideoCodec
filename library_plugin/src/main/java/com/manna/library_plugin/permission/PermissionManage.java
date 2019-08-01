package com.manna.library_plugin.permission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class PermissionManage {
    private static PermissionGlobalCallback globalConfigCallback;
    private PermissionCallback callback;
    private String[] permissions;
    private Context context;

    public PermissionManage(Context context) {
        this.context = context;
    }

    public static void init(PermissionGlobalCallback callback) {
        globalConfigCallback = callback;
    }

    static PermissionGlobalCallback getGlobalConfigCallback() {
        return globalConfigCallback;
    }

    public static PermissionManage with(Context context) {
        PermissionManage permisson = new PermissionManage(context);
        return permisson;
    }

    public PermissionManage permisson(String[] permissons) {
        this.permissions = permissons;
        return this;
    }

    public PermissionManage callback(PermissionCallback callback) {
        this.callback = callback;
        return this;
    }

    public void request() {
        if (permissions == null || permissions.length <= 0) {
            return;
        }
        PermissionActivity.request(context, permissions, callback);
    }

    /**
     * Jump to Settings page of your application
     *
     * @param context
     */
    public static void startSettingsActivity(Context context) {
        Uri packageURI = Uri.parse("package:" + context.getPackageName());
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

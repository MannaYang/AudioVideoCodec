package com.manna.library_plugin.permission;

public interface PermissionCallback {
    void onPermissionGranted();

    void shouldShowRational(String permission);

    void onPermissionReject(String permission);
}

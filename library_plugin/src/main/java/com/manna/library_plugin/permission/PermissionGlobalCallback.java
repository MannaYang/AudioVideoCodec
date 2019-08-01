package com.manna.library_plugin.permission;

public abstract class PermissionGlobalCallback {
    abstract public void shouldShowRational(String permission, int ration);

    abstract public void onPermissionReject(String permission, int reject);
}

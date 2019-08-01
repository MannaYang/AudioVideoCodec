package com.manna.library_plugin.permission;

/**
 * 权限标记常量
 */
public class PermissionConstants {
    public static final int READ_STORAGE_RATIONALE = 1000;
    public static final int READ_STORAGE_REJECT = 1002;

    public static final int WRITE_STORAGE_RATIONALE = 1003;
    public static final int WRITE_STORAGE_REJECT = 1004;

    public static final int LOCATION_RATIONALE = 1004;
    public static final int LOCATION_REJECT = 1005;

    public static final int CONTACT_RATIONALE = 1006;
    public static final int CONTACT_REJECT = 1007;

    public static final int CAMERA_RATIONALE = 1008;
    public static final int CAMERA_REJECT = 1009;

    public static final int READ_PHONE_RATIONALE = 1010;
    public static final int READ_PHONE_REJECT = 1011;

    public static final int READ_CALENDAR_RATIONALE = 1012;
    public static final int READ_CALENDAR_REJECT = 1013;

    public static final int WRITE_CALENDAR_RATIONALE = 1014;
    public static final int WRITE_CALENDAR_REJECT = 1015;

    public static final int AUDIO_RATIONALE = 1016;
    public static final int AUDIO_REJECT = 1017;

    public static final String MSG_STORAGE_REJECT = "应用缺少存储权限,请在设置页面开启";
    public static final String MSG_LOCATION_REJECT = "应用缺少定位权限,请在设置页面开启";
    public static final String MSG_CONTACT_REJECT = "应用缺少联系人权限,请在设置页面开启";
    public static final String MSG_CAMERA_REJECT = "应用缺少相机权限,请在设置页面开启";
    public static final String MSG_READ_PHONE_REJECT = "应用缺少电话权限,请在设置页面开启";
    public static final String MSG_CALENDAR_REJECT = "应用缺少日历权限,请在设置页面开启";
    public static final String MSG_AUDIO_REJECT = "应用缺少麦克风权限,请在设置页面开启";

    public static final String MSG_STORAGE_RATIONALE = "请开启存储权限";
    public static final String MSG_LOCATION_RATIONALE = "请开启定位权限";
    public static final String MSG_CONTACT_RATIONALE = "请开启联系人权限";
    public static final String MSG_CAMERA_RATIONALE = "请开启相机权限";
    public static final String MSG_READ_PHONE_RATIONALE = "请开启电话权限";
    public static final String MSG_CALENDAR_RATIONALE = "请开启日历权限";
    public static final String MSG_AUDIO_RATIONALE = "请开启麦克风权限";
}

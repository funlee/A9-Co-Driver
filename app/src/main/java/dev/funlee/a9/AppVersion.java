package dev.funlee.a9;

import android.content.Context;
import android.content.pm.PackageManager;

final class AppVersion {
    private AppVersion() {}

    static String name(Context context) {
        try {
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return version == null ? "" : version;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}

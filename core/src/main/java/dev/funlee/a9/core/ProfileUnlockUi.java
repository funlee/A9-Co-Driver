package dev.funlee.a9.core;

/** Cosmetic profile unlocks can be dismissed without opening the profile editor. */
public final class ProfileUnlockUi {
    private ProfileUnlockUi() {}
    public static boolean prompt(String text) {
        String t=VehicleUnlockGuard.normalize(text).replace("檔案頭像","档案头像");
        // Live OCR reads 已 as 己/巴; the unlock sentence and card label identify this popup.
        return t.contains("恭喜")&&t.contains("解锁新")&&t.contains("档案头像");
    }
}

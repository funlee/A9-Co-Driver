package dev.funlee.a9;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

final class FailureScreenshot {
    private static final Executor IO = Executors.newSingleThreadExecutor();

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "failure-screenshot.png"));
    }

    /** Takes ownership of the independent frame copy. A missing frame replaces any older image. */
    static void save(Context context, Bitmap frame) {
        IO.execute(() -> {
            AtomicFile file = file(context);
            FileOutputStream out = null;
            try {
                out = file.startWrite();
                if (frame != null && !frame.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw new IOException("无法编码异常截图");
                }
                file.finishWrite(out);
                SessionState.log(frame == null ? "本次异常未捕获画面" : "已保存异常截图");
            } catch (Exception e) {
                if (out != null) file.failWrite(out);
                file.delete();
                SessionState.log("异常截图保存失败：" + e.getMessage());
            } finally {
                if (frame != null) frame.recycle();
            }
        });
    }

    static void load(Context context, BiConsumer<Bitmap, Long> onLoaded) {
        IO.execute(() -> {
            File image = file(context).getBaseFile();
            Bitmap bitmap = image.length() == 0 ? null : BitmapFactory.decodeFile(image.getPath());
            long savedAt = image.lastModified();
            new Handler(Looper.getMainLooper()).post(() -> onLoaded.accept(bitmap, savedAt));
        });
    }
}

package dev.funlee.a9;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import dev.funlee.a9.core.RunIssue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class FailureScreenshot {
    static final int LIMIT = 5;
    private static final Executor IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String SUFFIX = ".record";
    private static final int VERSION_MARKER = -1;
    private static final int DEVICE_MARKER = -2;

    static final class Entry {
        final long time;
        final String reason, version, androidVersion;
        final int width, height;
        final boolean hasImage;
        private final File file;

        Entry(File file, long time, String reason, String version, String androidVersion,
                int width, int height, boolean hasImage) {
            this.file = file;
            this.time = time;
            this.reason = reason;
            this.version = version;
            this.androidVersion = androidVersion;
            this.width = width;
            this.height = height;
            this.hasImage = hasImage;
        }
    }

    /** Commit the reason and its optional image together, including failures before capture starts. */
    static void save(Context context, Bitmap frame, String detail, Consumer<Boolean> onSaved) {
        long time = System.currentTimeMillis();
        String reason = RunIssue.reason(detail);
        String version = AppVersion.name(context);
        DisplayMetrics display = CaptureSession.displayMetrics(context);
        IO.execute(() -> {
            boolean saved = false;
            try {
                File directory = historyDirectory(context);
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建异常记录目录");
                File[] files = historyFiles(directory);
                // Use a sequence for retention even if the device clock changes or two failures share a timestamp.
                long next = files.length == 0 ? 1 : Long.parseLong(files[0].getName().substring(0, 19)) + 1;
                File file = new File(directory, String.format(Locale.ROOT, "%019d%s", next, SUFFIX));
                Entry entry = new Entry(file, time, reason, version, Build.VERSION.RELEASE,
                        display.widthPixels, display.heightPixels, frame != null);
                try {
                    write(entry, frame);
                    saved = frame != null;
                } catch (IOException | RuntimeException e) {
                    SessionState.log("异常截图保存失败：" + e.getMessage());
                    write(entry, null);
                }
                SessionState.log(saved ? "已保存异常原因及现场截图" : "已保存异常原因，本次未取得截图");
                for (int i = LIMIT - 1; i < files.length; i++) new AtomicFile(files[i]).delete();
                if (files.length >= LIMIT - 1) {
                    new AtomicFile(legacyRecord(context)).delete();
                    new AtomicFile(legacyImage(context)).delete();
                }
            } catch (IOException | RuntimeException e) {
                SessionState.log("异常记录保存失败：" + e.getMessage());
            } finally {
                if (frame != null) frame.recycle();
            }
            if (onSaved != null) {
                boolean hasImage = saved;
                MAIN.post(() -> onSaved.accept(hasImage));
            }
        });
    }

    private static File historyDirectory(Context context) { return new File(context.getFilesDir(), "failure-history"); }
    private static File legacyRecord(Context context) { return new File(context.getFilesDir(), "last-failure-v1"); }
    private static File legacyImage(Context context) { return new File(context.getFilesDir(), "failure-screenshot.png"); }

    private static File[] historyFiles(File directory) throws IOException {
        if (!directory.exists()) return new File[0];
        File[] files = directory.listFiles((dir, name) -> name.matches("[0-9]{19}\\.record"));
        if (files == null) throw new IOException("无法读取异常记录目录");
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());
        return files;
    }

    private static void write(Entry entry, Bitmap frame) throws IOException {
        AtomicFile file = new AtomicFile(entry.file);
        FileOutputStream raw = file.startWrite();
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(raw));
            byte[] reason = entry.reason.getBytes(StandardCharsets.UTF_8);
            out.writeLong(entry.time);
            out.writeInt(DEVICE_MARKER);
            out.writeUTF(entry.version);
            out.writeUTF(entry.androidVersion);
            out.writeInt(entry.width);
            out.writeInt(entry.height);
            out.writeInt(reason.length);
            out.write(reason);
            out.writeBoolean(frame != null);
            if (frame != null && !frame.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException("无法编码画面");
            out.flush();
            file.finishWrite(raw);
        } catch (IOException | RuntimeException e) {
            file.failWrite(raw);
            throw e;
        }
    }

    static List<Entry> readHistory(Context context) throws IOException {
        File[] files = historyFiles(historyDirectory(context));
        List<Entry> entries = new ArrayList<>(LIMIT);
        IOException unreadable = null;
        for (int i = 0; i < Math.min(files.length, LIMIT); i++) {
            try {
                entries.add(readEntry(files[i]));
            } catch (IOException e) {
                unreadable = e;
            }
        }
        if (files.length < LIMIT) {
            try {
                entries.add(readEntry(legacyRecord(context)));
            } catch (FileNotFoundException e) {
                File image = legacyImage(context);
                if (image.exists()) entries.add(new Entry(image, image.lastModified(),
                        RunStatusRecord.legacyScreenshotReason(context), "", "", 0, 0, true));
            } catch (IOException e) {
                unreadable = e;
            }
        }
        if (entries.isEmpty() && unreadable != null) throw unreadable;
        entries.sort(Comparator.comparingLong((Entry entry) -> entry.time).reversed());
        return entries;
    }

    private static DataInputStream open(File file) throws IOException {
        return new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
    }

    private static Entry readEntry(File file) throws IOException {
        try (DataInputStream in = open(file)) {
            return readEntry(file, in);
        }
    }

    private static Entry readEntry(File file, DataInputStream in) throws IOException {
        long time = in.readLong();
        int size = in.readInt();
        String version = "", androidVersion = "";
        int width = 0, height = 0;
        if (size == DEVICE_MARKER) {
            version = in.readUTF();
            androidVersion = in.readUTF();
            width = in.readInt();
            height = in.readInt();
            size = in.readInt();
        } else if (size == VERSION_MARKER) {
            version = in.readUTF();
            size = in.readInt();
        }
        if (size < 0 || size > file.length()) throw new IOException("异常原因长度无效");
        byte[] reason = new byte[size];
        in.readFully(reason);
        return new Entry(file, time, new String(reason, StandardCharsets.UTF_8), version, androidVersion,
                width, height, in.readBoolean());
    }

    static Bitmap readImage(Entry entry) throws IOException {
        if (!entry.hasImage) return null;
        if (entry.file.getName().endsWith(".png")) return BitmapFactory.decodeFile(entry.file.getPath());
        try (DataInputStream in = open(entry.file)) {
            return readEntry(entry.file, in).hasImage ? BitmapFactory.decodeStream(in) : null;
        }
    }

    static void loadHistory(Context context, Consumer<List<Entry>> callback) {
        IO.execute(() -> {
            List<Entry> entries;
            try {
                entries = readHistory(context);
            } catch (IOException e) {
                entries = null;
            }
            List<Entry> result = entries;
            MAIN.post(() -> callback.accept(result));
        });
    }

    static void loadImage(Entry entry, Consumer<Bitmap> callback) {
        IO.execute(() -> {
            Bitmap image;
            try {
                image = readImage(entry);
            } catch (IOException e) {
                image = null;
            }
            Bitmap result = image;
            MAIN.post(() -> callback.accept(result));
        });
    }
}

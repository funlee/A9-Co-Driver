package dev.funlee.a9;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import dev.funlee.a9.core.RunIssue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class FailureScreenshot {
    private static final Executor IO = Executors.newSingleThreadExecutor();
    // Older records store a nonnegative reason length immediately after the timestamp.
    private static final int VERSION_MARKER = -1;
    private static AtomicFile record(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "last-failure-v1"));
    }
    static final class Entry {
        final long time;
        final String reason;
        final String version;
        final Bitmap image;
        Entry(long time,String reason,String version,Bitmap image){
            this.time=time;this.reason=reason;this.version=version;this.image=image;
        }
    }

    /** Commit the reason and its optional image together, including failures before capture starts. */
    static void save(Context context, Bitmap frame, String detail, Consumer<Boolean> onSaved) {
        long time=System.currentTimeMillis();
        String reason=RunIssue.reason(detail);
        String version=AppVersion.name(context);
        IO.execute(() -> {
            boolean saved=false;
            try {
                write(context,time,reason,version,frame);
                saved=frame!=null;
                SessionState.log(saved?"已保存异常原因及现场截图":"已保存异常原因，本次未取得截图");
            } catch (IOException | RuntimeException e) {
                SessionState.log("异常截图保存失败："+e.getMessage());
                try { write(context,time,reason,version,null); }
                catch(IOException | RuntimeException failed){SessionState.log("异常记录保存失败："+failed.getMessage());}
            } finally {
                if(frame!=null)frame.recycle();
            }
            if(onSaved!=null){
                boolean hasImage=saved;
                new Handler(Looper.getMainLooper()).post(()->onSaved.accept(hasImage));
            }
        });
    }
    private static void write(Context context,long time,String reason,String version,Bitmap frame)throws IOException {
        AtomicFile file=record(context);
        FileOutputStream raw=file.startWrite();
        try {
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(raw));
            byte[] bytes=reason.getBytes(StandardCharsets.UTF_8);
            out.writeLong(time);out.writeInt(VERSION_MARKER);out.writeUTF(version);
            out.writeInt(bytes.length);out.write(bytes);
            out.writeBoolean(frame!=null);
            if(frame!=null&&!frame.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("无法编码画面");
            out.flush();file.finishWrite(raw);
        } catch(IOException | RuntimeException e){file.failWrite(raw);throw e;}
    }
    static Entry read(Context context)throws IOException {
        AtomicFile file=record(context);
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(file.openRead()))){
            long time=in.readLong();
            int size=in.readInt();
            String version="";
            if(size==VERSION_MARKER){
                version=in.readUTF();
                size=in.readInt();
            }
            if(size<0 || size>file.getBaseFile().length())throw new IOException("异常原因长度无效");
            byte[] reason=new byte[size];in.readFully(reason);
            return new Entry(time,new String(reason,StandardCharsets.UTF_8),version,in.readBoolean()?BitmapFactory.decodeStream(in):null);
        }catch(FileNotFoundException oldVersion){ /* Read a screenshot from an earlier build. */ }
        File old=new File(context.getFilesDir(),"failure-screenshot.png");
        if(!old.exists())return null;
        String message=RunStatusRecord.legacyScreenshotReason(context);
        return new Entry(old.lastModified(),message,"",BitmapFactory.decodeFile(old.getPath()));
    }
    static void load(Context context,Consumer<Entry> callback) {
        IO.execute(()->{
            Entry result;
            try{result=read(context);}
            catch(IOException e){result=new Entry(0,"无法读取最近一次异常记录","",null);}
            Entry entry=result;
            new Handler(Looper.getMainLooper()).post(()->callback.accept(entry));
        });
    }
}

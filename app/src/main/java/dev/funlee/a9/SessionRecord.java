package dev.funlee.a9;

import android.content.Context;
import android.util.AtomicFile;
import dev.funlee.a9.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** A local resume record; never contains account credentials and is never uploaded. */
final class SessionRecord {
    private static AtomicFile file(Context context){File root=new File(context.getFilesDir(),"runtime");root.mkdirs();return new AtomicFile(new File(root,"task-session.json"));}
    static JSONObject read(Context context)throws Exception{
        AtomicFile file=file(context);if(!file.getBaseFile().isFile())return null;
        JSONObject saved=new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8));
        if(saved.optInt("schema")!=1)throw new IllegalStateException("无法读取此版本的任务记录");
        return saved;
    }
    static boolean available(Context context){try{JSONObject saved=read(context);return saved!=null&&!saved.optBoolean("complete");}catch(Exception e){return false;}}
    static void write(Context context,JSONObject config,TaskCheckpoint queue,TaskStats stats,boolean complete)throws Exception{
        JSONObject saved=new JSONObject().put("schema",1).put("updatedAt",System.currentTimeMillis()).put("complete",complete)
                .put("config",new JSONObject(config.toString())).put("queue",new JSONObject(queue.resume())).put("stats",new JSONObject(stats.snapshot()));
        AtomicFile file=file(context);FileOutputStream out=null;
        try{out=file.startWrite();out.write(saved.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
}

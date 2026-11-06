package dev.funlee.a9;

import android.content.Context;
import android.widget.*;

/** Shared layout for the live overlay and read-only visual checks. */
final class TaskPanelView {
    static LinearLayout create(Context context,TextView label,Button pause,Button stop){
        LinearLayout box=new LinearLayout(context);box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(12,8,12,8);box.setBackgroundColor(0xeef1f5f3);
        label.setTextSize(11);label.setTextColor(0xff123d3a);box.addView(label);
        LinearLayout row=new LinearLayout(context);pause.setTextSize(11);stop.setTextSize(11);
        row.addView(pause,new LinearLayout.LayoutParams(0,-2,1));row.addView(stop,new LinearLayout.LayoutParams(0,-2,1));box.addView(row);
        return box;
    }
}

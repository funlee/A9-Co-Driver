package dev.funlee.a9.core;

/** Whole-frame digits only. Large jumps need a second consistent observation. */
public final class ProgressFilter {
    private int last=-1,pending=-1;
    public int accept(int value) {
        if(value<0 || value>100) return -1;
        if(last>=0 && value<last) return -1;
        if((last<0 && value>10) || (last>=0 && value-last>8)) {
            if(pending<0 || Math.abs(pending-value)>2) { pending=value; return -1; }
        }
        last=value; pending=-1; return value;
    }
    public void reset() { last=-1; pending=-1; }
}

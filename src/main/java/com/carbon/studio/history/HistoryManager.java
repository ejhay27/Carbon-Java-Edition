package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;
import java.util.ArrayList;
import java.util.List;

/** Bounded undo/redo stacks. Paint edits store tiles; no canvas copy is made per pointer move. */
public class HistoryManager {
    private final List<Edit> undo=new ArrayList<>(),redo=new ArrayList<>();
    private int stepLimit=80;
    private long memoryLimit=512L*1024*1024;
    private long bytes;
    public void setLimits(int steps,long memoryBytes){stepLimit=Math.max(1,steps);memoryLimit=Math.max(1024*1024,memoryBytes);trim();}
    public void push(Edit edit){
        if(edit==null)return;redo.clear();undo.add(edit);bytes+=edit.estimatedBytes();trim();
    }
    private void trim(){
        while(undo.size()>stepLimit||(bytes>memoryLimit&&undo.size()>1)){Edit e=undo.remove(0);bytes-=e.estimatedBytes();}
        long redoBytes=redo.stream().mapToLong(Edit::estimatedBytes).sum();
        while(redo.size()>stepLimit||redoBytes>memoryLimit&&redo.size()>1){Edit e=redo.remove(redo.size()-1);redoBytes-=e.estimatedBytes();}
    }
    public boolean undo(CarbonDocument document){
        if(undo.isEmpty())return false;Edit e=undo.remove(undo.size()-1);bytes-=e.estimatedBytes();e.undo(document);redo.add(e);document.markDirty();return true;
    }
    public boolean redo(CarbonDocument document){
        if(redo.isEmpty())return false;Edit e=redo.remove(redo.size()-1);e.redo(document);undo.add(e);bytes+=e.estimatedBytes();document.markDirty();trim();return true;
    }
    public void clear(){undo.clear();redo.clear();bytes=0;}
    public boolean canUndo(){return!undo.isEmpty();}
    public boolean canRedo(){return!redo.isEmpty();}
    public String undoLabel(){return undo.isEmpty()?"":undo.get(undo.size()-1).label();}
    public String redoLabel(){return redo.isEmpty()?"":redo.get(redo.size()-1).label();}
    public List<String> labels(){return undo.stream().map(Edit::label).toList();}
    public long estimatedBytes(){return bytes+redo.stream().mapToLong(Edit::estimatedBytes).sum();}
}

package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Selection;

public final class SelectionEdit implements Edit {
    private final Selection before,after;
    public SelectionEdit(Selection before,Selection after){this.before=before;this.after=after;}
    @Override public String label(){return "Selection";}
    @Override public long estimatedBytes(){return 64+(before==null?0:before.getPoints().size()*8L)+(after==null?0:after.getPoints().size()*8L);}
    @Override public void undo(CarbonDocument document){document.setSelection(before);}
    @Override public void redo(CarbonDocument document){document.setSelection(after);}
}

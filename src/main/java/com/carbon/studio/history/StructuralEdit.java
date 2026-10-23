package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;

public final class StructuralEdit implements Edit {
    private final String label;
    private final DocumentSnapshot before,after;
    public StructuralEdit(String label,DocumentSnapshot before,DocumentSnapshot after){this.label=label;this.before=before;this.after=after;}
    @Override public String label(){return label;}
    @Override public long estimatedBytes(){return Math.max(1,before.estimatedBytes()+after.estimatedBytes());}
    @Override public void undo(CarbonDocument document){before.restore(document);}
    @Override public void redo(CarbonDocument document){after.restore(document);}
}

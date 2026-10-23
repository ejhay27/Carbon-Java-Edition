package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;

public interface Edit {
    String label();
    long estimatedBytes();
    void undo(CarbonDocument document);
    void redo(CarbonDocument document);
}

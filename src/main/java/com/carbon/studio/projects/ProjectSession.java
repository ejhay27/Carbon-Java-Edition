package com.carbon.studio.projects;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.history.HistoryManager;
import java.util.UUID;

/** One open document tab and its independent undo/redo history. */
public final class ProjectSession {
    private final String id=UUID.randomUUID().toString();
    private final CarbonDocument document;
    private final HistoryManager history;
    public ProjectSession(CarbonDocument document){this(document,new HistoryManager());}
    public ProjectSession(CarbonDocument document,HistoryManager history){if(document==null)throw new IllegalArgumentException("Document is required");this.document=document;this.history=history;}
    public String id(){return id;}
    public CarbonDocument document(){return document;}
    public HistoryManager history(){return history;}
    public String displayName(){return document.getName()+(document.isDirty()?" •":"");}
}

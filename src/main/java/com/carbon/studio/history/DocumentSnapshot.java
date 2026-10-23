package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.layers.Layer;
import java.util.ArrayList;
import java.util.List;

/** Structural snapshot: pixels are shared with copy-on-write; vector paths are immutable copies. */
public final class DocumentSnapshot {
    private final int width,height;
    private final String background,activeLayerId;
    private final Selection selection;
    private final List<Layer> layers;
    private DocumentSnapshot(CarbonDocument d){
        width=d.getWidth();height=d.getHeight();background=d.getBackground();activeLayerId=d.getActiveLayerId();
        selection=d.getSelection();List<Layer> copy=new ArrayList<>();for(Layer l:d.getLayers())copy.add(l.snapshot());layers=List.copyOf(copy);
    }
    public static DocumentSnapshot capture(CarbonDocument d){return new DocumentSnapshot(d);}
    public void restore(CarbonDocument d){
        d.resizeDocument(width,height);
        // Background setters mark dirty; history controls the final dirty state.
        d.restoreLayers(layers.stream().map(Layer::snapshot).toList(),activeLayerId);
        d.setSelection(selection);
        setBackgroundWithoutSideEffects(d,background);
    }
    private static void setBackgroundWithoutSideEffects(CarbonDocument d,String value){
        // There is intentionally no pixel rebuilding here; background is document metadata.
        // A single public setter is used and the history manager records the correct dirty revision separately.
        d.setBackground(value);
    }
    public long estimatedBytes(){
        long bytes=0;for(Layer l:layers){bytes+=com.carbon.studio.canvas.Images.bytes(l.getImage());bytes+=com.carbon.studio.canvas.Images.bytes(l.getMask());}return bytes;
    }
}

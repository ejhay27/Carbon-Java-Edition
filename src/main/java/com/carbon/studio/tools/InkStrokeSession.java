package com.carbon.studio.tools;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.history.DocumentSnapshot;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.history.StructuralEdit;
import com.carbon.studio.layers.InkStroke;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerType;
import java.awt.Rectangle;

/** One undoable Crisp Ink path with resolution-independent point data. */
public final class InkStrokeSession {
    private final CarbonDocument document;private final HistoryManager history;private final DocumentSnapshot before;private final Layer layer;private final InkStroke stroke;private final Brush brush;
    private double x,y;private boolean started,finished;private Rectangle dirty=new Rectangle();
    public InkStrokeSession(CarbonDocument d,String layerId,Brush brush,String color,HistoryManager history){
        if(brush.getMode()!=BrushMode.CRISP)throw new IllegalArgumentException("Crisp Ink session requires a crisp brush");Layer found=d.findLayer(layerId);if(found==null||found.getType()!=LayerType.INK)throw new IllegalStateException("Choose a Crisp Ink layer before using a crisp brush.");if(found.isLocked())throw new IllegalStateException("Layer is locked.");
        this.document=d;this.history=history;this.brush=brush.copy();before=DocumentSnapshot.capture(d);layer=found.deepCopy();d.replaceLayer(layer);stroke=new InkStroke(color,brush.getSize(),brush.getOpacity(),brush.getTaperStart(),brush.getTaperEnd());
    }
    public void append(StrokeSample sample){
        if(finished)throw new IllegalStateException("Stroke has finished");if(!started){x=sample.x();y=sample.y();started=true;}else{double response=1-brush.getSmoothing()*.88;x+=(sample.x()-x)*response;y+=(sample.y()-y)*response;}stroke.addPoint(x,y,sample.pressure());int r=(int)Math.ceil(brush.getSize()+4);Rectangle next=new Rectangle((int)x-r,(int)y-r,r*2+1,r*2+1);dirty=dirty.isEmpty()?next:dirty.union(next);
    }
    public void finish(){if(finished)return;finished=true;if(stroke.getPoints().isEmpty())return;layer.getStrokes().add(stroke);history.push(new StructuralEdit("Crisp Ink stroke",before,DocumentSnapshot.capture(document)));document.markDirty();}
    public Rectangle getDirtyBounds(){return new Rectangle(dirty);}
}

package com.carbon.studio.tools;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerService;
import com.carbon.studio.layers.LayerType;
import java.awt.Color;
import org.junit.Test;

public class DrawingEngineTest {
    private static Brush brush(){Brush b=new Brush();b.setName("Test");b.setSize(5);b.setHardness(1);b.setPressureSize(0);b.setSmoothing(0);b.setSpacing(.2);b.setOpacity(1);b.setFlow(1);return b;}
    @Test public void rasterStrokeChangesPixelsAndUndoRedoRestoresThem(){
        CarbonDocument d=CarbonDocument.create(40,30,"Draw",null);HistoryManager h=new HistoryManager();String id=d.getActiveLayerId();
        StrokeSession s=new StrokeSession(d,id,brush(),"#ff0000",false,42,new Symmetry(),h);s.append(new StrokeSample(10,10,1,0,0,0));s.append(new StrokeSample(20,10,1,0,0,16));assertNotNull(s.finish());
        assertTrue("center should contain paint",(d.getActiveLayer().getImage().getRGB(10,10)>>>24)>0);assertTrue(h.canUndo());assertTrue(h.undo(d));assertEquals(0,d.getActiveLayer().getImage().getRGB(10,10));assertTrue(h.redo(d));assertTrue((d.getActiveLayer().getImage().getRGB(10,10)>>>24)>0);
    }
    @Test public void strokeAtEdgeUsesPartialHistoryTiles(){
        CarbonDocument d=CarbonDocument.create(70,68,"Edge",null);HistoryManager h=new HistoryManager();StrokeSession s=new StrokeSession(d,d.getActiveLayerId(),brush(),"#00ff00",false,1,new Symmetry(),h);s.append(new StrokeSample(68,66,1));s.finish();assertTrue(h.undo(d));assertTrue(h.redo(d));
    }
    @Test public void eraserAndAlphaLockRespectLayerContract(){
        CarbonDocument d=CarbonDocument.create(20,20,"Erase",null);HistoryManager h=new HistoryManager();Layer l=d.getActiveLayer();l.getImage().setRGB(5,5,Color.BLUE.getRGB());
        StrokeSession erase=new StrokeSession(d,l.getId(),brush(),"#000000",true,1,new Symmetry(),h);erase.append(new StrokeSample(5,5,1));erase.finish();assertEquals(0,l.getImage().getRGB(5,5)>>>24);
        l.setAlphaLock(true);StrokeSession paint=new StrokeSession(d,l.getId(),brush(),"#ff0000",false,2,new Symmetry(),h);paint.append(new StrokeSample(5,5,1));paint.finish();assertEquals(0,l.getImage().getRGB(5,5)>>>24);
    }
    @Test public void selectionClipsFloodFill(){
        CarbonDocument d=CarbonDocument.create(20,20,"Fill",null);HistoryManager h=new HistoryManager();d.setSelection(Selection.rectangle(0,0,10,20));RasterEditingService tools=new RasterEditingService();int count=tools.floodFill(d,h,d.getActiveLayerId(),2,2,"#336699",0,false,0);assertTrue(count>0);assertTrue((d.getActiveLayer().getImage().getRGB(2,2)>>>24)>0);assertEquals(0,d.getActiveLayer().getImage().getRGB(15,2));
    }
    @Test public void scalingTransformsInkAndSelectionWithRasterContent(){
        CarbonDocument d=CarbonDocument.create(10,10,"Vectors",null);HistoryManager h=new HistoryManager();Layer ink=Layer.ink("Ink");com.carbon.studio.layers.InkStroke stroke=new com.carbon.studio.layers.InkStroke("#000000",4,1,0,0);stroke.addPoint(2,3,.5);stroke.addPoint(4,5,1);ink.getStrokes().add(stroke);d.insertLayer(ink,1);d.setSelection(Selection.rectangle(1,1,3,4));new LayerService().scaleImage(d,h,20,20);assertEquals(4,stroke.getPoints().get(0),.0001);assertEquals(6,stroke.getPoints().get(1),.0001);assertEquals(8,stroke.getSize(),.0001);assertEquals(2,d.getSelection().getPoints().get(0),.0001);assertTrue(h.undo(d));assertEquals(10,d.getWidth());assertEquals(2,d.getLayers().get(1).getStrokes().get(0).getPoints().get(0),.0001);
    }
    @Test public void layerDuplicateMergeAndResizeAreUndoable(){
        CarbonDocument d=CarbonDocument.create(12,10,"Layers",null);HistoryManager h=new HistoryManager();LayerService service=new LayerService();String first=d.getActiveLayerId();d.getActiveLayer().getImage().setRGB(1,1,Color.RED.getRGB());service.add(d,h,LayerType.RASTER);String second=d.getActiveLayerId();assertNotEquals(first,second);service.duplicate(d,h,second);assertEquals(3,d.getLayers().size());service.mergeDown(d,h,d.getActiveLayerId());assertEquals(2,d.getLayers().size());service.resizeCanvas(d,h,20,16,.5,.5);assertEquals(20,d.getWidth());while(h.canUndo())h.undo(d);assertEquals(12,d.getWidth());assertEquals(1,d.getLayers().size());assertEquals(Color.RED.getRGB(),d.getActiveLayer().getImage().getRGB(1,1));
    }
}

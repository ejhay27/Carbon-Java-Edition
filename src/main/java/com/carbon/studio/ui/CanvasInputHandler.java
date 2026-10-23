package com.carbon.studio.ui;

import com.carbon.studio.canvas.RasterCompositor;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.projects.ProjectSession;
import com.carbon.studio.tools.Brush;
import com.carbon.studio.tools.BrushMode;
import com.carbon.studio.tools.ColorUtil;
import com.carbon.studio.tools.InkStrokeSession;
import com.carbon.studio.tools.RasterEditingService;
import com.carbon.studio.tools.StrokeSample;
import com.carbon.studio.tools.StrokeSession;
import com.carbon.studio.tools.ToolType;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/** Maps JavaFX mouse input to real document commands. Pressure defaults to 1 for mouse input. */
public class CanvasInputHandler {
    private final DocumentCanvas view;private final Supplier<ProjectSession>sessions;private final EditorState state;private final Runnable changed;private final Consumer<String>status;private final RasterEditingService editing=new RasterEditingService();
    private StrokeSession stroke;private InkStrokeSession inkStroke;private double startX,startY,lastSceneX,lastSceneY;private boolean panning;
    public CanvasInputHandler(DocumentCanvas view,Supplier<ProjectSession>sessions,EditorState state,Runnable changed,Consumer<String>status){this.view=view;this.sessions=sessions;this.state=state;this.changed=changed;this.status=status;view.canvasNode().setOnMousePressed(this::pressed);view.canvasNode().setOnMouseDragged(this::dragged);view.canvasNode().setOnMouseReleased(this::released);}
    private void pressed(MouseEvent e){
        ProjectSession s=sessions.get();if(s==null)return;startX=e.getX();startY=e.getY();lastSceneX=e.getSceneX();lastSceneY=e.getSceneY();
        if(e.getButton()==MouseButton.MIDDLE||state.tool()==ToolType.HAND){panning=true;return;}
        if(e.getButton()==MouseButton.SECONDARY&&state.tool()==ToolType.ZOOM){s.document().getViewport().setZoom(s.document().getViewport().getZoom()/1.25);view.refreshTransform();return;}
        try{
            switch(state.tool()){
                case BRUSH,ERASER -> {
                    Brush b=state.tool()==ToolType.ERASER?state.eraser():state.brush();if(b==null)return;
                    if(b.getMode()==BrushMode.CRISP&&state.tool()==ToolType.BRUSH){inkStroke=new InkStrokeSession(s.document(),s.document().getActiveLayerId(),b,state.color(),s.history());inkStroke.append(sample(e,1));view.renderRegion(inkStroke.getDirtyBounds());}
                    else{stroke=new StrokeSession(s.document(),s.document().getActiveLayerId(),b,state.color(),state.tool()==ToolType.ERASER,Double.doubleToLongBits(Math.random()),state.symmetry(),s.history());stroke.append(sample(e,1));view.renderRegion(stroke.getDirtyBounds());}
                }
                case PICKER -> {int argb=RasterCompositor.samplePixel(s.document(),clamp((int)e.getX(),s.document().getWidth()),clamp((int)e.getY(),s.document().getHeight()));state.setColor(ColorUtil.hex(new java.awt.Color(argb,true)));changed.run();status.accept("Picked "+state.color());}
                case FILL -> {int n=editing.floodFill(s.document(),s.history(),s.document().getActiveLayerId(),(int)e.getX(),(int)e.getY(),state.color(),.18,e.isControlDown(),0);view.renderFullAsync();changed.run();status.accept("Filled "+n+" pixels");}
                case ZOOM -> {s.document().getViewport().setZoom(s.document().getViewport().getZoom()*1.25);view.refreshTransform();}
                case TEXT -> {TextInputDialog dialog=new TextInputDialog("Text");dialog.setTitle("Rasterized text");dialog.setHeaderText("Place text on the active raster layer");Optional<String>text=dialog.showAndWait();text.ifPresent(value->{editing.text(s.document(),s.history(),s.document().getActiveLayerId(),e.getX(),e.getY(),value,"System",Math.max(8,(int)state.brush().getSize()*3),state.color(),false,false);view.renderFullAsync();changed.run();});}
                default -> { }
            }
        }catch(RuntimeException ex){stroke=null;inkStroke=null;status.accept(ex.getMessage());}
        e.consume();
    }
    private void dragged(MouseEvent e){
        ProjectSession s=sessions.get();if(s==null)return;if(panning){s.document().getViewport().panBy(e.getSceneX()-lastSceneX,e.getSceneY()-lastSceneY);lastSceneX=e.getSceneX();lastSceneY=e.getSceneY();view.refreshTransform();e.consume();return;}
        try{if(stroke!=null){stroke.append(sample(e,1));view.renderRegion(stroke.getDirtyBounds());}else if(inkStroke!=null){inkStroke.append(sample(e,1));view.renderRegion(inkStroke.getDirtyBounds());}}catch(RuntimeException ex){status.accept(ex.getMessage());}e.consume();
    }
    private void released(MouseEvent e){
        ProjectSession s=sessions.get();if(s==null)return;if(panning){panning=false;return;}
        try{
            if(stroke!=null){stroke.finish();stroke=null;changed.run();}
            if(inkStroke!=null){inkStroke.finish();inkStroke=null;changed.run();}
            if(state.tool()==ToolType.LINE||state.tool()==ToolType.RECT||state.tool()==ToolType.ELLIPSE){Brush b=state.brush();if(b.getMode()==BrushMode.CRISP)throw new IllegalStateException("Raster shapes need a non-crisp brush.");editing.shape(s.document(),s.history(),s.document().getActiveLayerId(),state.tool(),startX,startY,e.getX(),e.getY(),b,state.color(),false,Double.doubleToLongBits(Math.random()),state.symmetry(),e.isShiftDown(),e.isControlDown());view.renderFullAsync();changed.run();}
            else if(state.tool()==ToolType.SELECT){Selection selection=Selection.rectangle(startX,startY,e.getX(),e.getY());editing.setSelection(s.document(),s.history(),selection);view.refreshOverlay();changed.run();}
            else if(state.tool()==ToolType.GRADIENT){java.awt.Color c=ColorUtil.parse(state.color());editing.gradient(s.document(),s.history(),s.document().getActiveLayerId(),e.isShiftDown(),startX,startY,e.getX(),e.getY(),List.of(new RasterEditingService.GradientStop(0,c),new RasterEditingService.GradientStop(1,new java.awt.Color(0,0,0,0))));view.renderFullAsync();changed.run();}
        }catch(RuntimeException ex){status.accept(ex.getMessage());}e.consume();
    }
    private static StrokeSample sample(MouseEvent e,double pressure){return new StrokeSample(e.getX(),e.getY(),pressure,0,0,System.currentTimeMillis());}
    private static int clamp(int value,int max){return Math.max(0,Math.min(max-1,value));}
}

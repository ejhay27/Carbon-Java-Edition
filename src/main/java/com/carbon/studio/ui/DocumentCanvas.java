package com.carbon.studio.ui;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.RasterCompositor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/** Interactive document surface with a cached composite and dirty-region updates. */
public class DocumentCanvas extends StackPane implements AutoCloseable {
    private final Canvas canvas=new Canvas();private final ExecutorService renderer=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"carbon-render");t.setDaemon(true);return t;});
    private final AtomicLong generation=new AtomicLong();private CarbonDocument document;private WritableImage fxImage;private BufferedImage composite;
    public DocumentCanvas(){getChildren().add(canvas);setManaged(false);setStyle("-fx-background-color:linear-gradient(from 0% 0% to 100% 100%, #30343d 25%, transparent 25%),linear-gradient(from 0% 100% to 100% 0%, #30343d 25%, #262a31 25%);");}
    public void setDocument(CarbonDocument value){generation.incrementAndGet();document=value;composite=null;fxImage=null;if(value==null){canvas.setWidth(1);canvas.setHeight(1);canvas.getGraphicsContext2D().clearRect(0,0,1,1);return;}canvas.setWidth(value.getWidth());canvas.setHeight(value.getHeight());resize(value.getWidth(),value.getHeight());refreshTransform();renderFullAsync();}
    public CarbonDocument document(){return document;}
    public void renderFullAsync(){
        CarbonDocument d=document;if(d==null)return;long ticket=generation.incrementAndGet();CarbonDocument snapshot=d.copyForIo();
        renderer.execute(()->{BufferedImage rendered;try{rendered=RasterCompositor.composite(snapshot);}catch(RuntimeException e){return;}Platform.runLater(()->{if(document!=d||generation.get()!=ticket)return;composite=rendered;fxImage=new WritableImage(rendered.getWidth(),rendered.getHeight());writePixels(0,0,rendered.getWidth(),rendered.getHeight());redraw();});});
    }
    public void renderRegion(Rectangle requested){
        CarbonDocument d=document;if(d==null||requested==null||requested.isEmpty())return;Rectangle clip=requested.intersection(new Rectangle(0,0,d.getWidth(),d.getHeight()));if(clip.isEmpty())return;
        if(composite==null||fxImage==null||composite.getWidth()!=d.getWidth()||composite.getHeight()!=d.getHeight()){renderFullAsync();return;}
        generation.incrementAndGet();BufferedImage region=RasterCompositor.compositeRegion(d,clip);composite.setRGB(clip.x,clip.y,clip.width,clip.height,region.getRGB(0,0,clip.width,clip.height,null,0,clip.width),0,clip.width);writePixels(clip.x,clip.y,clip.width,clip.height);canvas.getGraphicsContext2D().clearRect(clip.x,clip.y,clip.width,clip.height);canvas.getGraphicsContext2D().drawImage(fxImage,clip.x,clip.y,clip.width,clip.height,clip.x,clip.y,clip.width,clip.height);drawOverlay();
    }
    private void writePixels(int x,int y,int w,int h){int[]pixels=composite.getRGB(x,y,w,h,null,0,w);fxImage.getPixelWriter().setPixels(x,y,w,h,PixelFormat.getIntArgbInstance(),pixels,0,w);}
    private void redraw(){if(fxImage!=null){canvas.getGraphicsContext2D().clearRect(0,0,canvas.getWidth(),canvas.getHeight());canvas.getGraphicsContext2D().drawImage(fxImage,0,0);drawOverlay();}}
    private void drawOverlay(){
        if(document==null)return;var gc=canvas.getGraphicsContext2D();gc.save();gc.setLineWidth(Math.max(1/document.getViewport().getZoom(),.5));gc.setLineDashes(5,4);gc.setStroke(javafx.scene.paint.Color.web("#72e7ff"));var selection=document.getSelection();
        if(selection!=null&&!selection.getPoints().isEmpty()){var p=selection.getPoints();gc.beginPath();gc.moveTo(p.get(0),p.get(1));for(int i=2;i+1<p.size();i+=2)gc.lineTo(p.get(i),p.get(i+1));gc.closePath();gc.stroke();}gc.restore();
    }
    public void refreshOverlay(){redraw();}
    public void refreshTransform(){
        if(document==null)return;var v=document.getViewport();getTransforms().setAll(new Translate(v.getPanX(),v.getPanY()),new Rotate(v.getRotation()));if(v.isFlip())getTransforms().add(new Scale(-1,1));getTransforms().add(new Scale(v.getZoom(),v.getZoom()));getTransforms().add(new Translate(-document.getWidth()/2.0,-document.getHeight()/2.0));
    }
    public void fit(double width,double height){if(document==null)return;document.getViewport().fit(document.getWidth(),document.getHeight(),width,height,false);refreshTransform();}
    public Canvas canvasNode(){return canvas;}
    @Override public void close(){generation.incrementAndGet();renderer.shutdownNow();}
}

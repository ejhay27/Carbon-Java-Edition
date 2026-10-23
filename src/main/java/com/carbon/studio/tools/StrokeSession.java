package com.carbon.studio.tools;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.history.TilePixelEdit;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerType;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** One incremental raster stroke. No document copy is made for individual pointer samples. */
public final class StrokeSession {
    private final CarbonDocument document;
    private final Layer layer;
    private final Brush brush;
    private final Color color;
    private final boolean eraser;
    private final Symmetry symmetry;
    private final HistoryManager history;
    private final TilePixelEdit.Builder tiles;
    private final SplittableRandom random;
    private final Area selection;
    private boolean started,finished;
    private double smoothX,smoothY,lastDabX,lastDabY,lastPressure=1,distance;
    private long lastTime;
    private int steps;
    private Rectangle dirty;
    private Color carry;

    public StrokeSession(CarbonDocument document,String layerId,Brush brush,String color,boolean eraser,long seed,Symmetry symmetry,HistoryManager history){
        this.document=document;this.brush=brush.copy();this.color=ColorUtil.parse(color);this.eraser=eraser;
        this.symmetry=symmetry==null?new Symmetry():symmetry.copy();this.history=history;this.random=new SplittableRandom(seed);
        Layer found=document.findLayer(layerId);if(found==null)throw new IllegalArgumentException("Stroke layer does not exist: "+layerId);
        if(found.isLocked())throw new IllegalStateException("Layer is locked.");
        if(found.getType()!=LayerType.RASTER||found.getImage()==null)throw new IllegalStateException("Raster strokes require a raster layer.");
        if(brush.getMode()==BrushMode.CRISP)throw new IllegalArgumentException("Crisp brushes require an ink stroke session.");
        if(found.isImageShared()){found=found.deepCopy();document.replaceLayer(found);}
        this.layer=found;this.tiles=new TilePixelEdit.Builder(eraser?"Eraser":"Brush: "+brush.getName(),layerId);
        this.selection=document.getSelection()==null?null:document.getSelection().toArea(document.getWidth(),document.getHeight());
        this.carry=this.color;
    }
    public void append(StrokeSample sample){
        if(finished)throw new IllegalStateException("Stroke has already finished.");
        if(!started){started=true;smoothX=sample.x();smoothY=sample.y();lastDabX=smoothX;lastDabY=smoothY;lastPressure=sample.pressure();lastTime=sample.timeMillis();emit(smoothX,smoothY,lastPressure,lastTime);return;}
        double responsiveness=1-brush.getSmoothing()*0.88;
        smoothX+=(sample.x()-smoothX)*responsiveness;smoothY+=(sample.y()-smoothY)*responsiveness;
        double dx=smoothX-lastDabX,dy=smoothY-lastDabY,segment=Math.hypot(dx,dy);
        double baseSize=Math.max(0.5,brush.getSize());double spacing=Math.max(0.1,baseSize*brush.getSpacing());
        if(segment+distance<spacing){lastPressure=sample.pressure();lastTime=sample.timeMillis();return;}
        double travel=segment+distance;int count=Math.min(4096,(int)Math.floor(travel/spacing));if(count<=0)return;
        double start=distance;
        for(int i=0;i<count;i++){
            double d=(i+1)*spacing-start;if(d<0||d>segment)continue;double t=segment==0?0:d/segment;
            double x=lastDabX+dx*t,y=lastDabY+dy*t,p=lastPressure+(sample.pressure()-lastPressure)*t;
            emit(x,y,p,lastTime+(long)((sample.timeMillis()-lastTime)*t));
        }
        double consumed=(count*spacing-distance);distance=Math.max(0,segment-consumed);
        lastDabX=smoothX;lastDabY=smoothY;lastPressure=sample.pressure();lastTime=sample.timeMillis();
    }
    private void emit(double x,double y,double pressure,long time){
        double speed=time==lastTime?0:Math.hypot(x-lastDabX,y-lastDabY)/Math.max(1,Math.abs(time-lastTime));
        double pressureFactor=(1-brush.getPressureSize())+brush.getPressureSize()*(brush.getMinSize()+(1-brush.getMinSize())*pressure);
        double taper=brush.getTaperStart()>0?Math.min(1,(steps+1)/(brush.getTaperStart()*8+1)):1;
        double size=brush.getSize()*pressureFactor*taper*(1+brush.sizeJitter()*(random.nextDouble()*2-1));
        size*=Math.max(0.1,1-brush.speedSize()*Math.min(1,speed/4));size=Math.max(0.2,size);
        double opacity=brush.getOpacity()*brush.getFlow()*(1-brush.getPressureOpacity()+brush.getPressureOpacity()*pressure);
        opacity*=1+brush.opacityJitter()*(random.nextDouble()*2-1);opacity=Brush.clamp01(opacity);
        double angle=brush.getAngle()+brush.angleJitter()*180*(random.nextDouble()*2-1);
        if(brush.isFollowDirection())angle+=Math.toDegrees(Math.atan2(y-lastDabY,x-lastDabX));
        Color dabColor=color;
        if(brush.getMode()==BrushMode.MIX||brush.getMode()==BrushMode.SMUDGE){
            int ix=Math.max(0,Math.min(layer.getImage().getWidth()-1,(int)Math.round(x)));
            int iy=Math.max(0,Math.min(layer.getImage().getHeight()-1,(int)Math.round(y)));
            Color under=new Color(layer.getImage().getRGB(ix,iy),true);
            double pick=brush.getPickup();carry=new Color((int)(carry.getRed()*(1-pick)+under.getRed()*pick),(int)(carry.getGreen()*(1-pick)+under.getGreen()*pick),(int)(carry.getBlue()*(1-pick)+under.getBlue()*pick),255);
            dabColor=brush.getMode()==BrushMode.MIX?mix(color,carry,Math.min(1,brush.getWetness()+0.25)):carry;
            if(brush.getMode()==BrushMode.SMUDGE)opacity*=Math.max(0.1,1-brush.getWetness()*0.4);
        }
        if(brush.hueJitter()>0||brush.satJitter()>0||brush.valJitter()>0)dabColor=ColorUtil.jitter(dabColor,brush.hueJitter(),brush.satJitter(),brush.valJitter(),random::nextDouble);
        int count=Math.max(1,brush.getCount());
        for(int i=0;i<count;i++){
            double scatter=brush.getScatter()*size,ox=(random.nextDouble()*2-1)*scatter,oy=(random.nextDouble()*2-1)*scatter;
            double px=x+ox,py=y+oy;
            for(Point2D.Double p:symmetryPoints(px,py))stamp(p.x,p.y,size,angle,dabColor,(float)opacity);
        }
        steps++;
    }
    private static Color mix(Color a,Color b,double t){return new Color((int)(a.getRed()*(1-t)+b.getRed()*t),(int)(a.getGreen()*(1-t)+b.getGreen()*t),(int)(a.getBlue()*(1-t)+b.getBlue()*t),255);}
    private List<Point2D.Double> symmetryPoints(double x,double y){
        List<Point2D.Double> out=new ArrayList<>();double cx=symmetry.getCx()*document.getWidth(),cy=symmetry.getCy()*document.getHeight();
        switch(symmetry.getMode()){
            case VERTICAL -> {out.add(new Point2D.Double(x,y));out.add(new Point2D.Double(2*cx-x,y));}
            case HORIZONTAL -> {out.add(new Point2D.Double(x,y));out.add(new Point2D.Double(x,2*cy-y));}
            case QUAD -> {out.add(new Point2D.Double(x,y));out.add(new Point2D.Double(2*cx-x,y));out.add(new Point2D.Double(x,2*cy-y));out.add(new Point2D.Double(2*cx-x,2*cy-y));}
            case RADIAL -> {for(int i=0;i<symmetry.getCount();i++){double a=i*Math.PI*2/symmetry.getCount(),dx=x-cx,dy=y-cy;out.add(new Point2D.Double(cx+dx*Math.cos(a)-dy*Math.sin(a),cy+dx*Math.sin(a)+dy*Math.cos(a)));}}
            default -> out.add(new Point2D.Double(x,y));
        }return out;
    }
    private void stamp(double x,double y,double size,double angle,Color c,float opacity){
        int radius=(int)Math.ceil(size*1.2+4);Rectangle bounds=new Rectangle((int)Math.floor(x)-radius,(int)Math.floor(y)-radius,radius*2+1,radius*2+1);
        tiles.captureBefore(layer.getImage(),bounds);
        BrushStamper.stamp(layer.getImage(),x,y,size,angle,c,opacity,brush,eraser,selection,random);
        dirty=dirty==null?bounds:dirty.union(bounds);
    }
    public TilePixelEdit finish(){
        if(finished)return null;finished=true;
        if(layer.isAlphaLock())tiles.preserveOriginalAlpha(layer.getImage());
        TilePixelEdit edit=tiles.build(layer.getImage());if(edit!=null){history.push(edit);document.markDirty();}return edit;
    }
    public Rectangle getDirtyBounds(){return dirty==null?new Rectangle():new Rectangle(dirty);}
    public Layer getLayer(){return layer;}
}

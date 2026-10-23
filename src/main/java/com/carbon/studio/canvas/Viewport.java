package com.carbon.studio.canvas;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/** View-only transform; document pixels are never resampled by navigation. */
public class Viewport {
    public static final double MIN_ZOOM=0.02,MAX_ZOOM=64;
    private double zoom=1,panX,panY,rotation;
    private boolean flip;
    public double getZoom(){return zoom;}
    public void setZoom(double zoom){this.zoom=clamp(zoom,MIN_ZOOM,MAX_ZOOM);}
    public double getPanX(){return panX;}
    public double getPanY(){return panY;}
    public void setPan(double x,double y){panX=x;panY=y;}
    public double getRotation(){return rotation;}
    public void setRotation(double degrees){rotation=((degrees+180)%360+360)%360-180;}
    public boolean isFlip(){return flip;}
    public void setFlip(boolean flip){this.flip=flip;}
    public void panBy(double dx,double dy){panX+=dx;panY+=dy;}
    public void zoomAt(double factor,double sx,double sy){
        double next=clamp(zoom*factor,MIN_ZOOM,MAX_ZOOM),k=next/zoom;
        panX=sx+(panX-sx)*k;panY=sy+(panY-sy)*k;zoom=next;
    }
    public void fit(int docW,int docH,double viewportW,double viewportH,boolean actual){
        if(actual){zoom=1;}else{double pad=48;zoom=clamp(Math.min((viewportW-pad)/docW,(viewportH-pad)/docH),MIN_ZOOM,MAX_ZOOM);rotation=0;}
        panX=viewportW/2;panY=viewportH/2;
    }
    public AffineTransform transform(int docW,int docH){
        AffineTransform t=new AffineTransform();
        t.translate(panX,panY);t.rotate(Math.toRadians(rotation));
        if(flip)t.scale(-1,1);
        t.scale(zoom,zoom);t.translate(-docW/2.0,-docH/2.0);return t;
    }
    public Point2D toDocument(double sx,double sy,int docW,int docH){
        try{return transform(docW,docH).inverseTransform(new Point2D.Double(sx,sy),null);}
        catch(NoninvertibleTransformException e){return new Point2D.Double();}
    }
    public Point2D toScreen(double x,double y,int docW,int docH){return transform(docW,docH).transform(new Point2D.Double(x,y),null);}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    public Viewport copy(){Viewport v=new Viewport();v.zoom=zoom;v.panX=panX;v.panY=panY;v.rotation=rotation;v.flip=flip;return v;}
}

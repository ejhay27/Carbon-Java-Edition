package com.carbon.studio.canvas;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Polygon-based Carbon selection. Ellipses/circles are sampled into points for format compatibility. */
public class Selection {
    private final List<Double> points = new ArrayList<>();
    private boolean inverted;

    public Selection() {}
    public Selection(List<Double> xyPairs, boolean inverted) {
        if (xyPairs.size() % 2 != 0) throw new IllegalArgumentException("Selection coordinates must be x/y pairs.");
        points.addAll(xyPairs); this.inverted = inverted;
    }
    public static Selection rectangle(double x0, double y0, double x1, double y1) {
        return new Selection(List.of(x0,y0,x1,y0,x1,y1,x0,y1), false);
    }
    public static Selection ellipse(double x0, double y0, double x1, double y1) {
        double cx=(x0+x1)/2, cy=(y0+y1)/2, rx=Math.abs(x1-x0)/2, ry=Math.abs(y1-y0)/2;
        List<Double> p=new ArrayList<>();
        for(int i=0;i<96;i++){double a=i*Math.PI*2/96;p.add(cx+Math.cos(a)*rx);p.add(cy+Math.sin(a)*ry);}
        return new Selection(p,false);
    }
    public List<Double> getPoints() { return List.copyOf(points); }
    public void addPoint(double x,double y){points.add(x);points.add(y);}
    public boolean isInverted() { return inverted; }
    public void setInverted(boolean inverted) { this.inverted = inverted; }
    public Selection copy(){return new Selection(points,inverted);}
    public Shape toShape() {
        Path2D.Double path=new Path2D.Double();
        if(points.size()<6)return path;
        path.moveTo(points.get(0),points.get(1));
        for(int i=2;i+1<points.size();i+=2)path.lineTo(points.get(i),points.get(i+1));
        path.closePath();return path;
    }
    public Area toArea(int width,int height){
        Area area=new Area(toShape());
        if(inverted){Area all=new Area(new Rectangle2D.Double(0,0,width,height));all.subtract(area);return all;}
        return area;
    }
}

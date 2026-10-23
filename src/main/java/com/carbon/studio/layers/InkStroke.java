package com.carbon.studio.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Editable Crisp Ink path: x,y,pressure triples plus appearance. */
public class InkStroke {
    private String id = UUID.randomUUID().toString();
    private final List<Double> points = new ArrayList<>();
    private String color = "#000000";
    private double size = 4;
    private double opacity = 1;
    private double taperStart;
    private double taperEnd;

    public InkStroke() {}
    public InkStroke(String color, double size, double opacity, double taperStart, double taperEnd) {
        this.color = color; this.size = size; this.opacity = opacity;
        this.taperStart = taperStart; this.taperEnd = taperEnd;
    }
    public InkStroke(String id,List<Double> points,String color,double size,double opacity,double taperStart,double taperEnd){
        this(color,size,opacity,taperStart,taperEnd);setId(id);
        if(points==null||points.size()%3!=0)throw new IllegalArgumentException("Crisp Ink points must be x,y,pressure triples.");
        if(points.size()/3>2_000_000)throw new IllegalArgumentException("Crisp Ink stroke exceeds the point limit.");
        for(int i=0;i<points.size();i+=3){double x=points.get(i),y=points.get(i+1),p=points.get(i+2);if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(p))throw new IllegalArgumentException("Crisp Ink points must be finite numbers.");addPoint(x,y,p);}
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<Double> getPoints() { return points; }
    public void addPoint(double x, double y, double pressure) {
        points.add(x); points.add(y); points.add(Math.max(0, Math.min(1, pressure)));
    }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getSize() { return size; }
    public void setSize(double size) { this.size = Math.max(0.1, size); }
    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = Math.max(0, Math.min(1, opacity)); }
    public double getTaperStart() { return taperStart; }
    public void setTaperStart(double value) { taperStart = Math.max(0, value); }
    public double getTaperEnd() { return taperEnd; }
    public void setTaperEnd(double value) { taperEnd = Math.max(0, value); }

    public InkStroke copy() {
        InkStroke s = new InkStroke(color, size, opacity, taperStart, taperEnd);
        s.id = id; s.points.addAll(points); return s;
    }
}

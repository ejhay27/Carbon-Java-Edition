package com.carbon.studio.layers;

import com.carbon.studio.canvas.Images;
import com.carbon.studio.tools.ColorUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Resolution-independent Catmull-Rom Crisp Ink outlines with pressure and end tapers. */
public final class InkRenderer {
    private InkRenderer(){}
    public static BufferedImage render(Layer layer,int width,int height,double scale){
        BufferedImage out=Images.create(Math.max(1,(int)Math.round(width*scale)),Math.max(1,(int)Math.round(height*scale)));
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(scale,scale);
        for(InkStroke stroke:layer.getStrokes())drawStroke(g,stroke);g.dispose();return out;
    }
    public static void drawStroke(Graphics2D g,InkStroke stroke){
        if(stroke.getPoints().size()<3)return;Color color=ColorUtil.parse(stroke.getColor());g.setPaint(new Color(color.getRed(),color.getGreen(),color.getBlue(),(int)Math.round(color.getAlpha()*stroke.getOpacity())));g.fill(outline(stroke));
    }
    public static java.awt.Shape outline(InkStroke stroke){
        List<Double> p=stroke.getPoints();if(p.size()<3)return new Path2D.Double();
        if(p.size()==3){double r=stroke.getSize()*Math.max(0.1,p.get(2))/2;return new Ellipse2D.Double(p.get(0)-r,p.get(1)-r,r*2,r*2);}
        int count=p.size()/3;double[][] pts=new double[count][];for(int i=0;i<count;i++)pts[i]=new double[]{p.get(i*3),p.get(i*3+1),p.get(i*3+2)};
        int samples=Math.max(8,(count-1)*12);List<Point2D.Double>left=new ArrayList<>(samples+1),right=new ArrayList<>(samples+1);
        for(int i=0;i<=samples;i++){
            double t=(count-1)*i/(double)samples;int segment=Math.min(count-2,(int)t);double u=t-segment;
            double[]q=catmull(pts,segment,u),before=catmull(pts,segment,Math.max(0,u-0.005)),after=catmull(pts,segment,Math.min(1,u+0.005));
            double dx=after[0]-before[0],dy=after[1]-before[1],len=Math.max(1e-8,Math.hypot(dx,dy)),progress=i/(double)samples;
            double taper=taper(stroke,progress,count),width=stroke.getSize()*Math.max(0.03,q[2])*Math.max(0.02,taper)/2,nx=-dy/len*width,ny=dx/len*width;
            left.add(new Point2D.Double(q[0]+nx,q[1]+ny));right.add(new Point2D.Double(q[0]-nx,q[1]-ny));
        }
        Path2D.Double outline=new Path2D.Double();outline.moveTo(left.get(0).x,left.get(0).y);for(int i=1;i<left.size();i++)outline.lineTo(left.get(i).x,left.get(i).y);
        for(int i=right.size()-1;i>=0;i--)outline.lineTo(right.get(i).x,right.get(i).y);outline.closePath();return outline;
    }
    private static double taper(InkStroke s,double progress,int count){
        double value=s.getTaperStart()>0?Math.min(1,progress*count/s.getTaperStart()):1;
        if(s.getTaperEnd()>0)value=Math.min(value,(1-progress)*count/s.getTaperEnd());return value;
    }
    private static double[]catmull(double[][]p,int segment,double t){
        double[]p0=p[Math.max(0,segment-1)],p1=p[segment],p2=p[segment+1],p3=p[Math.min(p.length-1,segment+2)];double[]out=new double[3];
        for(int c=0;c<3;c++)out[c]=0.5*((2*p1[c])+(-p0[c]+p2[c])*t+(2*p0[c]-5*p1[c]+4*p2[c]-p3[c])*t*t+(-p0[c]+3*p1[c]-3*p2[c]+p3[c])*t*t*t);return out;
    }
}

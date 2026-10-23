package com.carbon.studio.tools;

import com.carbon.studio.canvas.BlendComposite;
import com.carbon.studio.layers.BlendMode;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;

/** Procedural normalized-tip renderer used by every brush mode. */
final class BrushStamper {
    private BrushStamper(){}
    static void stamp(BufferedImage target,double x,double y,double size,double angle,Color color,float alpha,
                      Brush brush,boolean eraser,Area clip,SplittableRandom random){
        if(size<=0||alpha<=0)return;
        Graphics2D g=target.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
            if(clip!=null)g.clip(clip);
            if(eraser)g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR,alpha));
            else g.setComposite(new BlendComposite(toBlend(brush.getComposite()),1));
            g.translate(x,y);g.rotate(Math.toRadians(angle));g.scale(size,size*brush.getRoundness());
            Color c=new Color(color.getRed(),color.getGreen(),color.getBlue(),255);
            Shape shape=shape(brush.getTip(),random);
            g.setPaint(paint(brush.getTip(),brush.getHardness(),c));
            g.fill(shape);
            details(g,brush,c,random);
        }finally{g.dispose();}
    }
    private static Paint paint(TipShape tip,double hardness,Color color){
        if(tip!=TipShape.ROUND&&tip!=TipShape.CLOUD&&tip!=TipShape.GRAIN)return color;
        if(hardness>=0.995)return color;
        float inner=(float)Math.max(0,Math.min(0.98,hardness));
        return new RadialGradientPaint(0,0,0.5f,new float[]{0,inner,1},
                new Color[]{color,color,new Color(color.getRed(),color.getGreen(),color.getBlue(),0)});
    }
    private static Shape shape(TipShape tip,SplittableRandom r){
        return switch(tip){
            case SQUARE,FLAT,FABRIC,BRICK,WINDOW,STAMP -> new Rectangle2D.Double(-0.5,-0.5,1,1);
            case STAR -> star(5,0.5,0.21);
            case ORNAMENT -> star(8,0.5,0.32);
            case LEAF,DROP,FLAME -> leaf(tip);
            case CLOUD -> cloud();
            case ROCK -> rock(r);
            case ROUND,GRAIN,PENCIL,CHALK,CHARCOAL,BRISTLE,SPLATTER,DOTS,SPONGE,HATCH,GRASS,HAIR,FLOWER,SNOW,WOOD,SCRATCH -> new Ellipse2D.Double(-0.5,-0.5,1,1);
        };
    }
    private static Shape star(int points,double outer,double inner){
        Path2D.Double p=new Path2D.Double();for(int i=0;i<points*2;i++){double a=-Math.PI/2+i*Math.PI/points,r=i%2==0?outer:inner;double x=Math.cos(a)*r,y=Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.closePath();return p;
    }
    private static Shape leaf(TipShape tip){
        Path2D.Double p=new Path2D.Double();
        if(tip==TipShape.DROP){p.moveTo(0,-0.55);p.curveTo(0.55,0.05,0.42,0.5,0,0.5);p.curveTo(-0.42,0.5,-0.55,0.05,0,-0.55);}
        else if(tip==TipShape.FLAME){p.moveTo(0.05,-0.55);p.curveTo(0.45,-0.1,0.5,0.35,0,0.5);p.curveTo(-0.5,0.25,-0.2,-0.15,0.05,-0.55);}
        else{p.moveTo(-0.5,0.25);p.quadTo(-0.2,-0.65,0.5,-0.3);p.quadTo(0.25,0.55,-0.5,0.25);}
        p.closePath();return p;
    }
    private static Shape cloud(){
        Area a=new Area(new Ellipse2D.Double(-0.45,-0.15,0.55,0.5));
        a.add(new Area(new Ellipse2D.Double(-0.2,-0.45,0.55,0.65)));a.add(new Area(new Ellipse2D.Double(0.05,-0.2,0.45,0.5)));return a;
    }
    private static Shape rock(SplittableRandom r){
        Path2D.Double p=new Path2D.Double();for(int i=0;i<9;i++){double a=i*Math.PI*2/9,rad=0.35+r.nextDouble()*0.15;double x=Math.cos(a)*rad,y=Math.sin(a)*rad;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.closePath();return p;
    }
    private static void details(Graphics2D g,Brush brush,Color color,SplittableRandom r){
        TipShape tip=brush.getTip();g.setPaint(color);g.setStroke(new BasicStroke(0.025f));
        switch(tip){
            case PENCIL,GRAIN,CHALK,CHARCOAL,SPONGE -> {
                int n=tip==TipShape.SPONGE?35:18;
                for(int i=0;i<n;i++){double x=(r.nextDouble()-0.5)*0.9,y=(r.nextDouble()-0.5)*0.9,s=0.015+r.nextDouble()*0.045;g.fill(new Ellipse2D.Double(x,y,s,s));}
            }
            case BRISTLE,HAIR,GRASS,SCRATCH,WOOD -> {
                int n=tip==TipShape.HAIR?14:8;for(int i=0;i<n;i++){double x=-0.42+i*0.84/Math.max(1,n-1);g.drawLine((int)Math.round(x*1000),-450,(int)Math.round((x+(r.nextDouble()-0.5)*0.08)*1000),450);}
            }
            case HATCH -> {for(int i=-4;i<=4;i++)g.drawLine(-500,i*110,500,i*110+400);}
            case FABRIC -> {for(int i=-4;i<=4;i++){g.drawLine(i*110,-500,i*110,500);g.drawLine(-500,i*110,500,i*110);}}
            case DOTS -> {for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++)g.fill(new Ellipse2D.Double(x*0.18-0.035,y*0.18-0.035,0.07,0.07));}
            case FLOWER -> {for(int i=0;i<6;i++){double a=i*Math.PI/3;g.fill(new Ellipse2D.Double(Math.cos(a)*0.22-0.12,Math.sin(a)*0.22-0.12,0.24,0.24));}}
            case SNOW -> {for(int i=0;i<6;i++){double a=i*Math.PI/3;g.drawLine(0,0,(int)(Math.cos(a)*450),(int)(Math.sin(a)*450));}}
            case BRICK -> {g.draw(new Rectangle2D.Double(-0.5,-0.5,1,0.45));g.draw(new Rectangle2D.Double(-0.5,0.05,0.45,0.45));g.draw(new Rectangle2D.Double(0.05,0.05,0.45,0.45));}
            case WINDOW -> {g.draw(new Rectangle2D.Double(-0.45,-0.45,0.9,0.9));g.drawLine(0,-450,0,450);g.drawLine(-450,0,450,0);}
            case SPLATTER -> {for(int i=0;i<9;i++){double a=r.nextDouble()*Math.PI*2,rad=0.3+r.nextDouble()*0.35,s=0.03+r.nextDouble()*0.08;g.fill(new Ellipse2D.Double(Math.cos(a)*rad,Math.sin(a)*rad,s,s));}}
            default -> { }
        }
        if(brush.getGrain()>0){
            int n=(int)(brush.getGrain()*12);for(int i=0;i<n;i++){double x=(r.nextDouble()-0.5),y=(r.nextDouble()-0.5),s=0.01+r.nextDouble()*0.025;g.fill(new Ellipse2D.Double(x,y,s,s));}
        }
        if(brush.getMode()==BrushMode.WASH&&brush.wetEdges()>0){
            g.setStroke(new BasicStroke((float)(0.015+brush.wetEdges()*0.035)));g.setPaint(BlendComposite.withAlpha(color,0.45));g.draw(new Ellipse2D.Double(-0.48,-0.48,0.96,0.96));
        }
    }
    private static BlendMode toBlend(StrokeComposite composite){
        return switch(composite){case MULTIPLY->BlendMode.MULTIPLY;case SCREEN->BlendMode.SCREEN;case LIGHTER->BlendMode.LIGHTER;case DARKEN->BlendMode.DARKEN;default->BlendMode.SOURCE_OVER;};
    }
}

package com.carbon.studio.canvas;

import com.carbon.studio.layers.Adjustment;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

/** Deterministic raster filters shared by commands and adjustment layers. */
public final class ImageFilters {
    private ImageFilters(){}
    public enum Kind { INVERT,GRAYSCALE,SEPIA,BLUR,BRIGHTNESS,CONTRAST }
    public static BufferedImage filter(BufferedImage source,Kind kind,double amount){
        BufferedImage out=Images.copy(source);
        if(kind==Kind.BLUR){float v=(float)Math.max(0.01,amount);Kernel kernel=new Kernel(3,3,new float[]{1/v,1/v,1/v,1/v,1/v,1/v,1/v,1/v,1/v});new java.awt.image.ConvolveOp(kernel,java.awt.image.ConvolveOp.EDGE_NO_OP,null).filter(out,out);return out;}
        for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++){
            int argb=out.getRGB(x,y),a=argb>>>24,r=argb>>16&255,g=argb>>8&255,b=argb&255;
            switch(kind){
                case INVERT -> {r=255-r;g=255-g;b=255-b;}
                case GRAYSCALE -> {int q=(int)(r*0.2126+g*0.7152+b*0.0722);r=g=b=q;}
                case SEPIA -> {int nr=clamp(r*.393+g*.769+b*.189),ng=clamp(r*.349+g*.686+b*.168),nb=clamp(r*.272+g*.534+b*.131);r=nr;g=ng;b=nb;}
                case BRIGHTNESS -> {r=clamp(r*amount);g=clamp(g*amount);b=clamp(b*amount);}
                case CONTRAST -> {r=clamp((r-128)*amount+128);g=clamp((g-128)*amount+128);b=clamp((b-128)*amount+128);}
                default -> { }
            }
            out.setRGB(x,y,a<<24|r<<16|g<<8|b);
        }return out;
    }
    public static BufferedImage adjust(BufferedImage source,Adjustment a){
        if(a==null)return source;BufferedImage out=Images.copy(source);
        for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++){
            int argb=out.getRGB(x,y),alpha=argb>>>24,r=argb>>16&255,g=argb>>8&255,b=argb&255;
            r=clamp((r-128)*a.contrast+128);g=clamp((g-128)*a.contrast+128);b=clamp((b-128)*a.contrast+128);
            r=clamp(r*a.bright);g=clamp(g*a.bright);b=clamp(b*a.bright);
            if(a.gray>0){int q=(int)(r*.2126+g*.7152+b*.0722);r=mix(r,q,a.gray);g=mix(g,q,a.gray);b=mix(b,q,a.gray);}
            if(a.invert>0){r=mix(r,255-r,a.invert);g=mix(g,255-g,a.invert);b=mix(b,255-b,a.invert);}
            if(a.sepia>0){int nr=clamp(r*.393+g*.769+b*.189),ng=clamp(r*.349+g*.686+b*.168),nb=clamp(r*.272+g*.534+b*.131);r=mix(r,nr,a.sepia);g=mix(g,ng,a.sepia);b=mix(b,nb,a.sepia);}
            if(a.hue!=0||a.sat!=1){float[]hsb=Color.RGBtoHSB(r,g,b,null);hsb[0]=(float)(hsb[0]+a.hue-Math.floor(hsb[0]+a.hue));hsb[1]=(float)Math.max(0,Math.min(1,hsb[1]*a.sat));int rgb=new Color(Color.HSBtoRGB(hsb[0],hsb[1],hsb[2])).getRGB();r=rgb>>16&255;g=rgb>>8&255;b=rgb&255;}
            out.setRGB(x,y,alpha<<24|r<<16|g<<8|b);
        }
        if(a.blur>0)out=filter(out,Kind.BLUR,Math.max(1,a.blur));return out;
    }
    private static int mix(int a,int b,double t){return clamp(a*(1-t)+b*t);}
    private static int clamp(double v){return(int)Math.round(Math.max(0,Math.min(255,v)));}
}

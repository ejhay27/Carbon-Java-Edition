package com.carbon.studio.canvas;

import com.carbon.studio.layers.BlendMode;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/** Straight-alpha Carbon blend compositing for Java2D raster layers and stroke stamps. */
public final class BlendComposite implements Composite {
    private final BlendMode mode;
    private final float alpha;
    public BlendComposite(BlendMode mode,float alpha){this.mode=mode;this.alpha=Math.max(0,Math.min(1,alpha));}
    @Override public CompositeContext createContext(ColorModel srcColorModel,ColorModel dstColorModel,RenderingHints hints){
        return new CompositeContext(){
            @Override public void compose(Raster src,Raster dstIn,WritableRaster dstOut){
            int minX=Math.max(src.getMinX(),Math.max(dstIn.getMinX(),dstOut.getMinX()));
            int minY=Math.max(src.getMinY(),Math.max(dstIn.getMinY(),dstOut.getMinY()));
            int maxX=Math.min(src.getMinX()+src.getWidth(),Math.min(dstIn.getMinX()+dstIn.getWidth(),dstOut.getMinX()+dstOut.getWidth()));
            int maxY=Math.min(src.getMinY()+src.getHeight(),Math.min(dstIn.getMinY()+dstIn.getHeight(),dstOut.getMinY()+dstOut.getHeight()));
            int[] s=null,d=null;
            for(int y=minY;y<maxY;y++)for(int x=minX;x<maxX;x++){
                s=src.getPixel(x,y,s);d=dstIn.getPixel(x,y,d);
                double sa=s[3]/255.0*alpha,da=d[3]/255.0;
                if(sa<=0){dstOut.setPixel(x,y,d);continue;}
                if(mode==BlendMode.LIGHTER){
                    double oa=Math.min(1,sa+da);int[] o={clamp(d[0]+s[0]*sa),clamp(d[1]+s[1]*sa),clamp(d[2]+s[2]*sa),clamp(oa*255)};
                    // Fully transparent destination pixels receive the source rather than additive black.
                    if(da==0){o[0]=clamp(s[0]*sa);o[1]=clamp(s[1]*sa);o[2]=clamp(s[2]*sa);}
                    dstOut.setPixel(x,y,o);continue;
                }
                double oa=sa+da*(1-sa);int[] out=new int[4];
                for(int c=0;c<3;c++){
                    double cs=s[c]/255.0,cd=d[c]/255.0,b=blend(c,s,d);
                    double co=(1-da)*cs*sa+da*b*sa+(1-sa)*cd*da;
                    out[c]=clamp((oa==0?0:co/oa)*255);
                }
                out[3]=clamp(oa*255);dstOut.setPixel(x,y,out);
            }
            }
            @Override public void dispose(){}
        };
    }
    private double blend(int channel,int[] s,int[]d){
        double a=s[channel]/255.0,b=d[channel]/255.0;
        return switch(mode){
            case SOURCE_OVER -> a;
            case MULTIPLY -> a*b;
            case SCREEN -> 1-(1-a)*(1-b);
            case OVERLAY -> b<=0.5?2*a*b:1-2*(1-a)*(1-b);
            case DARKEN -> Math.min(a,b);
            case LIGHTEN -> Math.max(a,b);
            case COLOR_DODGE -> a>=1?1:Math.min(1,b/(1-a));
            case COLOR_BURN -> a<=0?0:1-Math.min(1,(1-b)/a);
            case HARD_LIGHT -> a<=0.5?2*a*b:1-2*(1-a)*(1-b);
            case SOFT_LIGHT -> (1-2*a)*b*b+2*a*b;
            case DIFFERENCE -> Math.abs(a-b);
            case EXCLUSION -> a+b-2*a*b;
            case LINEAR_BURN -> Math.max(0,a+b-1);
            case HUE,SATURATION,COLOR,LUMINOSITY -> nonSeparable(channel,s,d);
            case LIGHTER -> a;
        };
    }
    private double nonSeparable(int channel,int[]s,int[]d){
        // HSL-based equivalents for the four non-separable Carbon modes.
        double[] sl=rgbToHsl(s),dl=rgbToHsl(d);
        return switch(mode){
            case HUE -> hslToRgb(sl[0],dl[1],dl[2])[channel];
            case SATURATION -> hslToRgb(dl[0],sl[1],dl[2])[channel];
            case COLOR -> hslToRgb(sl[0],sl[1],dl[2])[channel];
            case LUMINOSITY -> hslToRgb(dl[0],dl[1],sl[2])[channel];
            default -> s[channel]/255.0;
        };
    }
    private static double[] rgbToHsl(int[]rgb){
        double r=rgb[0]/255.0,g=rgb[1]/255.0,b=rgb[2]/255.0,max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
        double h=0,l=(max+min)/2,d=max-min,ss=d==0?0:d/(1-Math.abs(2*l-1));
        if(d!=0){if(max==r)h=((g-b)/d)%6;else if(max==g)h=(b-r)/d+2;else h=(r-g)/d+4;h/=6;if(h<0)h++;}
        return new double[]{h,ss,l};
    }
    private static double[] hslToRgb(double h,double s,double l){
        if(s==0)return new double[]{l,l,l};double q=l<0.5?l*(1+s):l+s-l*s,p=2*l-q;
        return new double[]{hue(p,q,h+1.0/3),hue(p,q,h),hue(p,q,h-1.0/3)};
    }
    private static double hue(double p,double q,double t){if(t<0)t++;if(t>1)t--;if(t<1.0/6)return p+(q-p)*6*t;if(t<0.5)return q;if(t<2.0/3)return p+(q-p)*(2.0/3-t)*6;return p;}
    private static int clamp(double v){return(int)Math.round(Math.max(0,Math.min(255,v)));}
    @Override public int hashCode(){return 31*mode.hashCode()+Float.floatToIntBits(alpha);}
    @Override public boolean equals(Object o){return o instanceof BlendComposite b&&mode==b.mode&&alpha==b.alpha;}
    public static Color withAlpha(Color color,double alpha){return new Color(color.getRed(),color.getGreen(),color.getBlue(),clamp(color.getAlpha()*alpha));}
}

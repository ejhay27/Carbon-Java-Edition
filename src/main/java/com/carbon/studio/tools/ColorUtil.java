package com.carbon.studio.tools;

import java.awt.Color;

public final class ColorUtil {
    private ColorUtil(){}
    public static Color parse(String value){
        if(value==null)return new Color(0,0,0,0);String s=value.trim();
        if(s.startsWith("#"))s=s.substring(1);
        if(s.length()==6)return new Color(Integer.parseInt(s,16));
        if(s.length()==8){long v=Long.parseLong(s,16);return new Color((int)(v>>16&255),(int)(v>>8&255),(int)(v&255),(int)(v>>24&255));}
        throw new IllegalArgumentException("Expected a six- or eight-digit HEX color: "+value);
    }
    public static String hex(Color color){return String.format("#%02x%02x%02x",color.getRed(),color.getGreen(),color.getBlue());}
    public static String hex8(Color color){return String.format("#%02x%02x%02x%02x",color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha());}
    public static Color jitter(Color color,double hue,double sat,double value,SplittableRandomAccessor random){
        float[] hsb=Color.RGBtoHSB(color.getRed(),color.getGreen(),color.getBlue(),null);
        hsb[0]=(float)(hsb[0]+(random.nextDouble()*2-1)*hue);hsb[0]=(float)(hsb[0]-Math.floor(hsb[0]));
        hsb[1]=(float)Brush.clamp01(hsb[1]+(random.nextDouble()*2-1)*sat);
        hsb[2]=(float)Brush.clamp01(hsb[2]+(random.nextDouble()*2-1)*value);
        Color c=new Color(Color.HSBtoRGB(hsb[0],hsb[1],hsb[2]));return new Color(c.getRed(),c.getGreen(),c.getBlue(),color.getAlpha());
    }
    /** Small interface avoids passing mutable RNG implementation into utility signatures. */
    public interface SplittableRandomAccessor { double nextDouble(); }
}

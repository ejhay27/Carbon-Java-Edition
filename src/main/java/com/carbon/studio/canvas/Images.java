package com.carbon.studio.canvas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class Images {
    private Images() {}
    public static BufferedImage create(int width,int height){
        if(width<=0||height<=0)throw new IllegalArgumentException("Image dimensions must be positive.");
        return new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    }
    public static BufferedImage copy(BufferedImage source){
        if(source==null)return null;
        BufferedImage out=create(source.getWidth(),source.getHeight());
        Graphics2D g=out.createGraphics();g.drawImage(source,0,0,null);g.dispose();return out;
    }
    public static BufferedImage copyRegion(BufferedImage source,int x,int y,int w,int h){
        BufferedImage out=create(w,h);
        int[] pixels=source.getRGB(x,y,w,h,null,0,w);out.setRGB(0,0,w,h,pixels,0,w);return out;
    }
    public static BufferedImage scale(BufferedImage source,int width,int height){
        BufferedImage out=create(width,height);Graphics2D g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source,0,0,width,height,null);g.dispose();return out;
    }
    public static int[] pixels(BufferedImage image){return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());}
    public static long bytes(BufferedImage image){return image==null?0L:(long)image.getWidth()*image.getHeight()*4;}
}

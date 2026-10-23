package com.carbon.studio.canvas;

import com.carbon.studio.layers.BlendMode;
import com.carbon.studio.layers.InkRenderer;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerType;
import com.carbon.studio.tools.ColorUtil;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Shared compositor for view, export, navigator and color picking. */
public final class RasterCompositor {
    private RasterCompositor(){}
    public static BufferedImage composite(CarbonDocument document){return compositeRegion(document,new java.awt.Rectangle(0,0,document.getWidth(),document.getHeight()));}
    /** Composites only a document-space region at 1:1, avoiding full-canvas raster copies during strokes. */
    public static BufferedImage compositeRegion(CarbonDocument document,java.awt.Rectangle requested){
        java.awt.Rectangle clip=requested.intersection(new java.awt.Rectangle(0,0,document.getWidth(),document.getHeight()));
        if(clip.isEmpty())return Images.create(1,1);BufferedImage out=Images.create(clip.width,clip.height);Graphics2D g=out.createGraphics();
        if(document.getBackground()!=null){g.setColor(ColorUtil.parse(document.getBackground()));g.fillRect(0,0,clip.width,clip.height);}BufferedImage belowAlpha=Images.create(clip.width,clip.height);
        for(Layer layer:document.getLayers()){
            if(!layer.isVisible())continue;if(layer.getType()==LayerType.ADJUST){g.dispose();out=ImageFilters.adjust(out,layer.getAdjust());g=out.createGraphics();continue;}
            BufferedImage source=renderLayerRegion(layer,document,clip);if(source==null)continue;
            if(layer.getMask()!=null&&layer.isMaskEnabled())source=applyMaskRegion(source,layer.getMask(),clip);
            if(layer.isClip())source=applyClip(source,belowAlpha);
            Graphics2D alphaG=belowAlpha.createGraphics();alphaG.setComposite(AlphaComposite.SrcOver);alphaG.drawImage(source,0,0,null);alphaG.dispose();
            if(layer.getBlend()==BlendMode.SOURCE_OVER)g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)layer.getOpacity()));else g.setComposite(new BlendComposite(layer.getBlend(),(float)layer.getOpacity()));g.drawImage(source,0,0,null);
        }
        g.dispose();return out;
    }
    private static BufferedImage renderLayerRegion(Layer layer,CarbonDocument document,java.awt.Rectangle clip){
        if(layer.getType()==LayerType.RASTER&&layer.getImage()!=null){BufferedImage out=Images.create(clip.width,clip.height);int[]pixels=layer.getImage().getRGB(clip.x,clip.y,clip.width,clip.height,null,0,clip.width);out.setRGB(0,0,clip.width,clip.height,pixels,0,clip.width);return out;}
        BufferedImage full=renderLayer(layer,document.getWidth(),document.getHeight(),1);return full==null?null:Images.copyRegion(full,clip.x,clip.y,clip.width,clip.height);
    }
    private static BufferedImage applyMaskRegion(BufferedImage source,BufferedImage mask,java.awt.Rectangle clip){
        int[]m=mask.getRGB(clip.x,clip.y,clip.width,clip.height,null,0,clip.width);for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){int c=source.getRGB(x,y),a=(c>>>24)*(m[y*clip.width+x]>>>24)/255;source.setRGB(x,y,a<<24|(c&0xffffff));}return source;
    }
    public static BufferedImage composite(CarbonDocument document,double scale){
        if(scale<=0||!Double.isFinite(scale))throw new IllegalArgumentException("Render scale must be positive.");
        int w=Math.max(1,(int)Math.round(document.getWidth()*scale)),h=Math.max(1,(int)Math.round(document.getHeight()*scale));
        BufferedImage out=Images.create(w,h);Graphics2D g=out.createGraphics();
        if(document.getBackground()!=null){g.setColor(ColorUtil.parse(document.getBackground()));g.fillRect(0,0,w,h);}
        BufferedImage belowAlpha=Images.create(w,h);
        for(Layer layer:document.getLayers()){
            if(!layer.isVisible())continue;
            if(layer.getType()==LayerType.ADJUST){g.dispose();out=ImageFilters.adjust(out,layer.getAdjust());g=out.createGraphics();continue;}
            BufferedImage source=renderLayer(layer,document.getWidth(),document.getHeight(),scale);
            if(source==null)continue;
            if(layer.getMask()!=null&&layer.isMaskEnabled())source=applyMask(source,layer.getMask());
            if(layer.isClip())source=applyClip(source,belowAlpha);
            Graphics2D alphaG=belowAlpha.createGraphics();alphaG.setComposite(AlphaComposite.SrcOver);alphaG.drawImage(source,0,0,null);alphaG.dispose();
            if(layer.getBlend()==BlendMode.SOURCE_OVER)g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)layer.getOpacity()));
            else g.setComposite(new BlendComposite(layer.getBlend(),(float)layer.getOpacity()));
            g.drawImage(source,0,0,null);
        }
        g.dispose();return out;
    }
    public static BufferedImage renderLayer(Layer layer,int docW,int docH,double scale){
        return switch(layer.getType()){
            case RASTER -> layer.getImage()==null?null:scale==1?Images.copy(layer.getImage()):Images.scale(layer.getImage(),Math.max(1,(int)Math.round(docW*scale)),Math.max(1,(int)Math.round(docH*scale)));
            case INK -> InkRenderer.render(layer,docW,docH,scale);
            default -> null;
        };
    }
    public static BufferedImage compositeTwo(Layer bottom,Layer top,int width,int height){
        BufferedImage out=Images.create(width,height);Graphics2D g=out.createGraphics();
        if(bottom.isVisible()){BufferedImage b=renderLayer(bottom,width,height,1);if(b!=null){g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)bottom.getOpacity()));g.drawImage(b,0,0,null);}}
        if(top.isVisible()){BufferedImage t=renderLayer(top,width,height,1);if(t!=null){g.setComposite(top.getBlend()==BlendMode.SOURCE_OVER?AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)top.getOpacity()):new BlendComposite(top.getBlend(),(float)top.getOpacity()));g.drawImage(t,0,0,null);}}
        g.dispose();return out;
    }
    public static int samplePixel(CarbonDocument document,int x,int y){
        if(x<0||y<0||x>=document.getWidth()||y>=document.getHeight())return 0;
        if(document.getActiveLayer()!=null&&document.getActiveLayer().getType()==LayerType.RASTER&&document.getActiveLayer().getImage()!=null)return document.getActiveLayer().getImage().getRGB(x,y);
        return composite(document).getRGB(x,y);
    }
    private static BufferedImage applyMask(BufferedImage source,BufferedImage mask){
        BufferedImage scaled=mask.getWidth()==source.getWidth()&&mask.getHeight()==source.getHeight()?mask:Images.scale(mask,source.getWidth(),source.getHeight());
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
            int c=source.getRGB(x,y),m=scaled.getRGB(x,y);int a=(c>>>24)*(m>>>24)/255;source.setRGB(x,y,(a<<24)|(c&0xffffff));
        }return source;
    }
    private static BufferedImage applyClip(BufferedImage source,BufferedImage below){
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
            int c=source.getRGB(x,y),a=(c>>>24)*(below.getRGB(x,y)>>>24)/255;source.setRGB(x,y,(a<<24)|(c&0xffffff));
        }return source;
    }
}

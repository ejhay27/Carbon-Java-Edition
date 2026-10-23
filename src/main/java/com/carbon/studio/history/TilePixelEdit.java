package com.carbon.studio.history;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.layers.Layer;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Undoable changed tiles for a raster edit. */
public final class TilePixelEdit implements Edit {
    public static final int TILE=64;
    private record Key(int x,int y){}
    private record Change(int x,int y,int[] before,int[] after){}
    private final String label,layerId;
    private final List<Change> changes;
    private final long bytes;
    private TilePixelEdit(String label,String layerId,List<Change> changes){
        this.label=label;this.layerId=layerId;this.changes=changes;
        this.bytes=changes.stream().mapToLong(c->(long)(c.before.length+c.after.length)*4).sum();
    }
    @Override public String label(){return label;}
    @Override public long estimatedBytes(){return bytes;}
    @Override public void undo(CarbonDocument document){apply(document,false);}
    @Override public void redo(CarbonDocument document){apply(document,true);}
    private void apply(CarbonDocument document,boolean after){
        Layer layer=document.findLayer(layerId);if(layer==null||layer.getImage()==null)throw new IllegalStateException("History layer is missing: "+layerId);
        for(Change c:changes){
            int[] p=after?c.after:c.before;
            int w=Math.min(TILE,layer.getImage().getWidth()-c.x),h=Math.min(TILE,layer.getImage().getHeight()-c.y);
            layer.getImage().setRGB(c.x,c.y,w,h,p,0,TILE);
        }
    }

    public static final class Builder {
        private final String label,layerId;
        private final Map<Key,int[]> before=new LinkedHashMap<>();
        public Builder(String label,String layerId){this.label=label;this.layerId=layerId;}
        /** Captures each affected tile once, before the first modification. */
        public void captureBefore(BufferedImage image,Rectangle dirty){
            Rectangle clip=dirty.intersection(new Rectangle(0,0,image.getWidth(),image.getHeight()));if(clip.isEmpty())return;
            int x0=Math.floorDiv(clip.x,TILE)*TILE,y0=Math.floorDiv(clip.y,TILE)*TILE;
            int x1=Math.floorDiv(clip.x+clip.width-1,TILE)*TILE,y1=Math.floorDiv(clip.y+clip.height-1,TILE)*TILE;
            for(int y=y0;y<=y1;y+=TILE)for(int x=x0;x<=x1;x+=TILE){
                Key key=new Key(x,y);if(before.containsKey(key))continue;
                int[] pixels=new int[TILE*TILE];
                // Temporary 64x64 read/write also covers edge tiles without special cases.
                BufferedImage tile=new BufferedImage(TILE,TILE,BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g=tile.createGraphics();g.drawImage(image,-x,-y,null);g.dispose();
                tile.getRGB(0,0,TILE,TILE,pixels,0,TILE);before.put(key,pixels);
            }
        }
        /** Restores the original alpha while retaining newly painted RGB (Carbon alpha lock). */
        public void preserveOriginalAlpha(BufferedImage image){
            for(var entry:before.entrySet()){
                Key key=entry.getKey();int w=Math.min(TILE,image.getWidth()-key.x),h=Math.min(TILE,image.getHeight()-key.y);
                int[] current=image.getRGB(key.x,key.y,w,h,null,0,w),original=entry.getValue();
                for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                    int i=y*w+x,b=original[y*TILE+x];
                    current[i]=(b&0xff000000)|(current[i]&0x00ffffff);
                }
                image.setRGB(key.x,key.y,w,h,current,0,w);
            }
        }
        public TilePixelEdit build(BufferedImage image){
            List<Change> changes=new ArrayList<>();
            for(var entry:before.entrySet()){
                Key key=entry.getKey();int[] after=new int[TILE*TILE];
                BufferedImage tile=new BufferedImage(TILE,TILE,BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g=tile.createGraphics();g.drawImage(image,-key.x,-key.y,null);g.dispose();
                tile.getRGB(0,0,TILE,TILE,after,0,TILE);
                if(!Arrays.equals(entry.getValue(),after))changes.add(new Change(key.x,key.y,entry.getValue(),after));
            }
            return changes.isEmpty()?null:new TilePixelEdit(label,layerId,changes);
        }
        public boolean isEmpty(){return before.isEmpty();}
    }
}

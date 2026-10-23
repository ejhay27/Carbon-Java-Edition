package com.carbon.studio.layers;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Images;
import com.carbon.studio.canvas.RasterCompositor;
import com.carbon.studio.canvas.ImageFilters;
import com.carbon.studio.history.DocumentSnapshot;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.history.StructuralEdit;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Undoable structural and whole-layer document operations. */
public class LayerService {
    public void add(CarbonDocument d,HistoryManager h,LayerType type){
        execute(d,h,"New "+type.name().toLowerCase()+" layer",()->{
            int index=d.getActiveLayer()==null?d.getLayers().size():indexOf(d,d.getActiveLayerId())+1;
            String name=switch(type){case INK->"Crisp Ink "+count(d,type);case COMIC->"Panels & Lettering";case GROUP->"Group "+count(d,type);case ADJUST->"Adjustment";default->"Layer "+(d.getLayers().size()+1);};
            Layer layer=switch(type){case INK->Layer.ink(name);case COMIC->Layer.comic(name);case GROUP->Layer.group(name);case ADJUST->Layer.adjust(name,new Adjustment());default->Layer.raster(name,d.getWidth(),d.getHeight());};
            d.insertLayer(layer,index);
        });
    }
    public void delete(CarbonDocument d,HistoryManager h,String id){execute(d,h,"Delete layer",()->d.removeLayer(id));}
    public void duplicate(CarbonDocument d,HistoryManager h,String id){
        execute(d,h,"Duplicate layer",()->{
            Layer source=require(d,id);
            // IDs are immutable, so construct a fresh layer and copy visible content through serialization-independent helpers.
            Layer fresh=new Layer(java.util.UUID.randomUUID().toString(),source.getName()+" copy",source.getType(),d.getWidth(),d.getHeight());
            copyInto(fresh,source);d.insertLayer(fresh,indexOf(d,id)+1);
        });
    }
    public void move(CarbonDocument d,HistoryManager h,String id,int index){execute(d,h,"Reorder layer",()->d.moveLayer(id,index));}
    public void update(CarbonDocument d,HistoryManager h,String id,String label,Consumer<Layer> update){
        execute(d,h,label,()->{Layer old=require(d,id),copy=old.snapshot();update.accept(copy);d.replaceLayer(copy);d.setActiveLayerId(id);});
    }
    public void mergeDown(CarbonDocument d,HistoryManager h,String id){
        execute(d,h,"Merge down",()->{
            int index=indexOf(d,id);if(index<=0)throw new IllegalStateException("The bottom layer cannot be merged down.");
            Layer top=require(d,id),bottom=d.getLayers().get(index-1);
            BufferedImage merged=RasterCompositor.compositeTwo(bottom,top,d.getWidth(),d.getHeight());
            Layer replacement=bottom.snapshot();replacement.setType(LayerType.RASTER);replacement.setImage(merged);replacement.setStrokes(new java.util.ArrayList<>());
            d.replaceLayer(replacement);d.removeLayer(id);d.setActiveLayerId(replacement.getId());
        });
    }
    public void clear(CarbonDocument d,HistoryManager h,String id){
        execute(d,h,"Clear layer",()->{Layer old=require(d,id),copy=old.snapshot();if(copy.getType()==LayerType.RASTER)copy.setImage(Images.create(d.getWidth(),d.getHeight()));else if(copy.getType()==LayerType.INK)copy.getStrokes().clear();else copy.getComic().clear();d.replaceLayer(copy);});
    }
    public void rasterize(CarbonDocument d,HistoryManager h,String id){
        execute(d,h,"Rasterize Crisp Ink",()->{Layer old=require(d,id);if(old.getType()!=LayerType.INK)throw new IllegalStateException("Only Crisp Ink layers can be rasterized here.");Layer copy=old.snapshot();copy.setType(LayerType.RASTER);copy.setImage(InkRenderer.render(old,d.getWidth(),d.getHeight(),1));copy.getStrokes().clear();d.replaceLayer(copy);});
    }
    public void resizeCanvas(CarbonDocument d,HistoryManager h,int width,int height,double anchorX,double anchorY){
        CarbonDocument.validateDimensions(width,height);execute(d,h,"Canvas size",()->{
            int dx=(int)Math.round((width-d.getWidth())*anchorX),dy=(int)Math.round((height-d.getHeight())*anchorY);
            for(Layer layer:d.getLayers()){
                if(layer.getImage()!=null)layer.setImage(translated(layer.getImage(),width,height,dx,dy));
                if(layer.getMask()!=null)layer.setMask(translated(layer.getMask(),width,height,dx,dy));transformVector(layer,new double[]{1,0,0,1,dx,dy});
            }d.setSelection(transformSelection(d.getSelection(),new double[]{1,0,0,1,dx,dy}));d.resizeDocument(width,height);
        });
    }
    public void scaleImage(CarbonDocument d,HistoryManager h,int width,int height){
        CarbonDocument.validateDimensions(width,height);execute(d,h,"Image size",()->{
            for(Layer layer:d.getLayers()){
                if(layer.getImage()!=null)layer.setImage(Images.scale(layer.getImage(),width,height));
                if(layer.getMask()!=null)layer.setMask(Images.scale(layer.getMask(),width,height));transformVector(layer,new double[]{(double)width/d.getWidth(),0,0,(double)height/d.getHeight(),0,0});
            }d.setSelection(transformSelection(d.getSelection(),new double[]{(double)width/d.getWidth(),0,0,(double)height/d.getHeight(),0,0}));d.resizeDocument(width,height);
        });
    }
    public void flip(CarbonDocument d,HistoryManager h,boolean horizontal){
        execute(d,h,horizontal?"Flip canvas horizontal":"Flip canvas vertical",()->{double[]m=horizontal?new double[]{-1,0,0,1,d.getWidth(),0}:new double[]{1,0,0,-1,0,d.getHeight()};for(Layer l:d.getLayers()){if(l.getImage()!=null)l.setImage(flipImage(l.getImage(),horizontal));if(l.getMask()!=null)l.setMask(flipImage(l.getMask(),horizontal));transformVector(l,m);}d.setSelection(transformSelection(d.getSelection(),m));});
    }
    public void rotate(CarbonDocument d,HistoryManager h,boolean clockwise){
        execute(d,h,clockwise?"Rotate 90° clockwise":"Rotate 90° counter-clockwise",()->{
            double[]m=clockwise?new double[]{0,1,-1,0,d.getHeight(),0}:new double[]{0,-1,1,0,0,d.getWidth()};for(Layer l:d.getLayers()){if(l.getImage()!=null)l.setImage(rotateImage(l.getImage(),clockwise));if(l.getMask()!=null)l.setMask(rotateImage(l.getMask(),clockwise));transformVector(l,m);}
            d.setSelection(transformSelection(d.getSelection(),m));d.resizeDocument(d.getHeight(),d.getWidth());
        });
    }
    public void filter(CarbonDocument d,HistoryManager h,String id,ImageFilters.Kind kind,double amount){
        execute(d,h,"Filter: "+kind.name().toLowerCase(),()->{Layer old=require(d,id);if(old.getType()!=LayerType.RASTER)throw new IllegalStateException("Filters apply to raster layers.");Layer copy=old.snapshot();copy.setImage(ImageFilters.filter(old.getImage(),kind,amount));d.replaceLayer(copy);});
    }

    private static void transformVector(Layer layer,double[]m){
        if(layer.getType()==LayerType.INK){for(InkStroke stroke:layer.getStrokes()){var points=stroke.getPoints();for(int i=0;i+2<points.size();i+=3){double x=points.get(i),y=points.get(i+1);points.set(i,m[0]*x+m[2]*y+m[4]);points.set(i+1,m[1]*x+m[3]*y+m[5]);}stroke.setSize(stroke.getSize()*Math.sqrt(Math.abs(m[0]*m[3]-m[1]*m[2])));}}
        if(layer.getType()==LayerType.COMIC){double sx=Math.hypot(m[0],m[1]),sy=Math.hypot(m[2],m[3]),border=Math.sqrt(sx*sy);boolean swap=Math.abs(m[0])<1e-6;for(com.google.gson.JsonObject o:layer.getComic()){
                String type=o.has("type")?o.get("type").getAsString():"";if(type.equals("panel")&&o.has("pts")){var pts=o.getAsJsonArray("pts");for(int i=0;i+1<pts.size();i+=2){double x=pts.get(i).getAsDouble(),y=pts.get(i+1).getAsDouble();pts.set(i,new com.google.gson.JsonPrimitive(m[0]*x+m[2]*y+m[4]));pts.set(i+1,new com.google.gson.JsonPrimitive(m[1]*x+m[3]*y+m[5]));}if(o.has("border"))o.addProperty("border",o.get("border").getAsDouble()*border);continue;}
                if(!o.has("x")||!o.has("y")||!o.has("w")||!o.has("h"))continue;double ox=o.get("x").getAsDouble(),oy=o.get("y").getAsDouble(),ow=o.get("w").getAsDouble(),oh=o.get("h").getAsDouble(),cx=ox+ow/2,cy=oy+oh/2,nx=m[0]*cx+m[2]*cy+m[4],ny=m[1]*cx+m[3]*cy+m[5],w=(swap?oh:ow)*sx,h=(swap?ow:oh)*sy;o.addProperty("x",nx-w/2);o.addProperty("y",ny-h/2);o.addProperty("w",w);o.addProperty("h",h);if(o.has("size"))o.addProperty("size",o.get("size").getAsDouble()*border);if(o.has("tail")&&o.get("tail").isJsonObject()){var tail=o.getAsJsonObject("tail");double x=tail.get("x").getAsDouble(),y=tail.get("y").getAsDouble();tail.addProperty("x",m[0]*x+m[2]*y+m[4]);tail.addProperty("y",m[1]*x+m[3]*y+m[5]);}
            }}
    }
    private static com.carbon.studio.canvas.Selection transformSelection(com.carbon.studio.canvas.Selection selection,double[]m){if(selection==null)return null;java.util.List<Double>points=new java.util.ArrayList<>();var old=selection.getPoints();for(int i=0;i+1<old.size();i+=2){double x=old.get(i),y=old.get(i+1);points.add(m[0]*x+m[2]*y+m[4]);points.add(m[1]*x+m[3]*y+m[5]);}return new com.carbon.studio.canvas.Selection(points,selection.isInverted());}
    private static void execute(CarbonDocument d,HistoryManager h,String label,Runnable action){
        DocumentSnapshot before=DocumentSnapshot.capture(d);action.run();DocumentSnapshot after=DocumentSnapshot.capture(d);h.push(new StructuralEdit(label,before,after));d.markDirty();
    }
    private static Layer require(CarbonDocument d,String id){Layer l=d.findLayer(id);if(l==null)throw new IllegalArgumentException("Unknown layer: "+id);return l;}
    private static int indexOf(CarbonDocument d,String id){for(int i=0;i<d.getLayers().size();i++)if(d.getLayers().get(i).getId().equals(id))return i;return -1;}
    private static int count(CarbonDocument d,LayerType type){return(int)d.getLayers().stream().filter(l->l.getType()==type).count()+1;}
    private static BufferedImage translated(BufferedImage source,int w,int h,int dx,int dy){BufferedImage out=Images.create(w,h);Graphics2D g=out.createGraphics();g.drawImage(source,dx,dy,null);g.dispose();return out;}
    private static BufferedImage flipImage(BufferedImage source,boolean horizontal){BufferedImage out=Images.create(source.getWidth(),source.getHeight());Graphics2D g=out.createGraphics();if(horizontal){g.translate(source.getWidth(),0);g.scale(-1,1);}else{g.translate(0,source.getHeight());g.scale(1,-1);}g.drawImage(source,0,0,null);g.dispose();return out;}
    private static BufferedImage rotateImage(BufferedImage source,boolean clockwise){BufferedImage out=Images.create(source.getHeight(),source.getWidth());Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);if(clockwise){g.translate(out.getWidth(),0);g.rotate(Math.PI/2);}else{g.translate(0,out.getHeight());g.rotate(-Math.PI/2);}g.drawImage(source,0,0,null);g.dispose();return out;}
    private static void copyInto(Layer target,Layer source){
        target.setVisible(source.isVisible());target.setLocked(source.isLocked());target.setOpacity(source.getOpacity());target.setBlend(source.getBlend());target.setAlphaLock(source.isAlphaLock());target.setClip(source.isClip());target.setReference(source.isReference());target.setParent(source.getParent());target.setMetadata(source.getMetadata());target.setLink(source.getLink());target.setAdjust(source.getAdjust()==null?null:source.getAdjust().copy());
        if(source.getImage()!=null)target.setImage(Images.copy(source.getImage()));if(source.getMask()!=null)target.setMask(Images.copy(source.getMask()));
        target.setStrokes(source.getStrokes().stream().map(InkStroke::copy).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        for(com.google.gson.JsonObject o:source.getComic())target.getComic().add(o.deepCopy());
    }
}

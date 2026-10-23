package com.carbon.studio.layers;

import com.carbon.studio.canvas.Images;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A Carbon layer. Raster images are copy-on-write when referenced by structural history. */
public class Layer {
    private final String id;
    private String name;
    private LayerType type;
    private boolean visible = true;
    private boolean locked;
    private double opacity = 1;
    private BlendMode blend = BlendMode.SOURCE_OVER;
    private boolean alphaLock;
    private boolean clip;
    private boolean reference;
    private boolean maskEnabled = true;
    private String parent;
    private boolean panelClip;
    private String panelId;
    private boolean collapsed;
    private Adjustment adjust;
    private JsonObject link;
    private BufferedImage image;
    private BufferedImage mask;
    private List<InkStroke> strokes = new ArrayList<>();
    private List<JsonObject> comic = new ArrayList<>();
    private JsonObject metadata = new JsonObject();
    private boolean imageShared;

    public Layer(String id, String name, LayerType type, int width, int height) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Layer id is required.");
        this.id = id; this.name = name == null ? "Layer" : name; this.type = type == null ? LayerType.RASTER : type;
        if (this.type == LayerType.RASTER) image = Images.create(width, height);
    }
    public static Layer raster(String name,int width,int height){return new Layer(UUID.randomUUID().toString(),name,LayerType.RASTER,width,height);}
    public static Layer ink(String name){return new Layer(UUID.randomUUID().toString(),name,LayerType.INK,1,1);}
    public static Layer comic(String name){return new Layer(UUID.randomUUID().toString(),name,LayerType.COMIC,1,1);}
    public static Layer group(String name){return new Layer(UUID.randomUUID().toString(),name,LayerType.GROUP,1,1);}
    public static Layer adjust(String name,Adjustment adjustment){Layer l=new Layer(UUID.randomUUID().toString(),name,LayerType.ADJUST,1,1);l.adjust=adjustment;return l;}

    public String getId(){return id;}
    public String getName(){return name;}
    public void setName(String name){this.name=name==null||name.isBlank()?"Layer":name;}
    public LayerType getType(){return type;}
    public void setType(LayerType type){this.type=type;}
    public boolean isVisible(){return visible;}
    public void setVisible(boolean visible){this.visible=visible;}
    public boolean isLocked(){return locked;}
    public void setLocked(boolean locked){this.locked=locked;}
    public double getOpacity(){return opacity;}
    public void setOpacity(double opacity){this.opacity=Math.max(0,Math.min(1,opacity));}
    public BlendMode getBlend(){return blend==null?BlendMode.SOURCE_OVER:blend;}
    public void setBlend(BlendMode blend){this.blend=blend==null?BlendMode.SOURCE_OVER:blend;}
    public boolean isAlphaLock(){return alphaLock;}
    public void setAlphaLock(boolean alphaLock){this.alphaLock=alphaLock;}
    public boolean isClip(){return clip;}
    public void setClip(boolean clip){this.clip=clip;}
    public boolean isReference(){return reference;}
    public void setReference(boolean reference){this.reference=reference;}
    public boolean isMaskEnabled(){return maskEnabled;}
    public void setMaskEnabled(boolean maskEnabled){this.maskEnabled=maskEnabled;}
    public String getParent(){return parent;}
    public void setParent(String parent){this.parent=parent;}
    public boolean isPanelClip(){return panelClip;}
    public void setPanelClip(boolean panelClip){this.panelClip=panelClip;}
    public String getPanelId(){return panelId;}
    public void setPanelId(String panelId){this.panelId=panelId;}
    public boolean isCollapsed(){return collapsed;}
    public void setCollapsed(boolean collapsed){this.collapsed=collapsed;}
    public Adjustment getAdjust(){return adjust;}
    public void setAdjust(Adjustment adjust){this.adjust=adjust;}
    public JsonObject getLink(){return link==null?null:link.deepCopy();}
    public void setLink(JsonObject link){this.link=link==null?null:link.deepCopy();}
    public BufferedImage getImage(){return image;}
    public void setImage(BufferedImage image){this.image=image;this.imageShared=false;}
    public BufferedImage getMask(){return mask;}
    public void setMask(BufferedImage mask){this.mask=mask;this.imageShared=false;}
    public List<InkStroke> getStrokes(){return strokes;}
    public void setStrokes(List<InkStroke> strokes){this.strokes=strokes==null?new ArrayList<>():strokes;}
    public List<JsonObject> getComic(){return comic;}
    public JsonObject getMetadata(){return metadata==null?new JsonObject():metadata.deepCopy();}
    public void setMetadata(JsonObject metadata){this.metadata=metadata==null?new JsonObject():metadata.deepCopy();}
    public boolean isImageShared(){return imageShared;}

    /** Returns a metadata snapshot that shares pixels and marks the source for copy-on-write. */
    public Layer snapshot(){
        Layer copy=copy(false);
        this.imageShared=true;
        copy.imageShared=true;
        return copy;
    }
    public Layer deepCopy(){return copy(true);}
    private Layer copy(boolean deepPixels){
        Layer l=new Layer(id,name,type,1,1);
        l.visible=visible;l.locked=locked;l.opacity=opacity;l.blend=blend;l.alphaLock=alphaLock;
        l.clip=clip;l.reference=reference;l.maskEnabled=maskEnabled;l.parent=parent;l.panelClip=panelClip;
        l.panelId=panelId;l.collapsed=collapsed;l.adjust=adjust==null?null:adjust.copy();
        l.link=link==null?null:link.deepCopy();l.metadata=metadata==null?new JsonObject():metadata.deepCopy();
        l.image=deepPixels?Images.copy(image):image;l.mask=deepPixels?Images.copy(mask):mask;
        for(InkStroke s:strokes)l.strokes.add(s.copy());
        for(JsonObject o:comic)l.comic.add(o.deepCopy());
        l.imageShared=!deepPixels&&(image!=null||mask!=null);
        return l;
    }
}

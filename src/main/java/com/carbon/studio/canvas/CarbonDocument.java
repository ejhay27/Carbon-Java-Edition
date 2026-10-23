package com.carbon.studio.canvas;

import com.carbon.studio.layers.Layer;
import com.carbon.studio.projects.ProjectKind;
import com.carbon.studio.storage.AssetData;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** UI-independent editable Carbon document. Layers are ordered bottom to top. */
public class CarbonDocument {
    public static final int MAX_SIDE=8192,MAX_AREA=40_000_000,MAX_LAYERS=64;
    private int width,height;
    private String name;
    private String background;
    private final String uuid;
    private final List<Layer> layers=new ArrayList<>();
    private String activeLayerId;
    private Selection selection;
    private final Viewport viewport=new Viewport();
    private final Map<String,AssetData> assets=new LinkedHashMap<>();
    private final Map<String,byte[]> preservedEntries=new LinkedHashMap<>();
    private JsonObject extensions=new JsonObject();
    private JsonObject manifestMetadata=new JsonObject();
    private JsonObject preservedManifest=new JsonObject();
    private ProjectKind kind=ProjectKind.DRAW;
    private Path filePath;
    private boolean dirty;
    private long revision;
    private String saveError;

    private CarbonDocument(int width,int height,String name,String background,String uuid){
        validateDimensions(width,height);
        this.width=width;this.height=height;this.name=name==null||name.isBlank()?"Untitled":name;
        this.background=background;this.uuid=uuid==null?"cbn-"+UUID.randomUUID():uuid;
    }
    public static CarbonDocument create(int width,int height,String name,String background){
        CarbonDocument d=new CarbonDocument(width,height,name,background,null);
        Layer layer=Layer.raster("Layer 1",width,height);d.layers.add(layer);d.activeLayerId=layer.getId();d.dirty=true;d.revision=1;return d;
    }
    /** Creates a layerless shell for the archive reader; never shown without loaded layers. */
    public static CarbonDocument empty(int width,int height,String name,String background,String uuid){
        return new CarbonDocument(width,height,name,background,uuid);
    }
    public static void validateDimensions(int width,int height){
        if(width<1||height<1)throw new IllegalArgumentException("Canvas width and height must be positive.");
        if(width>MAX_SIDE||height>MAX_SIDE)throw new IllegalArgumentException("Canvas sides cannot exceed 8192 pixels.");
        if((long)width*height>MAX_AREA)throw new IllegalArgumentException("Canvas cannot exceed 40 megapixels.");
    }
    public int getWidth(){return width;}
    public int getHeight(){return height;}
    public String getName(){return name;}
    public void setName(String name){this.name=name==null||name.isBlank()?"Untitled":name;markDirty();}
    public String getBackground(){return background;}
    public void setBackground(String background){this.background=background;markDirty();}
    public String getUuid(){return uuid;}
    public List<Layer> getLayers(){return Collections.unmodifiableList(layers);}
    public Layer getActiveLayer(){return findLayer(activeLayerId);}
    public String getActiveLayerId(){return activeLayerId;}
    public void setActiveLayerId(String id){if(findLayer(id)==null)throw new IllegalArgumentException("Unknown layer: "+id);activeLayerId=id;}
    public Layer findLayer(String id){for(Layer l:layers)if(l.getId().equals(id))return l;return null;}
    public void insertLayer(Layer layer,int index){
        if(layers.size()>=MAX_LAYERS)throw new IllegalStateException("Layer limit reached (64 layers).");
        if(findLayer(layer.getId())!=null)throw new IllegalArgumentException("Duplicate layer id: "+layer.getId());
        layers.add(Math.max(0,Math.min(layers.size(),index)),layer);activeLayerId=layer.getId();
    }
    public Layer removeLayer(String id){
        if(layers.size()<=1)throw new IllegalStateException("A document must keep at least one layer.");
        int i=indexOf(id);if(i<0)throw new IllegalArgumentException("Unknown layer: "+id);Layer removed=layers.remove(i);
        if(layers.isEmpty())throw new IllegalStateException("A document must keep at least one layer.");
        if(id.equals(activeLayerId))activeLayerId=layers.get(Math.min(i,layers.size()-1)).getId();return removed;
    }
    public void moveLayer(String id,int newIndex){
        int old=indexOf(id);if(old<0)throw new IllegalArgumentException("Unknown layer: "+id);
        Layer l=layers.remove(old);layers.add(Math.max(0,Math.min(layers.size(),newIndex)),l);
    }
    public void replaceLayer(Layer layer){int i=indexOf(layer.getId());if(i<0)throw new IllegalArgumentException("Unknown layer: "+layer.getId());layers.set(i,layer);}
    public void restoreLayers(List<Layer> restored,String activeId){
        if(restored==null||restored.isEmpty())throw new IllegalArgumentException("Project contains no layers.");
        if(restored.size()>MAX_LAYERS)throw new IllegalArgumentException("Project exceeds the 64-layer limit.");
        layers.clear();layers.addAll(restored);activeLayerId=findLayer(activeId)!=null?activeId:layers.get(layers.size()-1).getId();
    }
    public void resizeDocument(int newWidth,int newHeight){validateDimensions(newWidth,newHeight);width=newWidth;height=newHeight;}
    private int indexOf(String id){for(int i=0;i<layers.size();i++)if(layers.get(i).getId().equals(id))return i;return -1;}
    public Selection getSelection(){return selection==null?null:selection.copy();}
    public void setSelection(Selection selection){this.selection=selection==null?null:selection.copy();}
    public Viewport getViewport(){return viewport;}
    public Map<String,AssetData> getAssets(){return Collections.unmodifiableMap(assets);}
    public void putAsset(AssetData asset){assets.put(asset.id(),asset);}
    public Map<String,byte[]> getPreservedEntries(){return preservedEntries;}
    public JsonObject getExtensions(){return extensions.deepCopy();}
    public void setExtensions(JsonObject extensions){this.extensions=extensions==null?new JsonObject():extensions.deepCopy();}
    public JsonObject getManifestMetadata(){return manifestMetadata.deepCopy();}
    public void setManifestMetadata(JsonObject metadata){manifestMetadata=metadata==null?new JsonObject():metadata.deepCopy();}
    public JsonObject getPreservedManifest(){return preservedManifest.deepCopy();}
    public void setPreservedManifest(JsonObject manifest){preservedManifest=manifest==null?new JsonObject():manifest.deepCopy();}
    public ProjectKind getKind(){return kind;}
    public void setKind(ProjectKind kind){this.kind=kind==null?ProjectKind.DRAW:kind;}
    public Path getFilePath(){return filePath;}
    public void setFilePath(Path filePath){this.filePath=filePath;}
    public boolean isDirty(){return dirty;}
    public void setDirty(boolean dirty){this.dirty=dirty;if(!dirty)saveError=null;}
    public long getRevision(){return revision;}
    public void markDirty(){dirty=true;revision++;}
    public String getSaveError(){return saveError;}
    public void setSaveError(String saveError){this.saveError=saveError;}
}

package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Images;
import com.carbon.studio.canvas.RasterCompositor;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.layers.Adjustment;
import com.carbon.studio.layers.BlendMode;
import com.carbon.studio.layers.InkStroke;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** CARBON v1 ZIP/JSON/PNG reader and writer with bounded input and unknown-payload preservation. */
public class CarbonArchive {
    public static final int FORMAT_VERSION=1;
    public static final String MIME="application/x-carbon",COMIC_MIME="application/x-carbon-comic";
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final Limits limits;
    public CarbonArchive(){this(Limits.defaults());}
    public CarbonArchive(Limits limits){this.limits=limits;}
    public CarbonDocument read(Path path)throws IOException{
        ArchiveData archive=readEntries(path);JsonObject m=manifest(archive);validateManifest(m,archive);
        String uuid=m.has("meta")&&m.get("meta").isJsonObject()&&m.getAsJsonObject("meta").has("uuid")?m.getAsJsonObject("meta").get("uuid").getAsString():"cbn-"+UUID.randomUUID();
        CarbonDocument d=CarbonDocument.empty(reqInt(m,"width"),reqInt(m,"height"),str(m,"name","Untitled"),m.has("background")&&!m.get("background").isJsonNull()?m.get("background").getAsString():null,uuid);
        d.setPreservedManifest(m);if(m.has("meta")&&m.get("meta").isJsonObject())d.setManifestMetadata(m.getAsJsonObject("meta"));
        if(m.has("extensions")&&m.get("extensions").isJsonObject())d.setExtensions(m.getAsJsonObject("extensions"));
        List<Layer>layers=new ArrayList<>();for(JsonElement e:m.getAsJsonArray("layers"))layers.add(readLayer(e.getAsJsonObject(),archive,d.getWidth(),d.getHeight()));d.restoreLayers(layers,str(m,"activeLayerId",""));
        if(m.has("selection")&&m.get("selection").isJsonObject())d.setSelection(selection(m.getAsJsonObject("selection")));
        if(m.has("assets")&&m.get("assets").isJsonArray())for(JsonElement e:m.getAsJsonArray("assets")){
            JsonObject a=e.getAsJsonObject();String id=str(a,"id",""),file=str(a,"file","");byte[]bytes=archive.entries.get(file);if(bytes==null)throw new CarbonFormatException("Project asset data is missing for \""+id+"\".");d.putAsset(new AssetData(id,str(a,"mime","image/png"),bytes));
        }
        for(var entry:archive.entries.entrySet())d.getPreservedEntries().put(entry.getKey(),entry.getValue().clone());
        d.setFilePath(path.toAbsolutePath().normalize());d.setDirty(false);return d;
    }
    /** Verifies archive structure and required payloads without decoding all artwork. */
    public JsonObject verify(Path path)throws IOException{ArchiveData a=readEntries(path);JsonObject m=manifest(a);validateManifest(m,a);return m;}
    public void write(CarbonDocument d,Path path,boolean includeReplay)throws IOException{
        Map<String,byte[]>files=new LinkedHashMap<>();
        for(var e:d.getPreservedEntries().entrySet()){
            String n=e.getKey();if(n.equals("mimetype")||n.equals("manifest.json")||n.equals("thumbnail.png")||n.startsWith("layers/")||n.startsWith("assets/"))continue;
            if(!includeReplay&&n.startsWith("replay/"))continue;files.put(n,e.getValue().clone());
        }
        files.put("mimetype",MIME.getBytes(StandardCharsets.UTF_8));JsonArray layerArray=new JsonArray();
        for(Layer l:d.getLayers()){
            JsonObject o=l.getMetadata();o.addProperty("id",l.getId());o.addProperty("name",l.getName());o.addProperty("type",typeName(l.getType()));o.addProperty("visible",l.isVisible());o.addProperty("locked",l.isLocked());o.addProperty("opacity",l.getOpacity());o.addProperty("blend",l.getBlend().getCanvasName());o.addProperty("alphaLock",l.isAlphaLock());
            optional(o,"clip",l.isClip());optional(o,"reference",l.isReference());optional(o,"panelClip",l.isPanelClip());optional(o,"collapsed",l.isCollapsed());if(l.getParent()!=null)o.addProperty("parent",l.getParent());if(l.getPanelId()!=null)o.addProperty("panelId",l.getPanelId());if(l.getLink()!=null)o.add("link",l.getLink());if(l.getAdjust()!=null)o.add("adjust",GSON.toJsonTree(l.getAdjust()));
            String part=safePart(l.getId());
            if(l.getType()==LayerType.RASTER){if(l.getImage()==null)throw new CarbonFormatException("Raster layer \""+l.getName()+"\" has no pixels.");String n="layers/"+part+".png";o.addProperty("file",n);files.put(n,png(l.getImage()));}
            else{o.remove("file");if(l.getType()==LayerType.INK){JsonArray strokes=new JsonArray();for(InkStroke s:l.getStrokes()){JsonObject so=new JsonObject();so.addProperty("id",s.getId());JsonArray pts=new JsonArray();for(double p:s.getPoints())pts.add(p);so.add("pts",pts);so.addProperty("color",s.getColor());so.addProperty("size",s.getSize());so.addProperty("opacity",s.getOpacity());so.addProperty("taperStart",s.getTaperStart());so.addProperty("taperEnd",s.getTaperEnd());strokes.add(so);}o.add("strokes",strokes);}else if(l.getType()==LayerType.COMIC){JsonArray comic=new JsonArray();for(JsonObject c:l.getComic())comic.add(c.deepCopy());o.add("comic",comic);}}
            if(l.getMask()!=null){String n="layers/"+part+".mask.png";o.addProperty("mask",n);o.addProperty("maskEnabled",l.isMaskEnabled());files.put(n,png(l.getMask()));}else{o.remove("mask");o.remove("maskEnabled");}layerArray.add(o);
        }
        JsonArray assets=new JsonArray();for(AssetData asset:d.getAssets().values()){String n="assets/"+safePart(asset.id());JsonObject a=new JsonObject();a.addProperty("id",asset.id());a.addProperty("file",n);a.addProperty("mime",asset.mime());files.put(n,asset.bytes());assets.add(a);}
        JsonObject m=d.getPreservedManifest();m.addProperty("format","carbon");m.addProperty("version",FORMAT_VERSION);m.addProperty("app","Carbon: Java Edition");m.addProperty("name",d.getName());m.addProperty("width",d.getWidth());m.addProperty("height",d.getHeight());m.addProperty("background",d.getBackground());m.addProperty("activeLayerId",d.getActiveLayerId());if(d.getSelection()==null)m.add("selection",null);else{JsonObject selection=new JsonObject();JsonArray pts=new JsonArray();for(double p:d.getSelection().getPoints())pts.add(p);selection.add("pts",pts);selection.addProperty("inverted",d.getSelection().isInverted());m.add("selection",selection);}JsonObject meta=d.getManifestMetadata();meta.addProperty("format",1);meta.addProperty("uuid",d.getUuid());meta.addProperty("modified",System.currentTimeMillis());if(!meta.has("created"))meta.addProperty("created",System.currentTimeMillis());m.add("meta",meta);m.add("layers",layerArray);m.add("assets",assets);m.add("extensions",d.getExtensions());if(!includeReplay)m.add("replay",null);
        double scale=Math.min(1,320.0/Math.max(d.getWidth(),d.getHeight()));files.put("thumbnail.png",png(RasterCompositor.composite(d,scale)));files.put("manifest.json",GSON.toJson(m).getBytes(StandardCharsets.UTF_8));
        Path parent=path.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);
        try(OutputStream raw=Files.newOutputStream(path);ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(raw))){zip.setLevel(6);zip.setMethod(ZipEntry.DEFLATED);
            writeEntry(zip,"mimetype",files.remove("mimetype"),true);String manifestName="manifest.json";byte[]manifest=files.remove(manifestName);
            for(var e:files.entrySet())writeEntry(zip,e.getKey(),e.getValue(),e.getKey().equals("thumbnail.png"));writeEntry(zip,manifestName,manifest,false);
        }
    }
    public BufferedImageWithMetadata readThumbnail(Path path)throws IOException{ArchiveData a=readEntries(path);byte[]p=a.entries.get("thumbnail.png");if(p==null)return null;return new BufferedImageWithMetadata(decode(p,"thumbnail",0,0),str(manifest(a),"name",""));}
    private ArchiveData readEntries(Path path)throws IOException{
        if(!Files.isRegularFile(path))throw new CarbonFormatException("Project file does not exist: "+path);long size=Files.size(path);if(size>limits.maxArchiveBytes)throw new CarbonFormatException("Project archive exceeds the configured size limit.");
        Map<String,byte[]>entries=new LinkedHashMap<>();long total=0;
        try(InputStream in=Files.newInputStream(path);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(in))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                if(entries.size()>=limits.maxEntries)throw new CarbonFormatException("Project contains too many archive entries.");String name=entry.getName();if(name==null||name.isBlank()||entries.containsKey(name))throw new CarbonFormatException("Project contains a duplicate or empty archive entry.");if(entry.isDirectory())continue;
                long declared=entry.getSize();if(declared>limits.maxEntryBytes)throw new CarbonFormatException("Archive entry is too large: "+name);ByteArrayOutputStream out=new ByteArrayOutputStream((int)Math.min(8192,Math.max(32,declared)));byte[]buf=new byte[16384];long count=0;int n;
                while((n=zip.read(buf))!=-1){count+=n;if(count>limits.maxEntryBytes)throw new CarbonFormatException("Archive entry exceeds its size limit: "+name);out.write(buf,0,n);}total+=count;if(total>limits.maxTotalBytes)throw new CarbonFormatException("Project expands beyond the configured uncompressed size limit.");entries.put(name,out.toByteArray());zip.closeEntry();
            }}catch(java.util.zip.ZipException e){throw new CarbonFormatException("The file is damaged or is not a CARBON project.",e);}return new ArchiveData(entries);
    }
    private JsonObject manifest(ArchiveData a)throws CarbonFormatException{byte[]raw=a.entries.get("manifest.json");if(raw==null)throw new CarbonFormatException("Not a CARBON project: manifest.json is missing.");try{return JsonParser.parseString(new String(raw,StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new CarbonFormatException("Project manifest is corrupt (invalid JSON).",e);}}
    private void validateManifest(JsonObject m,ArchiveData a)throws CarbonFormatException{
        if(!str(m,"format","").equals("carbon"))throw new CarbonFormatException("Not a CARBON project (unknown format tag).");if(!m.has("version")||!m.get("version").isJsonPrimitive())throw new CarbonFormatException("Project version is missing.");int version=m.get("version").getAsInt();if(version>FORMAT_VERSION)throw new CarbonFormatException("This project was created by a newer CARBON (format v"+version+"). Please update.");
        try{CarbonDocument.validateDimensions(reqInt(m,"width"),reqInt(m,"height"));}catch(IllegalArgumentException e){throw new CarbonFormatException("Invalid canvas size in project: "+e.getMessage(),e);}if(!m.has("layers")||!m.get("layers").isJsonArray()||m.getAsJsonArray("layers").isEmpty())throw new CarbonFormatException("Project contains no layers.");if(m.getAsJsonArray("layers").size()>CarbonDocument.MAX_LAYERS)throw new CarbonFormatException("Project exceeds the 64-layer limit.");
        for(JsonElement e:m.getAsJsonArray("layers")){JsonObject l=e.getAsJsonObject();String type=str(l,"type","raster"),name=str(l,"name","Layer");if(type.equals("raster")&&(str(l,"file","").isEmpty()||!a.entries.containsKey(str(l,"file",""))))throw new CarbonFormatException("Layer \""+name+"\" pixel data is missing.");if(type.equals("ink")&&(!l.has("strokes")||!l.get("strokes").isJsonArray()))throw new CarbonFormatException("Crisp Ink layer \""+name+"\" is corrupt.");if(l.has("mask")&&!a.entries.containsKey(str(l,"mask","")))throw new CarbonFormatException("Layer mask data for \""+name+"\" is missing.");}
        if(m.has("replay")&&m.get("replay").isJsonObject()){String file=str(m.getAsJsonObject("replay"),"file","");if(file.isEmpty()||!a.entries.containsKey(file))throw new CarbonFormatException("Replay data missing from archive.");}
    }
    private Layer readLayer(JsonObject o,ArchiveData a,int w,int h)throws IOException{
        String type=str(o,"type","raster");LayerType t=switch(type){case "ink"->LayerType.INK;case "group"->LayerType.GROUP;case "adjust"->LayerType.ADJUST;case "comic"->LayerType.COMIC;default->LayerType.RASTER;};String id=str(o,"id","");if(id.isBlank())id="layer-"+UUID.randomUUID();Layer l=new Layer(id,str(o,"name","Layer"),t,w,h);l.setMetadata(o);
        l.setVisible(!o.has("visible")||o.get("visible").getAsBoolean());l.setLocked(bool(o,"locked"));l.setOpacity(o.has("opacity")?o.get("opacity").getAsDouble():1);l.setBlend(BlendMode.from(str(o,"blend","source-over")));l.setAlphaLock(bool(o,"alphaLock"));l.setClip(bool(o,"clip"));l.setReference(bool(o,"reference"));l.setPanelClip(bool(o,"panelClip"));l.setCollapsed(bool(o,"collapsed"));l.setMaskEnabled(!o.has("maskEnabled")||o.get("maskEnabled").getAsBoolean());if(o.has("parent"))l.setParent(o.get("parent").getAsString());if(o.has("panelId"))l.setPanelId(o.get("panelId").getAsString());if(o.has("link")&&o.get("link").isJsonObject())l.setLink(o.getAsJsonObject("link"));if(o.has("adjust")&&o.get("adjust").isJsonObject())l.setAdjust(adjust(o.getAsJsonObject("adjust")));
        if(t==LayerType.RASTER)l.setImage(decode(required(a,str(o,"file","")),"layer \""+l.getName()+"\"",w,h));
        if(t==LayerType.INK){List<InkStroke>strokes=new ArrayList<>();for(JsonElement e:o.getAsJsonArray("strokes")){JsonObject s=e.getAsJsonObject();List<Double>points=new ArrayList<>();if(!s.has("pts")||!s.get("pts").isJsonArray())throw new CarbonFormatException("Crisp Ink stroke is missing points.");for(JsonElement p:s.getAsJsonArray("pts"))points.add(p.getAsDouble());strokes.add(new InkStroke(str(s,"id",UUID.randomUUID().toString()),points,str(s,"color","#000000"),num(s,"size",4),num(s,"opacity",1),num(s,"taperStart",0),num(s,"taperEnd",0)));}l.setStrokes(strokes);}
        if(t==LayerType.COMIC&&o.has("comic")&&o.get("comic").isJsonArray())for(JsonElement e:o.getAsJsonArray("comic"))if(e.isJsonObject())l.getComic().add(e.getAsJsonObject().deepCopy());
        if(o.has("mask")&&!o.get("mask").isJsonNull())l.setMask(decode(required(a,str(o,"mask","")),"mask of \""+l.getName()+"\"",w,h));return l;
    }
    private static Adjustment adjust(JsonObject o){Adjustment a=new Adjustment();a.hue=num(o,"hue",0);a.sat=num(o,"sat",1);a.bright=num(o,"bright",1);a.contrast=num(o,"contrast",1);a.invert=num(o,"invert",0);a.gray=num(o,"gray",0);a.sepia=num(o,"sepia",0);a.blur=num(o,"blur",0);return a;}
    private static Selection selection(JsonObject o)throws CarbonFormatException{List<Double>pts=new ArrayList<>();if(!o.has("pts")||!o.get("pts").isJsonArray())throw new CarbonFormatException("Project selection is corrupt.");for(JsonElement e:o.getAsJsonArray("pts"))pts.add(e.getAsDouble());return new Selection(pts,bool(o,"inverted"));}
    private static java.awt.image.BufferedImage decode(byte[]bytes,String what,int w,int h)throws CarbonFormatException{try{java.awt.image.BufferedImage in=ImageIO.read(new ByteArrayInputStream(bytes));if(in==null)throw new IOException("No image decoder");if(w>0&&h>0&&(in.getWidth()!=w||in.getHeight()!=h))return Images.scale(in,w,h);return Images.copy(in);}catch(Exception e){throw new CarbonFormatException("Corrupt image data for "+what+".",e);}}
    private static byte[]png(java.awt.image.BufferedImage image)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();if(!ImageIO.write(image,"png",out))throw new IOException("PNG encoder is unavailable");return out.toByteArray();}
    private static byte[]required(ArchiveData a,String name)throws CarbonFormatException{byte[]b=a.entries.get(name);if(b==null)throw new CarbonFormatException("Required archive entry is missing: "+name);return b;}
    private static void writeEntry(ZipOutputStream zip,String name,byte[]bytes,boolean stored)throws IOException{ZipEntry e=new ZipEntry(name);if(stored){e.setMethod(ZipEntry.STORED);e.setSize(bytes.length);e.setCompressedSize(bytes.length);CRC32 crc=new CRC32();crc.update(bytes);e.setCrc(crc.getValue());}zip.putNextEntry(e);zip.write(bytes);zip.closeEntry();}
    private static String safePart(String id){String base=id.replaceAll("[^a-zA-Z0-9._-]","_");return base.length()>120?base.substring(0,120):base;}
    private static String typeName(LayerType t){return switch(t){case INK->"ink";case GROUP->"group";case ADJUST->"adjust";case COMIC->"comic";default->"raster";};}
    private static void optional(JsonObject o,String key,boolean value){if(value)o.addProperty(key,true);else o.remove(key);}
    private static String str(JsonObject o,String key,String fallback){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():fallback;}
    private static boolean bool(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()&&o.get(key).getAsBoolean();}
    private static int reqInt(JsonObject o,String key)throws CarbonFormatException{try{return o.get(key).getAsInt();}catch(Exception e){throw new CarbonFormatException("Project manifest is missing numeric field: "+key);}}
    private static double num(JsonObject o,String key,double fallback){try{return o.has(key)?o.get(key).getAsDouble():fallback;}catch(Exception e){return fallback;}}
    private record ArchiveData(Map<String,byte[]>entries){}
    public record BufferedImageWithMetadata(java.awt.image.BufferedImage image,String name){}
    public record Limits(long maxArchiveBytes,long maxEntryBytes,long maxTotalBytes,int maxEntries){public static Limits defaults(){return new Limits(512L*1024*1024,512L*1024*1024,1024L*1024*1024,10_000);}}
}

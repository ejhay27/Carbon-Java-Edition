package com.carbon.studio.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Offline brush library extracted from the authoritative 7.9.9 source tree. */
public class BrushPresetLibrary {
    public static final int MAX_PACK_BRUSHES=2000,DEFAULT_MAX_PACK_BYTES=100*1024*1024;
    private static final String RESOURCE="/com/carbon/studio/data/brushes.json";
    private final Gson gson=new GsonBuilder().disableHtmlEscaping().create();
    private final List<Brush> builtins=new ArrayList<>();
    private final List<PackDefinition> packs=new ArrayList<>();
    public BrushPresetLibrary(){load();}
    private void load(){
        try(InputStream in=BrushPresetLibrary.class.getResourceAsStream(RESOURCE)){
            if(in==null)throw new IllegalStateException("Missing bundled brush resource: "+RESOURCE);
            JsonObject root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:root.getAsJsonArray("brushes")){Brush b=gson.fromJson(e,Brush.class);b.validate();builtins.add(b);}
            for(var p:root.getAsJsonArray("packs")){
                JsonObject o=p.getAsJsonObject();List<Brush> brushes=new ArrayList<>();
                for(var e:o.getAsJsonArray("brushes")){Brush b=gson.fromJson(e,Brush.class);b.validate();brushes.add(b);}
                packs.add(new PackDefinition(str(o,"id"),str(o,"name"),str(o,"category"),o.get("version").getAsInt(),str(o,"description"),List.copyOf(brushes)));
            }
        }catch(IOException e){throw new IllegalStateException("Could not read bundled brushes",e);}
    }
    public List<Brush> builtins(){return builtins.stream().map(Brush::copy).toList();}
    public List<PackDefinition> packs(){return packs;}
    public Brush byId(String id){for(Brush b:builtins)if(b.getId().equals(id))return b.copy();for(PackDefinition p:packs)for(Brush b:p.brushes())if(b.getId().equals(id))return b.copy();return null;}
    public Brush defaultBrush(){Brush b=byId("pencil-hb");if(b==null)throw new IllegalStateException("HB Pencil preset is missing");return b;}
    public Brush defaultEraser(){Brush b=byId("eraser-hard");if(b==null)throw new IllegalStateException("Hard Eraser preset is missing");return b;}
    public List<Brush> search(List<Brush> brushes,String query){
        String q=query==null?"":query.trim().toLowerCase();if(q.isEmpty())return brushes;String[]parts=q.split("\\s+");
        return brushes.stream().filter(b->{String hay=(b.getName()+" "+b.getCategory()+" "+String.join(" ",b.getTags())+" "+b.getMode()+" "+b.getTip()).toLowerCase();for(String p:parts)if(!hay.contains(p))return false;return true;}).toList();
    }
    public String exportPack(String name,List<Brush> brushes,String packId,String category){
        JsonObject root=new JsonObject();root.addProperty("format","carbon-brushpack");root.addProperty("id",safeId(packId));root.addProperty("name",name);root.addProperty("category",category);root.addProperty("version",1);root.addProperty("engine",1);
        JsonArray array=new JsonArray();for(Brush b:brushes)array.add(gson.toJsonTree(b));root.add("brushes",array);return gson.toJson(root);
    }
    public ImportResult importPack(String json,List<Brush> existing,int maxBytes){
        if(json.getBytes(StandardCharsets.UTF_8).length>maxBytes)throw new IllegalArgumentException("Pack exceeds the import size limit.");
        JsonObject root;try{root=JsonParser.parseString(json).getAsJsonObject();}catch(Exception e){throw new IllegalArgumentException("File is not valid Carbon brush JSON.",e);}
        String format=str(root,"format");if(!format.equals("carbon-brushpack")&&!format.equals("carbon-brushes"))throw new IllegalArgumentException("Not a CARBON brush pack.");
        if(root.has("engine")&&root.get("engine").getAsInt()!=1)throw new IllegalArgumentException("Pack requires an unsupported brush engine.");
        JsonArray input=root.getAsJsonArray("brushes");if(input==null)throw new IllegalArgumentException("Brush pack contains no brush list.");if(input.size()>MAX_PACK_BRUSHES)throw new IllegalArgumentException("Pack contains more than 2000 brushes.");
        Set<String> fingerprints=new HashSet<>();for(Brush b:existing)fingerprints.add(fingerprint(b));Set<String>ids=new HashSet<>();for(Brush b:existing)ids.add(b.getId());
        String namespace=safeId(str(root,"id"));List<Brush>added=new ArrayList<>();int duplicates=0;List<String>warnings=new ArrayList<>();
        for(int i=0;i<input.size();i++){
            try{
                Brush b=gson.fromJson(input.get(i),Brush.class);if(b==null)continue;
                String candidate=b.getId();b.setId(candidate!=null&&candidate.matches("ip-[a-zA-Z0-9._-]{1,120}")&&!ids.contains(candidate)?candidate:"ip-"+namespace+"-"+i);
                while(ids.contains(b.getId()))b.setId(b.getId()+"-copy");ids.add(b.getId());b.builtin=false;b.validate();
                if(b.getStamp()!=null&&!validStamp(b.getStamp())){warnings.add(b.getName()+": invalid or oversized stamp rejected");b.setStamp(null);if(b.getTip()==TipShape.STAMP)b.setTip(TipShape.ROUND);}
                String fp=fingerprint(b);if(!fingerprints.add(fp)){duplicates++;continue;}added.add(b);
            }catch(Exception e){warnings.add("Brush "+(i+1)+": "+e.getMessage());}
        }
        if(added.isEmpty())throw new IllegalArgumentException(duplicates>0?"Every brush in this pack is already installed.":"The pack contains no usable brushes.");
        PackDefinition pack=new PackDefinition("imp-"+namespace,str(root,"name").isBlank()?"Imported pack":str(root,"name"),str(root,"category"),1,str(root,"description"),List.copyOf(added));
        return new ImportResult(pack,duplicates,List.copyOf(warnings));
    }
    public String fingerprint(Brush brush){
        Brush b=brush.copy();b.id="";b.name="";b.category="";b.tags=List.of();b.description="";b.builtin=false;
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[]hash=digest.digest(gson.toJson(b).getBytes(StandardCharsets.UTF_8));return java.util.HexFormat.of().formatHex(hash);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    private static boolean validStamp(String value){
        try{int comma=value.indexOf(',');if(!value.startsWith("data:image/")||comma<0)return false;byte[]bytes=Base64.getDecoder().decode(value.substring(comma+1));if(bytes.length<12||bytes.length>300_000)return false;
            if(value.startsWith("data:image/png"))return(bytes[0]&255)==137&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G';
            if(value.startsWith("data:image/jpeg"))return(bytes[0]&255)==255&&(bytes[1]&255)==216;
            if(value.startsWith("data:image/webp"))return bytes[0]=='R'&&bytes[1]=='I'&&bytes[2]=='F'&&bytes[3]=='F'&&bytes[8]=='W'&&bytes[9]=='E'&&bytes[10]=='B'&&bytes[11]=='P';return false;
        }catch(Exception e){return false;}
    }
    private static String safeId(String value){String s=value==null?"":value.toLowerCase().replaceAll("[^a-z0-9._-]+","-").replaceAll("^-+|-+$","");return s.isBlank()?"pack-"+UUID.randomUUID().toString().substring(0,8):s.substring(0,Math.min(80,s.length()));}
    private static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    public record PackDefinition(String id,String name,String category,int version,String description,List<Brush>brushes){}
    public record ImportResult(PackDefinition pack,int duplicates,List<String>warnings){}
}

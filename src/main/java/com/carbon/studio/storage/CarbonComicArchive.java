package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** .carboncomic v1: an index of complete, independently valid .carbon pages. */
public class CarbonComicArchive {
    private final CarbonArchive pages;private final long maxBytes;
    public CarbonComicArchive(CarbonArchive pages){this(pages,1024L*1024*1024);}
    public CarbonComicArchive(CarbonArchive pages,long maxBytes){this.pages=pages;this.maxBytes=maxBytes;}
    public ComicProject read(Path path)throws IOException{
        if(Files.size(path)>maxBytes)throw new CarbonFormatException("Comic archive exceeds the configured size limit.");Map<String,byte[]>entries=entries(path);byte[]raw=entries.get("comic.json");if(raw==null)throw new CarbonFormatException("comic.json is missing or corrupt.");JsonObject index;
        try{index=JsonParser.parseString(new String(raw,StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new CarbonFormatException("comic.json is missing or corrupt.",e);}if(!"carbon-comic".equals(str(index,"format")))throw new CarbonFormatException("Not a CARBON comic project.");if(index.get("version").getAsInt()>1)throw new CarbonFormatException("This comic was created by a newer CARBON.");if(!index.has("pages")||!index.get("pages").isJsonArray()||index.getAsJsonArray("pages").isEmpty())throw new CarbonFormatException("The comic contains no pages.");if(index.getAsJsonArray("pages").size()>500)throw new CarbonFormatException("Comic exceeds the 500-page safety limit.");
        Path temp=Files.createTempDirectory("carbon-comic-read-");try{List<CarbonDocument>documents=new ArrayList<>();for(var e:index.getAsJsonArray("pages")){JsonObject p=e.getAsJsonObject();String name=str(p,"name"),file=str(p,"file");byte[]bytes=entries.get(file);if(bytes==null)throw new CarbonFormatException("Page “"+name+"” is missing from the archive.");Path page=temp.resolve("page.carbon");Files.write(page,bytes);CarbonDocument document=pages.read(page);document.setFilePath(null);documents.add(document);}return new ComicProject(str(index,"name"),List.copyOf(documents));}finally{deleteTree(temp);}
    }
    public void write(String name,List<CarbonDocument>documents,Path target)throws IOException{
        if(documents==null||documents.isEmpty())throw new CarbonFormatException("The comic contains no pages.");if(documents.size()>500)throw new CarbonFormatException("Comic exceeds the 500-page safety limit.");Path file=target.toAbsolutePath().normalize(),parent=file.getParent();if(parent!=null)Files.createDirectories(parent);Path tempDir=Files.createTempDirectory("carbon-comic-write-"),temp=Files.createTempFile(parent,"."+file.getFileName(),".tmp");boolean moved=false;
        try{JsonArray index=new JsonArray();try(var out=Files.newOutputStream(temp);var zip=new ZipOutputStream(new BufferedOutputStream(out))){putStored(zip,"mimetype",CarbonArchive.COMIC_MIME.getBytes(StandardCharsets.UTF_8));for(int i=0;i<documents.size();i++){
                    CarbonDocument document=documents.get(i);String entry=String.format("pages/%03d.carbon",i+1);Path page=tempDir.resolve("page.carbon");pages.write(document,page,true);byte[]bytes=Files.readAllBytes(page);zip.putNextEntry(new ZipEntry(entry));zip.write(bytes);zip.closeEntry();JsonObject p=new JsonObject();p.addProperty("file",entry);p.addProperty("name",pageName(document,i));index.add(p);
                }JsonObject root=new JsonObject();root.addProperty("format","carbon-comic");root.addProperty("version",1);root.addProperty("name",name);root.add("pages",index);byte[]json=new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8);zip.putNextEntry(new ZipEntry("comic.json"));zip.write(json);zip.closeEntry();}
            read(temp);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}moved=true;
        }finally{deleteTree(tempDir);if(!moved)Files.deleteIfExists(temp);}
    }
    private Map<String,byte[]>entries(Path path)throws IOException{Map<String,byte[]>out=new LinkedHashMap<>();long total=0;try(var in=Files.newInputStream(path);var zip=new ZipInputStream(new BufferedInputStream(in))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){if(entry.isDirectory())continue;if(out.containsKey(entry.getName()))throw new CarbonFormatException("Comic contains a duplicate archive entry.");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[]buf=new byte[16384];int n;while((n=zip.read(buf))!=-1){bytes.write(buf,0,n);total+=n;if(total>maxBytes)throw new CarbonFormatException("Comic expands beyond the configured size limit.");}out.put(entry.getName(),bytes.toByteArray());}}catch(java.util.zip.ZipException e){throw new CarbonFormatException("The comic file is damaged.",e);}return out;}
    private static String pageName(CarbonDocument d,int index){try{var ext=d.getExtensions();if(ext.has("comic")){JsonObject comic=ext.getAsJsonObject("comic");if(comic.has("pageName"))return comic.get("pageName").getAsString();}}catch(Exception ignored){}return "Page "+(index+1);}
    private static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private static void putStored(ZipOutputStream zip,String name,byte[]bytes)throws IOException{ZipEntry e=new ZipEntry(name);e.setMethod(ZipEntry.STORED);e.setSize(bytes.length);e.setCompressedSize(bytes.length);CRC32 crc=new CRC32();crc.update(bytes);e.setCrc(crc.getValue());zip.putNextEntry(e);zip.write(bytes);zip.closeEntry();}
    private static void deleteTree(Path root)throws IOException{if(!Files.exists(root))return;try(var stream=Files.walk(root)){for(Path p:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    public record ComicProject(String name,List<CarbonDocument>pages){public ComicProject{pages=List.copyOf(pages);}}
}

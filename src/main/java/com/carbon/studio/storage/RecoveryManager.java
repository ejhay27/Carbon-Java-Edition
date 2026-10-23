package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Versioned recovery files plus small metadata records; autosave never marks the source document saved. */
public class RecoveryManager {
    private final AppPaths paths;private final AtomicProjectStorage storage;private final Executor io;private final Gson gson=new GsonBuilder().setPrettyPrinting().create();
    public RecoveryManager(AppPaths paths,AtomicProjectStorage storage,Executor io){this.paths=paths;this.storage=storage;this.io=io;}
    public CompletableFuture<RecoveryRecord>autosave(CarbonDocument live){
        CarbonDocument snapshot=live.copyForIo();long revision=live.getRevision();Path file=paths.recovery().resolve(safe(live.getUuid())+".carbon");
        return CompletableFuture.supplyAsync(()->{try{Files.createDirectories(paths.recovery());storage.saveRecovery(snapshot,file);RecoveryRecord record=new RecoveryRecord(snapshot.getUuid(),snapshot.getName(),snapshot.getFilePath()==null?live.getFilePath():snapshot.getFilePath(),file,System.currentTimeMillis(),revision);writeMetadata(record);return record;}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},io);
    }
    public List<RecoveryRecord>list()throws IOException{
        List<RecoveryRecord>out=new ArrayList<>();if(!Files.isDirectory(paths.recovery()))return out;try(var stream=Files.list(paths.recovery())){for(Path p:stream.filter(p->p.getFileName().toString().endsWith(".json")).toList()){try{JsonObject o=JsonParser.parseString(Files.readString(p,StandardCharsets.UTF_8)).getAsJsonObject();Path file=Path.of(o.get("file").getAsString());if(Files.isRegularFile(file))out.add(new RecoveryRecord(o.get("uuid").getAsString(),o.get("name").getAsString(),o.has("originalPath")&&!o.get("originalPath").isJsonNull()?Path.of(o.get("originalPath").getAsString()):null,file,o.get("savedAt").getAsLong(),o.get("revision").getAsLong()));}catch(Exception ignored){/* A corrupt sidecar is skipped; the .carbon file is left for manual diagnosis. */}}}out.sort(Comparator.comparingLong(RecoveryRecord::savedAt).reversed());return out;
    }
    public CarbonDocument recover(RecoveryRecord record)throws IOException{CarbonDocument d=storage.open(record.file());d.setFilePath(record.originalPath());d.setDirty(true);return d;}
    public void discard(String uuid)throws IOException{Files.deleteIfExists(paths.recovery().resolve(safe(uuid)+".carbon"));Files.deleteIfExists(paths.recovery().resolve(safe(uuid)+".json"));Files.deleteIfExists(paths.recovery().resolve(safe(uuid)+".carbon.bak"));}
    private void writeMetadata(RecoveryRecord r)throws IOException{JsonObject o=new JsonObject();o.addProperty("format",1);o.addProperty("uuid",r.uuid());o.addProperty("name",r.name());o.addProperty("originalPath",r.originalPath()==null?null:r.originalPath().toString());o.addProperty("file",r.file().toString());o.addProperty("savedAt",r.savedAt());o.addProperty("revision",r.revision());Path target=paths.recovery().resolve(safe(r.uuid())+".json"),tmp=paths.recovery().resolve(safe(r.uuid())+".json.tmp");Files.writeString(tmp,gson.toJson(o),StandardCharsets.UTF_8);try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}}
    private static String safe(String value){return value.replaceAll("[^a-zA-Z0-9._-]","_");}
    public record RecoveryRecord(String uuid,String name,Path originalPath,Path file,long savedAt,long revision){}
}

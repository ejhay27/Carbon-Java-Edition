package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Save/Save As with same-directory temporary files, structural verification, backup and fsync. */
public class AtomicProjectStorage {
    private final CarbonArchive archive;
    public AtomicProjectStorage(CarbonArchive archive){this.archive=archive;}
    public CarbonDocument open(Path path)throws IOException{return archive.read(path);}
    public SaveReceipt save(CarbonDocument document,Path target,boolean includeReplay)throws IOException{
        Path file=target.toAbsolutePath().normalize(),parent=file.getParent();if(parent==null)throw new IOException("Save location has no parent directory.");Files.createDirectories(parent);
        String base=file.getFileName().toString();Path temp=Files.createTempFile(parent,"."+base+"-",".tmp");Path backup=parent.resolve(base+".bak");boolean committed=false;
        try{
            archive.write(document,temp,includeReplay);sync(temp);JsonObject manifest=archive.verify(temp);long bytes=Files.size(temp);if(bytes==0)throw new IOException("Refusing to replace the project with an empty file.");String checksum=sha256(temp);
            if(Files.exists(file)){Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);sync(backup);}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}committed=true;sync(file);syncDirectory(parent);
            document.setFilePath(file);document.setDirty(false);document.setSaveError(null);return new SaveReceipt(file,bytes,checksum,Files.exists(backup)?backup:null,manifest.getAsJsonArray("layers").size());
        }catch(IOException|RuntimeException e){document.setSaveError(e.getMessage());throw e;}finally{if(!committed)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
    public SaveReceipt saveRecovery(CarbonDocument document,Path target)throws IOException{
        // Recovery writes use the same verification/atomic path but never change the document's clean state or file path.
        Path original=document.getFilePath();boolean dirty=document.isDirty();SaveReceipt receipt=save(document,target,true);document.setFilePath(original);document.setDirty(dirty);return receipt;
    }
    private static void sync(Path path)throws IOException{try(FileChannel channel=FileChannel.open(path,StandardOpenOption.WRITE)){channel.force(true);}}
    private static void syncDirectory(Path directory){try(FileChannel channel=FileChannel.open(directory,StandardOpenOption.READ)){channel.force(true);}catch(IOException|UnsupportedOperationException ignored){/* Windows does not uniformly support directory fsync. */}}
    private static String sha256(Path path)throws IOException{try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(path)){byte[]buf=new byte[65536];int n;while((n=in.read(buf))!=-1)digest.update(buf,0,n);}return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public record SaveReceipt(Path file,long bytes,String sha256,Path backup,int layerCount){}
}

package com.carbon.studio.projects;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.storage.AtomicProjectStorage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Multi-document lifecycle and serialized background file I/O. */
public class ProjectManager implements AutoCloseable {
    private final List<ProjectSession>sessions=new ArrayList<>();
    private final AtomicProjectStorage storage;
    private final ExecutorService io;
    private ProjectSession active;
    public ProjectManager(AtomicProjectStorage storage){this.storage=storage;this.io=Executors.newSingleThreadExecutor(new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"carbon-file-io");t.setDaemon(true);return t;}});}
    public ProjectSession create(int width,int height,String name,String background){ProjectSession s=new ProjectSession(CarbonDocument.create(width,height,name,background));add(s);return s;}
    public ProjectSession addDocument(CarbonDocument document){ProjectSession s=new ProjectSession(document);add(s);return s;}
    private void add(ProjectSession s){sessions.add(s);active=s;}
    public List<ProjectSession>sessions(){return Collections.unmodifiableList(sessions);}
    public ProjectSession active(){return active;}
    public void activate(String sessionId){active=sessions.stream().filter(s->s.id().equals(sessionId)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown document tab"));}
    public CompletableFuture<CarbonDocument>open(Path path){return CompletableFuture.supplyAsync(()->{try{return storage.open(path);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},io);}
    public CompletableFuture<AtomicProjectStorage.SaveReceipt>save(ProjectSession session,Path target,boolean includeReplay){
        if(session==null)return CompletableFuture.failedFuture(new IllegalStateException("No document is open."));CarbonDocument live=session.document();long revision=live.getRevision();CarbonDocument snapshot=live.copyForIo();
        return CompletableFuture.supplyAsync(()->{try{return storage.save(snapshot,target,includeReplay);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},io).whenComplete((receipt,error)->{
            if(error==null){live.setFilePath(target.toAbsolutePath().normalize());if(live.getRevision()==revision)live.setDirty(false);live.setSaveError(null);}else{Throwable cause=error instanceof java.util.concurrent.CompletionException&&error.getCause()!=null?error.getCause():error;live.setSaveError(cause.getMessage());}
        });
    }
    public boolean close(ProjectSession session,CloseGuard guard){
        if(session==null)return true;if(session.document().isDirty()&&guard!=null&&!guard.confirmUnsaved(session.document()))return false;
        int index=sessions.indexOf(session);if(index<0)return true;sessions.remove(index);if(active==session){active=sessions.isEmpty()?null:sessions.get(Math.min(index,sessions.size()-1));}return true;
    }
    public boolean closeAll(CloseGuard guard){for(ProjectSession s:List.copyOf(sessions))if(!close(s,guard))return false;return true;}
    @Override public void close(){io.shutdown();try{if(!io.awaitTermination(30,TimeUnit.SECONDS))io.shutdownNow();}catch(InterruptedException e){Thread.currentThread().interrupt();io.shutdownNow();}}
    @FunctionalInterface public interface CloseGuard{boolean confirmUnsaved(CarbonDocument document);}
}

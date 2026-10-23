package com.carbon.studio.projects;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.storage.AppPaths;
import com.carbon.studio.storage.AtomicProjectStorage;
import com.carbon.studio.storage.CarbonArchive;
import com.carbon.studio.storage.RecoveryManager;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProjectLifecycleTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void asynchronousSaveDoesNotClearNewerEdits()throws Exception{
        AtomicProjectStorage storage=new AtomicProjectStorage(new CarbonArchive());try(ProjectManager manager=new ProjectManager(storage)){ProjectSession s=manager.create(32,24,"Live",null);Path file=temp.newFile("live.carbon").toPath();var save=manager.save(s,file,true);s.document().getActiveLayer().setAlphaLock(true);s.document().markDirty();save.get();assertTrue(s.document().isDirty());assertEquals(file.toAbsolutePath(),s.document().getFilePath());assertTrue(Files.size(file)>0);}
    }
    @Test public void cleanSaveAndMultipleTabsHaveIndependentHistory()throws Exception{
        try(ProjectManager manager=new ProjectManager(new AtomicProjectStorage(new CarbonArchive()))){ProjectSession a=manager.create(10,10,"A",null),b=manager.create(12,12,"B",null);assertNotEquals(a.id(),b.id());assertEquals(b,manager.active());manager.activate(a.id());assertEquals(a,manager.active());assertFalse(manager.close(a,d->false));a.document().setDirty(false);assertTrue(manager.close(a,d->false));assertEquals(b,manager.active());}
    }
    @Test public void recoveryRestoresDirtyDocumentWithoutChangingLiveSaveState()throws Exception{
        AppPaths paths=new AppPaths(temp.getRoot().toPath());paths.initialize();var io=Executors.newSingleThreadExecutor();try{RecoveryManager recovery=new RecoveryManager(paths,new AtomicProjectStorage(new CarbonArchive()),io);CarbonDocument d=CarbonDocument.create(16,12,"Recover",null);d.getActiveLayer().getImage().setRGB(1,1,Color.RED.getRGB());RecoveryManager.RecoveryRecord record=recovery.autosave(d).get();assertTrue(d.isDirty());assertNull(d.getFilePath());assertTrue(Files.isRegularFile(record.file()));assertEquals(1,recovery.list().size());CarbonDocument restored=recovery.recover(record);assertTrue(restored.isDirty());assertEquals(Color.RED.getRGB(),restored.getActiveLayer().getImage().getRGB(1,1));recovery.discard(d.getUuid());assertTrue(recovery.list().isEmpty());}finally{io.shutdown();}}
}

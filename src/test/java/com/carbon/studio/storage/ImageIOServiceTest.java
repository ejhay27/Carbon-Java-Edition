package com.carbon.studio.storage;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.history.HistoryManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ImageIOServiceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void importCreatesRealLayerAndPngExportRoundTrips()throws Exception{
        BufferedImage source=new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB);source.setRGB(1,1,Color.RED.getRGB());Path input=temp.newFile("input.png").toPath();ImageIO.write(source,"png",input.toFile());CarbonDocument d=CarbonDocument.create(8,8,"Import",null);HistoryManager h=new HistoryManager();ImageIOService service=new ImageIOService();service.importLayer(d,h,input,1,1,3,2);assertEquals(2,d.getLayers().size());assertEquals(1,d.getAssets().size());assertEquals(Color.RED.getRGB(),d.getActiveLayer().getImage().getRGB(2,2));assertTrue(h.undo(d));assertEquals(1,d.getLayers().size());assertEquals(0,d.getAssets().size());assertTrue(h.redo(d));assertEquals(1,d.getAssets().size());Path exact=temp.newFile("exact.png").toPath();service.export(d,exact,1,1);assertEquals(Color.RED.getRGB(),ImageIO.read(exact.toFile()).getRGB(2,2));Path output=temp.newFile("output.png").toPath();ImageIOService.ExportReceipt receipt=service.export(d,output,2,1);assertEquals(16,receipt.width());assertTrue(receipt.bytes()>0);BufferedImage check=ImageIO.read(output.toFile());assertEquals(16,check.getWidth());int scaled=check.getRGB(4,4);assertTrue((scaled>>>24)>0);assertEquals(255,scaled>>16&255);assertEquals(0,scaled>>8&255);assertEquals(0,scaled&255);
    }
    @Test public void jpegExportIsOpaqueAndVerifiesDimensions()throws Exception{CarbonDocument d=CarbonDocument.create(5,4,"JPEG",null);Path out=temp.newFile("out.jpg").toPath();new ImageIOService().export(d,out,1,.8);BufferedImage image=ImageIO.read(out.toFile());assertEquals(5,image.getWidth());assertEquals(4,image.getHeight());assertEquals(255,image.getRGB(0,0)>>>24);}
    @Test public void projectIndexSupportsRecentsRenameAndSoftDelete()throws Exception{try(CarbonDatabase db=CarbonDatabase.open(temp.newFile("projects.db").toPath())){ProjectIndexRepository repo=new ProjectIndexRepository(db);ProjectIndexRepository.ProjectRecord p=repo.upsert("cbn-test","Name","/tmp/test.carbon",null,123,456);assertEquals(1,repo.recent(10).size());repo.rename(p.id(),"Renamed");assertEquals("Renamed",repo.recent(10).get(0).name());repo.setDeleted(p.id(),true);assertTrue(repo.recent(10).isEmpty());}}
}

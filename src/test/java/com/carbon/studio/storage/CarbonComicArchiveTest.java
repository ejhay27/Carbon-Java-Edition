package com.carbon.studio.storage;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CarbonComicArchiveTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void multiPageComicContainsIndependentValidCarbonPages()throws Exception{
        CarbonDocument a=CarbonDocument.create(16,16,"Page A",null),b=CarbonDocument.create(20,18,"Page B","#ffffff");a.getActiveLayer().getImage().setRGB(1,1,Color.RED.getRGB());b.getActiveLayer().getImage().setRGB(2,2,Color.BLUE.getRGB());Path file=temp.newFile("test.carboncomic").toPath();CarbonComicArchive comics=new CarbonComicArchive(new CarbonArchive());comics.write("Two pages",List.of(a,b),file);CarbonComicArchive.ComicProject read=comics.read(file);assertEquals("Two pages",read.name());assertEquals(2,read.pages().size());assertEquals(Color.RED.getRGB(),read.pages().get(0).getActiveLayer().getImage().getRGB(1,1));assertEquals(20,read.pages().get(1).getWidth());assertEquals(Color.BLUE.getRGB(),read.pages().get(1).getActiveLayer().getImage().getRGB(2,2));
    }
    @Test public void emptyComicIsRejected()throws Exception{CarbonComicArchive comics=new CarbonComicArchive(new CarbonArchive());assertThrows(CarbonFormatException.class,()->comics.write("Empty",List.of(),temp.newFile("empty.carboncomic").toPath()));}
}

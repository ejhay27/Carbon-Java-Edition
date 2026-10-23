package com.carbon.studio.storage;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.layers.InkStroke;
import com.carbon.studio.layers.Layer;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CarbonArchiveTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void roundTripPreservesArtworkInkMaskAssetsSelectionAndUnknownData()throws Exception{
        CarbonDocument d=CarbonDocument.create(24,18,"Round trip","#101010");Layer raster=d.getActiveLayer();raster.getImage().setRGB(3,4,Color.RED.getRGB());raster.setMask(com.carbon.studio.canvas.Images.create(24,18));raster.getMask().setRGB(3,4,0xffffffff);JsonObject layerMetadata=raster.getMetadata();layerMetadata.addProperty("futureLayer",42);raster.setMetadata(layerMetadata);
        Layer ink=Layer.ink("Ink");InkStroke stroke=new InkStroke("#00ff00",4,1,.2,.3);stroke.addPoint(2,2,.5);stroke.addPoint(8,8,1);stroke.addPoint(15,4,.8);ink.getStrokes().add(stroke);d.insertLayer(ink,1);
        d.putAsset(new AssetData("asset-1","image/png",new byte[]{1,2,3,4}));d.setSelection(Selection.rectangle(1,2,10,12));JsonObject extensions=d.getExtensions();extensions.addProperty("futureExtension","kept");d.setExtensions(extensions);d.getPreservedEntries().put("extensions/future.bin",new byte[]{9,8,7});JsonObject documentMetadata=d.getManifestMetadata();documentMetadata.addProperty("author","Tester");d.setManifestMetadata(documentMetadata);
        Path file=temp.newFile("round.carbon").toPath();AtomicProjectStorage storage=new AtomicProjectStorage(new CarbonArchive());AtomicProjectStorage.SaveReceipt receipt=storage.save(d,file,true);assertTrue(receipt.bytes()>0);assertEquals(64,receipt.sha256().length());assertFalse(d.isDirty());
        CarbonDocument loaded=storage.open(file);assertEquals(24,loaded.getWidth());assertEquals(2,loaded.getLayers().size());assertEquals(Color.RED.getRGB(),loaded.getLayers().get(0).getImage().getRGB(3,4));assertNotNull(loaded.getLayers().get(0).getMask());assertEquals(1,loaded.getLayers().get(1).getStrokes().size());assertEquals(9,loaded.getLayers().get(1).getStrokes().get(0).getPoints().size());assertArrayEquals(new byte[]{1,2,3,4},loaded.getAssets().get("asset-1").bytes());assertEquals("kept",loaded.getExtensions().get("futureExtension").getAsString());assertArrayEquals(new byte[]{9,8,7},loaded.getPreservedEntries().get("extensions/future.bin"));assertNotNull(loaded.getSelection());assertEquals(8,loaded.getSelection().getPoints().size());
        Path second=temp.newFile("second.carbon").toPath();storage.save(loaded,second,true);CarbonDocument reopened=storage.open(second);assertEquals(42,reopened.getLayers().get(0).getMetadata().get("futureLayer").getAsInt());assertArrayEquals(new byte[]{9,8,7},reopened.getPreservedEntries().get("extensions/future.bin"));
    }
    @Test public void newerVersionAndMissingLayerPayloadAreRejected()throws Exception{
        Path newer=temp.newFile("newer.carbon").toPath();try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(newer))){put(z,"manifest.json","{\"format\":\"carbon\",\"version\":99,\"width\":10,\"height\":10,\"layers\":[]}");}CarbonFormatException error=assertThrows(CarbonFormatException.class,()->new CarbonArchive().read(newer));assertTrue(error.getMessage().contains("newer"));
        Path missing=temp.newFile("missing.carbon").toPath();try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(missing))){put(z,"manifest.json","{\"format\":\"carbon\",\"version\":1,\"width\":10,\"height\":10,\"layers\":[{\"id\":\"x\",\"name\":\"X\",\"type\":\"raster\"}]}");}assertThrows(CarbonFormatException.class,()->new CarbonArchive().read(missing));
    }
    @Test public void failedSaveDoesNotReplaceExistingFile()throws Exception{
        CarbonDocument d=CarbonDocument.create(8,8,"Original",null);AtomicProjectStorage storage=new AtomicProjectStorage(new CarbonArchive());Path file=temp.newFile("safe.carbon").toPath();storage.save(d,file,true);byte[]before=Files.readAllBytes(file);d.getActiveLayer().setImage(null);assertThrows(Exception.class,()->storage.save(d,file,true));assertArrayEquals(before,Files.readAllBytes(file));
    }
    private static void put(ZipOutputStream z,String name,String value)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(value.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
}

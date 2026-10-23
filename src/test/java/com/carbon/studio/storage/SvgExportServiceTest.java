package com.carbon.studio.storage;

import static org.junit.Assert.*;
import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.layers.InkStroke;
import com.carbon.studio.layers.Layer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SvgExportServiceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void inkIsVectorAndRasterIsExplicitPng()throws Exception{
        CarbonDocument d=CarbonDocument.create(16,16,"SVG",null);Layer ink=Layer.ink("Vector");InkStroke stroke=new InkStroke("#ff0000",3,1,0,0);stroke.addPoint(2,2,.5);stroke.addPoint(8,10,1);stroke.addPoint(12,4,.8);ink.getStrokes().add(stroke);d.insertLayer(ink,1);Path file=temp.newFile("art.svg").toPath();SvgExportService.ExportResult result=new SvgExportService().export(d,file);assertEquals(1,result.vectorInkLayers());assertEquals(1,result.rasterLayers());assertFalse(result.compositeFallback());String svg=Files.readString(file);assertTrue(svg.contains("<path d=\"M"));assertTrue(svg.contains("data:image/png;base64,"));assertTrue(svg.contains("not represented as editable vectors"));
    }
    @Test public void adjustmentUsesDeclaredCompositeFallback()throws Exception{CarbonDocument d=CarbonDocument.create(16,16,"Fallback",null);d.insertLayer(Layer.adjust("Adjust",new com.carbon.studio.layers.Adjustment()),1);Path file=temp.newFile("fallback.svg").toPath();SvgExportService.ExportResult result=new SvgExportService().export(d,file);assertTrue(result.compositeFallback());assertTrue(Files.readString(file).contains("data-carbon-fallback=\"composite\""));}
}

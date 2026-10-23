package com.carbon.studio.canvas;

import static org.junit.Assert.*;
import com.carbon.studio.layers.BlendMode;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class ViewportAndCompositeTest {
    @Test public void viewportRoundTripsWithRotationAndFlip(){
        Viewport v=new Viewport();v.setZoom(2.5);v.setPan(30,-12);v.setRotation(.7);v.setFlip(true);Point2D.Double p=new Point2D.Double(17,22);Point2D screen=v.toScreen(p.x,p.y,100,80),back=v.toDocument(screen.getX(),screen.getY(),100,80);assertEquals(p.x,back.getX(),0.0001);assertEquals(p.y,back.getY(),0.0001);
    }
    @Test public void compositorRespectsVisibilityBackgroundAndPixels(){
        CarbonDocument d=CarbonDocument.create(16,16,"Composite","#ffffff");d.getActiveLayer().getImage().setRGB(1,1,Color.RED.getRGB());BufferedImage out=RasterCompositor.composite(d);assertEquals(Color.RED.getRGB(),out.getRGB(1,1));assertEquals(Color.WHITE.getRGB(),out.getRGB(0,0));d.getActiveLayer().setVisible(false);assertEquals(Color.WHITE.getRGB(),RasterCompositor.composite(d).getRGB(1,1));
    }
    @Test public void everyBlendModeCanCompositeWithoutThrowing(){
        for(BlendMode mode:BlendMode.values()){BufferedImage out=Images.create(2,2);out.setRGB(0,0,0xff406080);BufferedImage src=Images.create(2,2);src.setRGB(0,0,0x80ff8000);java.awt.Graphics2D g=out.createGraphics();g.setComposite(new BlendComposite(mode,1));g.drawImage(src,0,0,null);g.dispose();assertTrue((out.getRGB(0,0)>>>24)>0);}
    }
    @Test public void carbonMinimumCanvasSizeIsEnforced(){assertThrows(IllegalArgumentException.class,()->CarbonDocument.create(15,16,"Too small",null));assertThrows(IllegalArgumentException.class,()->CarbonDocument.create(16,15,"Too small",null));assertNotNull(CarbonDocument.create(16,16,"Minimum",null));}
    @Test public void selectionInversionLeavesInteriorOut(){Selection s=Selection.rectangle(2,2,8,8);s.setInverted(true);assertFalse(s.toArea(10,10).contains(5,5));assertTrue(s.toArea(10,10).contains(0,0));}
}

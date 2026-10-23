package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.RasterCompositor;
import com.carbon.studio.layers.InkRenderer;
import com.carbon.studio.layers.InkStroke;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerType;
import com.carbon.studio.tools.ColorUtil;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

/** SVG with genuine Crisp Ink paths and explicit PNG raster images/fallbacks. */
public class SvgExportService {
    public ExportResult export(CarbonDocument d,Path target)throws IOException{
        for(Layer l:d.getLayers())if(l.isVisible()&&l.getType()==LayerType.COMIC&&!l.getComic().isEmpty())throw new IOException("SVG export of comic objects is not implemented. Export PNG/PDF instead; no comic content was omitted silently.");
        boolean compositeFallback=d.getLayers().stream().anyMatch(l->l.isVisible()&&(l.getType()==LayerType.ADJUST||l.isClip()||(l.getMask()!=null&&l.isMaskEnabled())));
        StringBuilder svg=new StringBuilder(16384);svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"").append(d.getWidth()).append("\" height=\"").append(d.getHeight()).append("\" viewBox=\"0 0 ").append(d.getWidth()).append(' ').append(d.getHeight()).append("\" data-carbon-format=\"1\" data-carbon-application=\"Carbon: Java Edition\">\n");
        if(d.getBackground()!=null){var c=ColorUtil.parse(d.getBackground());svg.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(ColorUtil.hex(c)).append("\" fill-opacity=\"").append(number(c.getAlpha()/255.0)).append("\"/>\n");}
        int vector=0,raster=0;
        if(compositeFallback){svg.append("<!-- Explicit full-composite raster fallback for adjustment, mask, or clipping semantics. -->\n");svg.append("<g data-carbon-fallback=\"composite\">").append(image(RasterCompositor.composite(d),"composite",0,0,d.getWidth(),d.getHeight())).append("</g>\n");raster=1;}
        else for(Layer layer:d.getLayers()){
            if(!layer.isVisible()||layer.getType()==LayerType.GROUP)continue;svg.append("<g id=\"").append(escape(layer.getId())).append("\" data-carbon-name=\"").append(escape(layer.getName())).append("\" data-carbon-blend=\"").append(layer.getBlend().getCanvasName()).append("\" opacity=\"").append(number(layer.getOpacity())).append("\"").append(blendStyle(layer)).append(">\n");
            if(layer.getType()==LayerType.RASTER&&layer.getImage()!=null){svg.append("<!-- Raster layer encoded as PNG; not represented as editable vectors. -->\n").append(image(layer.getImage(),layer.getId(),0,0,d.getWidth(),d.getHeight()));raster++;}
            else if(layer.getType()==LayerType.INK){for(InkStroke stroke:layer.getStrokes()){var color=ColorUtil.parse(stroke.getColor());svg.append("<path d=\"").append(path(InkRenderer.outline(stroke))).append("\" fill=\"").append(ColorUtil.hex(color)).append("\" fill-opacity=\"").append(number(color.getAlpha()/255.0*stroke.getOpacity())).append("\" fill-rule=\"nonzero\"/>\n");}vector++;}
            svg.append("</g>\n");
        }
        svg.append("</svg>\n");Path file=target.toAbsolutePath().normalize();if(file.getParent()!=null)Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"."+file.getFileName(),".tmp");boolean moved=false;
        try{Files.writeString(temp,svg.toString(),StandardCharsets.UTF_8);verify(temp);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}moved=true;return new ExportResult(file,Files.size(file),vector,raster,compositeFallback);}finally{if(!moved)Files.deleteIfExists(temp);}
    }
    private static String image(BufferedImage image,String id,double x,double y,double w,double h)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return "<image id=\""+escape(id)+"\" x=\""+number(x)+"\" y=\""+number(y)+"\" width=\""+number(w)+"\" height=\""+number(h)+"\" xlink:href=\"data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray())+"\"/>\n";}
    private static String path(Shape shape){StringBuilder out=new StringBuilder();PathIterator it=shape.getPathIterator(null);double[]c=new double[6];while(!it.isDone()){int type=it.currentSegment(c);switch(type){case PathIterator.SEG_MOVETO->out.append('M').append(number(c[0])).append(' ').append(number(c[1]));case PathIterator.SEG_LINETO->out.append('L').append(number(c[0])).append(' ').append(number(c[1]));case PathIterator.SEG_QUADTO->out.append('Q').append(number(c[0])).append(' ').append(number(c[1])).append(' ').append(number(c[2])).append(' ').append(number(c[3]));case PathIterator.SEG_CUBICTO->out.append('C').append(number(c[0])).append(' ').append(number(c[1])).append(' ').append(number(c[2])).append(' ').append(number(c[3])).append(' ').append(number(c[4])).append(' ').append(number(c[5]));case PathIterator.SEG_CLOSE->out.append('Z');}it.next();}return out.toString();}
    private static String blendStyle(Layer layer){return switch(layer.getBlend()){case SOURCE_OVER->"";case LIGHTER->" style=\"mix-blend-mode:plus-lighter\"";case LINEAR_BURN->"";default->" style=\"mix-blend-mode:"+layer.getBlend().getCanvasName()+"\"";};}
    private static void verify(Path file)throws IOException{try{DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setExpandEntityReferences(false);var doc=factory.newDocumentBuilder().parse(file.toFile());if(!"svg".equals(doc.getDocumentElement().getLocalName())&&!"svg".equals(doc.getDocumentElement().getNodeName()))throw new IOException("SVG verification failed.");}catch(IOException e){throw e;}catch(Exception e){throw new IOException("Generated SVG is not well-formed.",e);}}
    private static String number(double value){return String.format(Locale.ROOT,"%.4f",value).replaceAll("0+$","").replaceAll("\\.$","");}
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace("\"","&quot;").replace("'","&apos;");}
    public record ExportResult(Path file,long bytes,int vectorInkLayers,int rasterLayers,boolean compositeFallback){}
}

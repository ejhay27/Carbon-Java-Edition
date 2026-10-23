package com.carbon.studio.storage;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Images;
import com.carbon.studio.canvas.RasterCompositor;
import com.carbon.studio.history.DocumentSnapshot;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.history.StructuralEdit;
import com.carbon.studio.layers.Layer;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Real ImageIO import/export with format/quality reporting and atomic file replacement. */
public class ImageIOService {
    public static final long MAX_IMAGE_BYTES=100L*1024*1024,MAX_IMAGE_PIXELS=100_000_000L;
    public PreparedImage prepareImport(Path file)throws IOException{byte[]bytes=checkedBytes(file);return new PreparedImage(file,read(bytes,file.toString()),bytes,mime(file));}
    public Layer importLayer(CarbonDocument d,HistoryManager history,Path file,double x,double y,double width,double height)throws IOException{return importLayer(d,history,prepareImport(file),x,y,width,height);}
    public Layer importLayer(CarbonDocument d,HistoryManager history,PreparedImage prepared,double x,double y,double width,double height)throws IOException{
        Path file=prepared.file();BufferedImage source=prepared.image();byte[]sourceBytes=prepared.bytes();if(width<=0)width=source.getWidth();if(height<=0)height=source.getHeight();if(width<1||height<1||width>d.getWidth()*4||height>d.getHeight()*4)throw new IOException("Imported image dimensions are invalid.");
        DocumentSnapshot before=DocumentSnapshot.capture(d);String assetId="asset-"+UUID.randomUUID();d.putAsset(new AssetData(assetId,mime(file),sourceBytes));
        BufferedImage pixels=Images.create(d.getWidth(),d.getHeight());Graphics2D g=pixels.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,(int)Math.round(x),(int)Math.round(y),(int)Math.round(width),(int)Math.round(height),null);g.dispose();
        String name=file.getFileName().toString().replaceFirst("\\.[^.]+$","");Layer layer=Layer.raster(name,d.getWidth(),d.getHeight());layer.setImage(pixels);JsonObject meta=layer.getMetadata();meta.addProperty("assetId",assetId);layer.setMetadata(meta);d.insertLayer(layer,d.getLayers().indexOf(d.getActiveLayer())+1);history.push(new StructuralEdit("Import image",before,DocumentSnapshot.capture(d)));d.markDirty();return layer;
    }
    public ExportReceipt export(CarbonDocument d,Path target,double scale,double quality)throws IOException{
        if(scale<=0||!Double.isFinite(scale)||scale>16)throw new IllegalArgumentException("Export scale must be between 0 and 16.");int w=Math.max(1,(int)Math.round(d.getWidth()*scale)),h=Math.max(1,(int)Math.round(d.getHeight()*scale));if((long)w*h>MAX_IMAGE_PIXELS)throw new IOException("Exported image exceeds the 100-megapixel safety limit.");
        String extension=extension(target);if(!supports(extension))throw new IOException("No Java ImageIO "+extension.toUpperCase()+" writer is installed. PNG and JPEG are always available; WebP requires a supported plugin.");
        BufferedImage image=RasterCompositor.composite(d,scale);if(extension.equals("jpg")||extension.equals("jpeg")){BufferedImage opaque=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=opaque.createGraphics();g.setColor(d.getBackground()==null?Color.WHITE:com.carbon.studio.tools.ColorUtil.parse(d.getBackground()));g.fillRect(0,0,w,h);g.drawImage(image,0,0,null);g.dispose();image=opaque;}
        Path file=target.toAbsolutePath().normalize();if(file.getParent()!=null)Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"."+file.getFileName(),".tmp");boolean moved=false;
        try{write(image,extension,quality,tmp);BufferedImage verified=ImageIO.read(tmp.toFile());if(verified==null||verified.getWidth()!=w||verified.getHeight()!=h)throw new IOException("Exported image failed verification.");try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}moved=true;return new ExportReceipt(file,Files.size(file),extension,w,h);}finally{if(!moved)Files.deleteIfExists(tmp);}
    }
    public BufferedImage read(Path path)throws IOException{return read(checkedBytes(path),path.toString());}
    private static BufferedImage read(byte[]bytes,String name)throws IOException{BufferedImage image=ImageIO.read(new java.io.ByteArrayInputStream(bytes));if(image==null)throw new IOException("Unsupported or corrupt image: "+name);if((long)image.getWidth()*image.getHeight()>MAX_IMAGE_PIXELS)throw new IOException("Image exceeds the 100-megapixel safety limit.");return image;}
    private static byte[]checkedBytes(Path path)throws IOException{if(!Files.isRegularFile(path))throw new IOException("Image file does not exist: "+path);long size=Files.size(path);if(size>MAX_IMAGE_BYTES)throw new IOException("Image exceeds the 100 MiB import limit.");return Files.readAllBytes(path);}
    private static void write(BufferedImage image,String extension,double quality,Path file)throws IOException{
        Iterator<ImageWriter>writers=ImageIO.getImageWritersBySuffix(extension);if(!writers.hasNext())throw new IOException("No writer for "+extension);ImageWriter writer=writers.next();try(ImageOutputStream out=ImageIO.createImageOutputStream(file.toFile())){writer.setOutput(out);ImageWriteParam params=writer.getDefaultWriteParam();if(params.canWriteCompressed()){params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);String[]types=params.getCompressionTypes();if(types!=null&&types.length>0)params.setCompressionType(types[0]);params.setCompressionQuality((float)Math.max(0,Math.min(1,quality)));}writer.write(null,new IIOImage(image,null,null),params);out.flush();}finally{writer.dispose();}
    }
    private static boolean supports(String extension){if(extension.equals("carbon")||extension.isBlank())return false;return ImageIO.getImageWritersBySuffix(extension).hasNext();}
    private static String extension(Path path){String n=path.getFileName().toString();int dot=n.lastIndexOf('.');return dot<0?"":n.substring(dot+1).toLowerCase(Locale.ROOT);}
    private static String mime(Path path){return switch(extension(path)){case "jpg","jpeg"->"image/jpeg";case "gif"->"image/gif";case "webp"->"image/webp";default->"image/png";};}
    public record PreparedImage(Path file,BufferedImage image,byte[]bytes,String mime){public PreparedImage{bytes=bytes.clone();}@Override public byte[]bytes(){return bytes.clone();}}
    public record ExportReceipt(Path file,long bytes,String format,int width,int height){}
}

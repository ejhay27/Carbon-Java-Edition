package com.carbon.studio.tools;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Images;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.history.HistoryManager;
import com.carbon.studio.history.SelectionEdit;
import com.carbon.studio.history.TilePixelEdit;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerService;
import com.carbon.studio.layers.LayerType;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Raster editing tools that operate on actual pixels and produce undoable changes. */
public class RasterEditingService {
    public void setSelection(CarbonDocument d,HistoryManager h,Selection selection){Selection before=d.getSelection();d.setSelection(selection);h.push(new SelectionEdit(before,selection));d.markDirty();}
    public int floodFill(CarbonDocument d,HistoryManager h,String layerId,int sx,int sy,String hex,double tolerance,boolean all,int expand){
        Layer layer=raster(d,layerId);BufferedImage image=layer.getImage();int w=image.getWidth(),hgt=image.getHeight();
        if(sx<0||sy<0||sx>=w||sy>=hgt)return 0;int target=image.getRGB(sx,sy),replacement=ColorUtil.parse(hex).getRGB();double tol=Math.max(0,Math.min(1,tolerance))*441.67295593;
        boolean[] visited=new boolean[w*hgt];List<Integer> changed=new ArrayList<>();Area selection=d.getSelection()==null?null:d.getSelection().toArea(w,hgt);
        TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Fill",layerId);
        if(all){
            for(int y=0;y<hgt;y++)for(int x=0;x<w;x++){int i=y*w+x;if(!visited[i]&&distance(target,image.getRGB(x,y))<=tol&&(selection==null||selection.contains(x,y))){visited[i]=true;changed.add(i);}}
        }else{
            ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(sy*w+sx);visited[sy*w+sx]=true;
            while(!queue.isEmpty()){
                int i=queue.removeFirst(),x=i%w,y=i/w;if(distance(target,image.getRGB(x,y))>tol||(selection!=null&&!selection.contains(x,y)))continue;changed.add(i);
                if(x>0)add(queue,visited,i-1);if(x<w-1)add(queue,visited,i+1);if(y>0)add(queue,visited,i-w);if(y<hgt-1)add(queue,visited,i+w);
            }
        }
        if(expand>0)changed=expand(changed,visited,w,hgt,expand,selection);
        for(int i:changed){int x=i%w,y=i/w;tiles.captureBefore(image,new Rectangle(x,y,1,1));image.setRGB(x,y,over(image.getRGB(x,y),replacement));}
        finish(d,h,layer,tiles);return changed.size();
    }
    public void fillSelection(CarbonDocument d,HistoryManager h,String layerId,String hex){
        Layer layer=raster(d,layerId);Area area=d.getSelection()==null?new Area(new Rectangle(0,0,d.getWidth(),d.getHeight())):d.getSelection().toArea(d.getWidth(),d.getHeight());
        TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Fill selection",layerId);tiles.captureBefore(layer.getImage(),area.getBounds());Graphics2D g=layer.getImage().createGraphics();g.clip(area);g.setColor(ColorUtil.parse(hex));g.fillRect(0,0,d.getWidth(),d.getHeight());g.dispose();finish(d,h,layer,tiles);
    }
    public void clearSelection(CarbonDocument d,HistoryManager h,String layerId){
        Layer layer=raster(d,layerId);Area area=d.getSelection()==null?new Area(new Rectangle(0,0,d.getWidth(),d.getHeight())):d.getSelection().toArea(d.getWidth(),d.getHeight());
        TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Clear",layerId);tiles.captureBefore(layer.getImage(),area.getBounds());Graphics2D g=layer.getImage().createGraphics();g.clip(area);g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,d.getWidth(),d.getHeight());g.dispose();finish(d,h,layer,tiles);
    }
    public void shape(CarbonDocument d,HistoryManager h,String layerId,ToolType tool,double x0,double y0,double x1,double y1,Brush brush,String color,boolean eraser,long seed,Symmetry symmetry,boolean constrain,boolean filled){
        if(tool!=ToolType.LINE&&tool!=ToolType.RECT&&tool!=ToolType.ELLIPSE)throw new IllegalArgumentException("Not a shape tool: "+tool);
        if(constrain){if(tool==ToolType.LINE){double a=Math.atan2(y1-y0,x1-x0),snap=Math.round(a/(Math.PI/4))*Math.PI/4,len=Math.hypot(x1-x0,y1-y0);x1=x0+Math.cos(snap)*len;y1=y0+Math.sin(snap)*len;}else{double side=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));x1=x0+Math.signum(x1-x0)*side;y1=y0+Math.signum(y1-y0)*side;}}
        if(filled&&tool!=ToolType.LINE){Layer layer=raster(d,layerId);TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Filled shape",layerId);Rectangle bounds=new Rectangle((int)Math.floor(Math.min(x0,x1)),(int)Math.floor(Math.min(y0,y1)),(int)Math.ceil(Math.abs(x1-x0))+1,(int)Math.ceil(Math.abs(y1-y0))+1);tiles.captureBefore(layer.getImage(),bounds);Graphics2D g=layer.getImage().createGraphics();g.setColor(ColorUtil.parse(color));if(tool==ToolType.RECT)g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);else g.fillOval(bounds.x,bounds.y,bounds.width,bounds.height);g.dispose();finish(d,h,layer,tiles);return;}
        List<StrokeSample> samples=new ArrayList<>();
        if(tool==ToolType.LINE){int n=Math.max(2,(int)Math.ceil(Math.hypot(x1-x0,y1-y0)/3));for(int i=0;i<=n;i++)samples.add(new StrokeSample(x0+(x1-x0)*i/n,y0+(y1-y0)*i/n,1,0,0,i*4));}
        else if(tool==ToolType.RECT){double[][]p={{x0,y0},{x1,y0},{x1,y1},{x0,y1},{x0,y0}};long t=0;for(int k=0;k<4;k++){int n=Math.max(1,(int)Math.ceil(Math.hypot(p[k+1][0]-p[k][0],p[k+1][1]-p[k][1])/3));for(int i=0;i<n;i++)samples.add(new StrokeSample(p[k][0]+(p[k+1][0]-p[k][0])*i/n,p[k][1]+(p[k+1][1]-p[k][1])*i/n,1,0,0,t+=4));}}
        else{double cx=(x0+x1)/2,cy=(y0+y1)/2,rx=Math.abs(x1-x0)/2,ry=Math.abs(y1-y0)/2;int n=Math.max(32,(int)Math.ceil(Math.PI*(rx+ry)/3));for(int i=0;i<=n;i++){double a=i*Math.PI*2/n;samples.add(new StrokeSample(cx+Math.cos(a)*rx,cy+Math.sin(a)*ry,1,0,0,i*4));}}
        StrokeSession stroke=new StrokeSession(d,layerId,brush,color,eraser,seed,symmetry,h);for(StrokeSample s:samples)stroke.append(s);stroke.finish();
    }
    public void text(CarbonDocument d,HistoryManager h,String layerId,double x,double y,String text,String family,int size,String hex,boolean bold,boolean italic){
        if(text==null||text.isEmpty())return;Layer layer=raster(d,layerId);Font font=new Font(family,(bold?Font.BOLD:0)|(italic?Font.ITALIC:0),Math.max(1,size));
        BufferedImage measure=Images.create(1,1);Graphics2D mg=measure.createGraphics();FontMetrics fm=mg.getFontMetrics(font);int w=fm.stringWidth(text)+size*2,hh=fm.getHeight()+size;mg.dispose();
        TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Text",layerId);tiles.captureBefore(layer.getImage(),new Rectangle((int)x-size,(int)y-fmSafe(family,size),w+size*2,hh+size*2));
        Graphics2D g=layer.getImage().createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setFont(font);g.setColor(ColorUtil.parse(hex));g.drawString(text,(float)x,(float)y);g.dispose();finish(d,h,layer,tiles);
    }
    public void gradient(CarbonDocument d,HistoryManager h,String layerId,boolean radial,double x0,double y0,double x1,double y1,List<GradientStop>stops){
        if(stops.size()<2)throw new IllegalArgumentException("A gradient needs at least two stops.");Layer layer=raster(d,layerId);TilePixelEdit.Builder tiles=new TilePixelEdit.Builder("Gradient",layerId);tiles.captureBefore(layer.getImage(),new Rectangle(0,0,d.getWidth(),d.getHeight()));
        float[]fractions=new float[stops.size()];Color[]colors=new Color[stops.size()];for(int i=0;i<stops.size();i++){fractions[i]=(float)stops.get(i).offset();colors[i]=stops.get(i).color();}
        Graphics2D g=layer.getImage().createGraphics();
        if(radial){float radius=(float)Math.max(0.001,Math.hypot(x1-x0,y1-y0));g.setPaint(new RadialGradientPaint(new Point2D.Double(x0,y0),radius,fractions,colors,MultipleGradientPaint.CycleMethod.NO_CYCLE));}else g.setPaint(new LinearGradientPaint(new Point2D.Double(x0,y0),new Point2D.Double(x1,y1),fractions,colors));
        if(d.getSelection()!=null)g.clip(d.getSelection().toArea(d.getWidth(),d.getHeight()));g.fillRect(0,0,d.getWidth(),d.getHeight());g.dispose();finish(d,h,layer,tiles);
    }
    public void transform(CarbonDocument d,HistoryManager h,LayerService layers,String layerId,AffineTransform transform){
        Layer old=raster(d,layerId);layers.update(d,h,layerId,"Transform",layer->{BufferedImage out=Images.create(d.getWidth(),d.getHeight());Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(old.getImage(),transform,null);g.dispose();layer.setImage(out);});
    }
    private static int fmSafe(String family,int size){return Math.max(1,size);}
    private static void finish(CarbonDocument d,HistoryManager h,Layer layer,TilePixelEdit.Builder tiles){if(layer.isAlphaLock())tiles.preserveOriginalAlpha(layer.getImage());TilePixelEdit edit=tiles.build(layer.getImage());if(edit!=null){h.push(edit);d.markDirty();}}
    private static Layer raster(CarbonDocument d,String id){Layer l=d.findLayer(id);if(l==null)throw new IllegalArgumentException("Unknown layer: "+id);if(l.isLocked())throw new IllegalStateException("Layer is locked.");if(l.getType()!=LayerType.RASTER||l.getImage()==null)throw new IllegalStateException("This tool requires a raster layer.");if(l.isImageShared()){l=l.deepCopy();d.replaceLayer(l);}return l;}
    private static void add(ArrayDeque<Integer>q,boolean[]v,int i){if(!v[i]){v[i]=true;q.add(i);}}
    private static double distance(int a,int b){int dr=(a>>16&255)-(b>>16&255),dg=(a>>8&255)-(b>>8&255),db=(a&255)-(b&255),da=(a>>>24)-(b>>>24);return Math.sqrt(dr*dr+dg*dg+db*db+da*da);}
    private static int over(int dst,int src){double sa=(src>>>24)/255.0,da=(dst>>>24)/255.0,oa=sa+da*(1-sa);if(oa==0)return 0;int[]o=new int[3];for(int c=0;c<3;c++){int shift=16-c*8;double cs=(src>>shift&255)/255.0,cd=(dst>>shift&255)/255.0;o[c]=(int)Math.round(((cs*sa+cd*da*(1-sa))/oa)*255);}return((int)(oa*255)<<24)|(o[0]<<16)|(o[1]<<8)|o[2];}
    private static List<Integer> expand(List<Integer>source,boolean[]visited,int w,int h,int radius,Area selection){List<Integer>out=new ArrayList<>(source);boolean[]seen=visited.clone();for(int i:source){int x=i%w,y=i/w;for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=w||ny>=h)continue;int ni=ny*w+nx;if(!seen[ni]&&(selection==null||selection.contains(nx,ny))){seen[ni]=true;out.add(ni);}}}return out;}
    public record GradientStop(double offset,Color color){public GradientStop{if(offset<0||offset>1)throw new IllegalArgumentException("Gradient stop offset must be 0..1.");}}
}

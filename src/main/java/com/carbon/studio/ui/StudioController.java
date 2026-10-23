package com.carbon.studio.ui;

import com.carbon.studio.canvas.CarbonDocument;
import com.carbon.studio.canvas.Selection;
import com.carbon.studio.layers.LayerService;
import com.carbon.studio.layers.LayerType;
import com.carbon.studio.projects.ProjectManager;
import com.carbon.studio.projects.ProjectSession;
import com.carbon.studio.storage.AppPaths;
import com.carbon.studio.storage.AtomicProjectStorage;
import com.carbon.studio.storage.CarbonArchive;
import com.carbon.studio.storage.CarbonDatabase;
import com.carbon.studio.storage.ImageIOService;
import com.carbon.studio.storage.ProjectIndexRepository;
import com.carbon.studio.storage.RecoveryManager;
import com.carbon.studio.tools.Brush;
import com.carbon.studio.tools.BrushPresetLibrary;
import com.carbon.studio.tools.RasterEditingService;
import com.carbon.studio.tools.Symmetry;
import com.carbon.studio.tools.SymmetryMode;
import com.carbon.studio.tools.ToolType;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/** JavaFX desktop workspace backed by the real document, history and storage services. */
public class StudioController {
    private final Stage stage;private final CarbonDatabase database;private final ProjectManager projects;private final RecoveryManager recovery;private final ImageIOService images=new ImageIOService();private final LayerService layerService=new LayerService();private final RasterEditingService editing=new RasterEditingService();private final BrushPresetLibrary brushLibrary=new BrushPresetLibrary();
    private final ExecutorService tasks=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"carbon-background");t.setDaemon(true);return t;});private final ExecutorService recoveryIo=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"carbon-recovery");t.setDaemon(true);return t;});
    private final EditorState state=new EditorState();private final DocumentCanvas canvas=new DocumentCanvas();private final LayerPanel layers=new LayerPanel();private final TabPane tabs=new TabPane();private final Map<String,Tab>tabMap=new HashMap<>();private final Map<ToolType,ToggleButton>toolButtons=new HashMap<>();private final BorderPane root=new BorderPane();private final Pane canvasHost=new Pane();private final Label status=new Label("Ready");private final Label coordinates=new Label("");
    private final ColorPicker colorPicker=new ColorPicker(Color.web("#ff5c8a"));private final ComboBox<Brush>brushChoice=new ComboBox<>();private final Spinner<Double>sizeSpinner=new Spinner<>();private boolean updatingControls;private final Timeline autosave;
    public StudioController(Stage stage,AppPaths paths,CarbonDatabase database){
        this.stage=stage;this.database=database;AtomicProjectStorage storage=new AtomicProjectStorage(new CarbonArchive());projects=new ProjectManager(storage);recovery=new RecoveryManager(paths,storage,recoveryIo);
        state.setBrush(brushLibrary.defaultBrush());state.setEraser(brushLibrary.defaultEraser());build();installShortcuts();
        autosave=new Timeline(new KeyFrame(Duration.seconds(60),e->autosaveDirty()));autosave.setCycleCount(Timeline.INDEFINITE);autosave.play();
        createDocument(1280,960,"Untitled 1",null);restorePrompt();
    }
    public Scene scene(){Scene scene=new Scene(root,1440,900);scene.getStylesheets().add(getClass().getResource("/com/carbon/studio/ui/carbon.css").toExternalForm());return scene;}
    private void build(){
        root.setTop(new VBox(menuBar(),topTools()));root.setLeft(toolPalette());root.setRight(layers);root.setBottom(statusBar());
        canvasHost.getChildren().add(canvas);canvasHost.setStyle("-fx-background-color:#151920;");canvas.canvasNode().setOnScroll(e->{ProjectSession s=active();if(s==null)return;var point=canvasHost.sceneToLocal(e.getSceneX(),e.getSceneY());s.document().getViewport().zoomAt(e.getDeltaY()>0?1.12:1/1.12,point.getX(),point.getY());canvas.refreshTransform();changed();e.consume();});BorderPane center=new BorderPane();tabs.setPrefHeight(42);tabs.setMinHeight(42);tabs.setMaxHeight(42);center.setTop(tabs);center.setCenter(canvasHost);root.setCenter(center);
        tabs.getSelectionModel().selectedItemProperty().addListener((o,old,tab)->{if(tab==null)return;ProjectSession s=(ProjectSession)tab.getUserData();if(s!=null&&projects.active()!=s){projects.activate(s.id());attach(s);}});
        canvasHost.widthProperty().addListener((o,a,b)->{if(canvas.document()!=null)canvas.refreshTransform();});
        layers.setSession(null,this::changed);
    }
    private MenuBar menuBar(){
        MenuItem newFile=item("New…",this::newDocument),open=item("Open…",this::open),save=item("Save",()->save(false)),saveAs=item("Save As…",()->save(true)),importImage=item("Import image…",this::importImage),export=item("Export image…",this::exportImage),quit=item("Exit",()->{if(projects.closeAll(this::discardGuard))stage.close();});
        Menu file=new Menu("File",null,newFile,open,new SeparatorMenuItem(),save,saveAs,new SeparatorMenuItem(),importImage,export,new SeparatorMenuItem(),quit);
        Menu edit=new Menu("Edit",null,item("Undo",this::undo),item("Redo",this::redo),new SeparatorMenuItem(),item("Select All",()->selectAll(false)),item("Deselect",()->selectAll(true)),item("Invert Selection",this::invertSelection),new SeparatorMenuItem(),item("Clear selection / layer",this::clear));
        Menu layer=new Menu("Layer",null,item("New raster layer",()->layerOp(()->layerService.add(active().document(),active().history(),LayerType.RASTER))),item("New Crisp Ink layer",()->layerOp(()->layerService.add(active().document(),active().history(),LayerType.INK))),item("New adjustment layer",()->layerOp(()->layerService.add(active().document(),active().history(),LayerType.ADJUST))),item("New group",()->layerOp(()->layerService.add(active().document(),active().history(),LayerType.GROUP))),new SeparatorMenuItem(),item("Duplicate",()->layerOp(()->layerService.duplicate(active().document(),active().history(),activeId()))),item("Merge down",()->layerOp(()->layerService.mergeDown(active().document(),active().history(),activeId()))),item("Rasterize Crisp Ink",()->layerOp(()->layerService.rasterize(active().document(),active().history(),activeId()))),item("Clear layer",()->layerOp(()->layerService.clear(active().document(),active().history(),activeId()))),item("Delete layer",()->layerOp(()->layerService.delete(active().document(),active().history(),activeId()))));
        Menu image=new Menu("Image",null,item("Canvas size…",()->resize(false)),item("Scale image…",()->resize(true)),new SeparatorMenuItem(),item("Flip horizontal",()->layerOp(()->layerService.flip(active().document(),active().history(),true))),item("Flip vertical",()->layerOp(()->layerService.flip(active().document(),active().history(),false))),item("Rotate 90° clockwise",()->layerOp(()->layerService.rotate(active().document(),active().history(),true))),item("Rotate 90° counter-clockwise",()->layerOp(()->layerService.rotate(active().document(),active().history(),false))));
        Menu view=new Menu("View",null,item("Zoom in",()->zoom(1.25)),item("Zoom out",()->zoom(.8)),item("Fit",()->canvas.fit(canvasHost.getWidth(),canvasHost.getHeight())),item("Actual size",()->{if(active()!=null){active().document().getViewport().setZoom(1);canvas.refreshTransform();}}),item("Reset rotation / mirror",()->{if(active()!=null){active().document().getViewport().setRotation(0);active().document().getViewport().setFlip(false);canvas.refreshTransform();}}));
        Menu help=new Menu("Help",null,item("Quick Start",()->message("Quick Start","Choose Brush (B), pick a color, and draw. Use E for Eraser, G for Fill, M for selection, H to pan, and the mouse wheel to zoom. Ctrl+S saves a real .carbon project; File → Export writes PNG/JPEG.")),item("About Carbon: Java Edition",()->message("Carbon: Java Edition","Core migration of Carbon: Prime 7.9.9+ls. This build contains a functional editor core; advanced Carbon studios, complete rendering parity, stylus hardware acceptance, and Windows packaging remain under migration.")));
        MenuBar bar=new MenuBar(file,edit,layer,image,view,help);bar.setUseSystemMenuBar(false);return bar;
    }
    private Node topTools(){
        brushChoice.setPrefWidth(210);ArrayList<Brush>brushes=new ArrayList<>(brushLibrary.builtins());for(var pack:brushLibrary.packs())brushes.addAll(pack.brushes());brushChoice.setItems(FXCollections.observableArrayList(brushes));brushChoice.setCellFactory(v->new BrushCell());brushChoice.setButtonCell(new BrushCell());brushChoice.setValue(state.brush());brushChoice.setOnAction(e->{Brush b=brushChoice.getValue();if(b!=null){state.setBrush(b.copy());updatingControls=true;sizeSpinner.getValueFactory().setValue(b.getSize());updatingControls=false;}});
        sizeSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(.5,500,state.brush().getSize(),1));sizeSpinner.setPrefWidth(90);sizeSpinner.valueProperty().addListener((o,a,b)->{if(!updatingControls&&state.brush()!=null){Brush brush=state.brush().copy();brush.setSize(b);state.setBrush(brush);}});
        Slider opacity=new Slider(0,1,1);opacity.setPrefWidth(90);opacity.setTooltip(new javafx.scene.control.Tooltip("Brush opacity"));opacity.valueProperty().addListener((o,a,b)->{Brush brush=state.brush();if(brush!=null){Brush copy=brush.copy();copy.setOpacity(b.doubleValue());state.setBrush(copy);}});
        colorPicker.setPrefWidth(105);colorPicker.valueProperty().addListener((o,a,b)->state.setColor(toHex(b)));
        ComboBox<SymmetryMode>symmetry=new ComboBox<>(FXCollections.observableArrayList(SymmetryMode.values()));symmetry.setValue(SymmetryMode.OFF);symmetry.setOnAction(e->{Symmetry s=new Symmetry();s.setMode(symmetry.getValue());state.setSymmetry(s);});
        ToolBar bar=new ToolBar(new Label("Brush"),brushChoice,new Label("Size"),sizeSpinner,new Label("Opacity"),opacity,new Label("Color"),colorPicker,new Label("Symmetry"),symmetry);bar.setMinHeight(48);return bar;
    }
    private Node toolPalette(){
        ToggleGroup group=new ToggleGroup();VBox box=new VBox(5);box.setPadding(new Insets(8));ToolType[]tools={ToolType.BRUSH,ToolType.ERASER,ToolType.PICKER,ToolType.FILL,ToolType.SELECT,ToolType.LINE,ToolType.RECT,ToolType.ELLIPSE,ToolType.TEXT,ToolType.GRADIENT,ToolType.HAND,ToolType.ZOOM};
        for(ToolType type:tools){ToggleButton button=new ToggleButton(type.shortcut().isEmpty()?type.label():type.shortcut()+"  "+type.label());button.setToggleGroup(group);button.setPrefWidth(112);button.setUserData(type);toolButtons.put(type,button);button.setOnAction(e->selectTool(type));if(type==ToolType.BRUSH)button.setSelected(true);box.getChildren().add(button);}ScrollPane scroll=new ScrollPane(box);scroll.setFitToWidth(true);scroll.setPrefWidth(132);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);return scroll;
    }
    private Node statusBar(){javafx.scene.layout.HBox bar=new javafx.scene.layout.HBox(12,status,new Pane(),coordinates);bar.setPadding(new Insets(6,10,6,10));javafx.scene.layout.HBox.setHgrow(bar.getChildren().get(1),Priority.ALWAYS);status.setMaxWidth(Double.MAX_VALUE);return bar;}
    private void installShortcuts(){root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{
        if(e.getTarget() instanceof TextInputControl)return;
        if(new KeyCodeCombination(KeyCode.Z,KeyCombination.SHORTCUT_DOWN).match(e)){undo();e.consume();}
        else if(new KeyCodeCombination(KeyCode.Y,KeyCombination.SHORTCUT_DOWN).match(e)||new KeyCodeCombination(KeyCode.Z,KeyCombination.SHORTCUT_DOWN,KeyCombination.SHIFT_DOWN).match(e)){redo();e.consume();}
        else if(new KeyCodeCombination(KeyCode.S,KeyCombination.SHORTCUT_DOWN).match(e)){save(false);e.consume();}
        else if(new KeyCodeCombination(KeyCode.O,KeyCombination.SHORTCUT_DOWN).match(e)){open();e.consume();}
        else if(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN).match(e)){newDocument();e.consume();}
        else if(new KeyCodeCombination(KeyCode.A,KeyCombination.SHORTCUT_DOWN).match(e)){selectAll(false);e.consume();}
        else if(new KeyCodeCombination(KeyCode.D,KeyCombination.SHORTCUT_DOWN).match(e)){selectAll(true);e.consume();}
        else if(!e.isControlDown()&&!e.isMetaDown()&&!e.isAltDown()){ToolType tool=switch(e.getCode()){case B->ToolType.BRUSH;case E->ToolType.ERASER;case I->ToolType.PICKER;case G->ToolType.FILL;case M->ToolType.SELECT;case L->ToolType.LINE;case U->ToolType.RECT;case O->ToolType.ELLIPSE;case X->ToolType.TEXT;case H->ToolType.HAND;case Z->ToolType.ZOOM;default->null;};if(tool!=null){selectTool(tool);e.consume();}}
    });}
    private void selectTool(ToolType tool){state.setTool(tool);ToggleButton button=toolButtons.get(tool);if(button!=null)button.setSelected(true);status.setText(tool.label());}
    private void createDocument(int w,int h,String name,String background){try{ProjectSession s=projects.create(w,h,name,background);addTab(s);}catch(RuntimeException e){error(e);}}
    private void addTab(ProjectSession s){
        Tab tab=new Tab();tab.setUserData(s);tab.setClosable(true);tab.setOnCloseRequest(e->{if(!projects.close(s,this::discardGuard)){e.consume();return;}tabMap.remove(s.id());if(projects.active()==null){canvas.setDocument(null);layers.setSession(null,this::changed);}});tabMap.put(s.id(),tab);tabs.getTabs().add(tab);tabs.getSelectionModel().select(tab);projects.activate(s.id());attach(s);
    }
    private void attach(ProjectSession s){canvas.setDocument(s.document());layers.setSession(s,this::changed);canvas.fit(canvasHost.getWidth()>100?canvasHost.getWidth():1200,canvasHost.getHeight()>100?canvasHost.getHeight():720);changed();}
    private void changed(){for(var entry:tabMap.entrySet()){ProjectSession s=projects.sessions().stream().filter(p->p.id().equals(entry.getKey())).findFirst().orElse(null);if(s!=null)entry.getValue().setText(s.displayName());}ProjectSession s=active();if(s!=null){stage.setTitle(s.displayName()+" — Carbon: Java Edition");status.setText(s.document().getWidth()+" × "+s.document().getHeight()+"  •  "+Math.round(s.document().getViewport().getZoom()*100)+"%  •  "+state.tool().label());}else stage.setTitle("Carbon: Java Edition");if(!updatingControls){updatingControls=true;try{colorPicker.setValue(Color.web(state.color()));}finally{updatingControls=false;}}}
    private void newDocument(){Dialog<NewRequest>d=new Dialog<>();d.setTitle("New Carbon document");d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);TextField name=new TextField("Untitled"),width=new TextField("1920"),height=new TextField("1080");ColorPicker background=new ColorPicker(Color.TRANSPARENT);GridPane grid=new GridPane();grid.setHgap(8);grid.setVgap(8);grid.addRow(0,new Label("Name"),name);grid.addRow(1,new Label("Width"),width);grid.addRow(2,new Label("Height"),height);grid.addRow(3,new Label("Background"),background);d.getDialogPane().setContent(grid);d.setResultConverter(b->{if(b!=ButtonType.OK)return null;try{return new NewRequest(Integer.parseInt(width.getText()),Integer.parseInt(height.getText()),name.getText(),background.getValue().getOpacity()==0?null:toHex(background.getValue()));}catch(Exception e){return null;}});d.showAndWait().ifPresent(r->createDocument(r.w,r.h,r.name,r.background));}
    private void open(){FileChooser c=chooser("Open Carbon project");c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Carbon project","*.carbon"));File f=c.showOpenDialog(stage);if(f==null)return;status.setText("Opening "+f.getName()+"…");projects.open(f.toPath()).thenAccept(doc->Platform.runLater(()->{ProjectSession s=projects.addDocument(doc);addTab(s);index(doc);status.setText("Opened "+f.getName());})).exceptionally(e->{Platform.runLater(()->error(e));return null;});}
    private void save(boolean as){ProjectSession s=active();if(s==null)return;Path target=s.document().getFilePath();if(as||target==null){FileChooser c=chooser("Save Carbon project");c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Carbon project","*.carbon"));if(target!=null)c.setInitialFileName(target.getFileName().toString());else c.setInitialFileName(s.document().getName()+".carbon");File f=c.showSaveDialog(stage);if(f==null)return;target=f.toPath();if(!target.getFileName().toString().toLowerCase().endsWith(".carbon"))target=target.resolveSibling(target.getFileName()+".carbon");}
        final Path destination=target;status.setText("Saving…");projects.save(s,destination,true).thenAccept(receipt->Platform.runLater(()->{changed();index(s.document());status.setText("Saved "+receipt.file().getFileName()+" • "+receipt.bytes()+" bytes");})).exceptionally(e->{Platform.runLater(()->error(e));return null;});
    }
    private void importImage(){ProjectSession s=active();if(s==null)return;FileChooser c=chooser("Import image");c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.gif","*.webp","*.bmp"));File f=c.showOpenDialog(stage);if(f==null)return;status.setText("Decoding image…");CompletableFuture.supplyAsync(()->{try{return images.prepareImport(f.toPath());}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},tasks).thenAccept(prepared->Platform.runLater(()->{try{images.importLayer(s.document(),s.history(),prepared,0,0,prepared.image().getWidth(),prepared.image().getHeight());canvas.renderFullAsync();changed();status.setText("Imported "+f.getName());}catch(Exception e){error(e);}})).exceptionally(e->{Platform.runLater(()->error(e));return null;});}
    private void exportImage(){ProjectSession s=active();if(s==null)return;FileChooser c=chooser("Export image");c.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG","*.png"),new FileChooser.ExtensionFilter("JPEG","*.jpg"));c.setInitialFileName(s.document().getName()+".png");File f=c.showSaveDialog(stage);if(f==null)return;TextInputDialog scaleDialog=new TextInputDialog("1");scaleDialog.setTitle("Export scale");scaleDialog.setHeaderText("Scale (0.01–16)");var scaleText=scaleDialog.showAndWait();if(scaleText.isEmpty())return;double scale;try{scale=Double.parseDouble(scaleText.get());}catch(Exception e){error(e);return;}CarbonDocument snapshot=s.document().copyForIo();status.setText("Exporting…");CompletableFuture.supplyAsync(()->{try{return images.export(snapshot,f.toPath(),scale,.92);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}},tasks).thenAccept(receipt->Platform.runLater(()->status.setText("Exported "+receipt.width()+" × "+receipt.height()+" • "+receipt.bytes()+" bytes"))).exceptionally(e->{Platform.runLater(()->error(e));return null;});}
    private void undo(){ProjectSession s=active();if(s!=null&&s.history().undo(s.document())){canvas.renderFullAsync();changed();}}
    private void redo(){ProjectSession s=active();if(s!=null&&s.history().redo(s.document())){canvas.renderFullAsync();changed();}}
    private void clear(){ProjectSession s=active();if(s==null)return;try{editing.clearSelection(s.document(),s.history(),activeId());canvas.renderFullAsync();changed();}catch(RuntimeException e){error(e);}}
    private void selectAll(boolean clear){ProjectSession s=active();if(s==null)return;editing.setSelection(s.document(),s.history(),clear?null:Selection.rectangle(0,0,s.document().getWidth(),s.document().getHeight()));canvas.refreshOverlay();changed();}
    private void invertSelection(){ProjectSession s=active();if(s==null)return;Selection selection=s.document().getSelection();if(selection==null){selectAll(false);selection=s.document().getSelection();}selection.setInverted(!selection.isInverted());editing.setSelection(s.document(),s.history(),selection);canvas.refreshOverlay();changed();}
    private void resize(boolean scale){ProjectSession s=active();if(s==null)return;TextInputDialog d=new TextInputDialog(s.document().getWidth()+","+s.document().getHeight());d.setTitle(scale?"Scale image":"Canvas size");d.setHeaderText("Width,height (maximum 8192 per side, 40 MP)");d.showAndWait().ifPresent(value->{try{String[]p=value.split(",");int w=Integer.parseInt(p[0].trim()),h=Integer.parseInt(p[1].trim());if(scale)layerService.scaleImage(s.document(),s.history(),w,h);else layerService.resizeCanvas(s.document(),s.history(),w,h,.5,.5);attach(s);}catch(Exception e){error(e);}});}
    private void layerOp(Runnable action){if(active()==null)return;try{action.run();canvas.renderFullAsync();layers.refresh();changed();}catch(RuntimeException e){error(e);}}
    private void zoom(double factor){if(active()==null)return;active().document().getViewport().setZoom(active().document().getViewport().getZoom()*factor);canvas.refreshTransform();changed();}
    private void autosaveDirty(){for(ProjectSession s:projects.sessions())if(s.document().isDirty())recovery.autosave(s.document()).exceptionally(e->{Platform.runLater(()->status.setText("Autosave failed: "+rootCause(e).getMessage()));return null;});}
    private void restorePrompt(){try{var records=recovery.list();if(records.isEmpty())return;Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"A recovery copy is available for "+records.get(0).name()+". Restore it?",ButtonType.YES,ButtonType.NO);alert.setHeaderText("Carbon recovery");alert.showAndWait().filter(ButtonType.YES::equals).ifPresent(b->{try{addTab(projects.addDocument(recovery.recover(records.get(0))));}catch(Exception e){error(e);}});}catch(Exception e){status.setText("Recovery scan failed: "+e.getMessage());}}
    private void index(CarbonDocument d){if(database==null||d.getFilePath()==null)return;try{new ProjectIndexRepository(database).upsert(d.getUuid(),d.getName(),d.getFilePath().toString(),null,System.currentTimeMillis(),java.nio.file.Files.size(d.getFilePath()));}catch(Exception e){status.setText("Project index warning: "+e.getMessage());}}
    private boolean discardGuard(CarbonDocument d){Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Close "+d.getName()+" and discard unsaved changes?",ButtonType.YES,ButtonType.NO);alert.setHeaderText("Unsaved changes");return alert.showAndWait().orElse(ButtonType.NO)==ButtonType.YES;}
    private FileChooser chooser(String title){FileChooser c=new FileChooser();c.setTitle(title);return c;}
    private MenuItem item(String name,Runnable action){MenuItem item=new MenuItem(name);item.setOnAction(e->action.run());return item;}
    private ProjectSession active(){return projects.active();}private String activeId(){return active().document().getActiveLayerId();}
    private void message(String title,String text){Alert a=new Alert(Alert.AlertType.INFORMATION,text);a.setTitle(title);a.setHeaderText(title);a.setResizable(true);a.showAndWait();}
    private void error(Throwable error){Throwable cause=rootCause(error);status.setText(cause.getMessage());Alert a=new Alert(Alert.AlertType.ERROR,cause.getMessage()==null?cause.toString():cause.getMessage());a.setTitle("Carbon operation failed");a.setHeaderText("The operation could not be completed");a.setResizable(true);a.showAndWait();}
    private static Throwable rootCause(Throwable e){while((e instanceof java.util.concurrent.CompletionException||e instanceof java.util.concurrent.ExecutionException)&&e.getCause()!=null)e=e.getCause();return e;}
    private static String toHex(Color c){return c.getOpacity()<1?String.format("#%02x%02x%02x%02x",(int)(c.getRed()*255+.5),(int)(c.getGreen()*255+.5),(int)(c.getBlue()*255+.5),(int)(c.getOpacity()*255+.5)):String.format("#%02x%02x%02x",(int)(c.getRed()*255+.5),(int)(c.getGreen()*255+.5),(int)(c.getBlue()*255+.5));}
    public boolean closeRequested(){return projects.closeAll(this::discardGuard);}
    public void close(){autosave.stop();canvas.close();projects.close();tasks.shutdownNow();recoveryIo.shutdown();if(database!=null)try{database.close();}catch(Exception ignored){}}
    private static class BrushCell extends ListCell<Brush>{@Override protected void updateItem(Brush b,boolean empty){super.updateItem(b,empty);setText(empty||b==null?null:b.getName());}}
    private record NewRequest(int w,int h,String name,String background){}
}

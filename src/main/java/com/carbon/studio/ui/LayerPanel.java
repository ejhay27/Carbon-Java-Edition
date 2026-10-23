package com.carbon.studio.ui;

import com.carbon.studio.layers.BlendMode;
import com.carbon.studio.layers.Layer;
import com.carbon.studio.layers.LayerService;
import com.carbon.studio.layers.LayerType;
import com.carbon.studio.projects.ProjectSession;
import java.util.ArrayList;
import java.util.Collections;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Live layer list and undoable layer commands. */
public class LayerPanel extends VBox {
    private final ListView<Layer>list=new ListView<>();private final Slider opacity=new Slider(0,1,1);private final ComboBox<BlendMode>blend=new ComboBox<>(FXCollections.observableArrayList(BlendMode.values()));private final LayerService service=new LayerService();
    private ProjectSession session;private Runnable changed;private boolean updating;
    public LayerPanel(){
        setSpacing(7);setPadding(new Insets(10));setPrefWidth(250);Label title=new Label("LAYERS");title.getStyleClass().add("panel-title");list.setPrefHeight(360);list.setCellFactory(v->new LayerCell());
        list.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(updating||b==null||session==null)return;session.document().setActiveLayerId(b.getId());syncProperties();});
        Button raster=new Button("+ Raster"),ink=new Button("+ Ink"),duplicate=new Button("Duplicate"),merge=new Button("Merge"),up=new Button("↑"),down=new Button("↓"),delete=new Button("Delete");
        raster.setOnAction(e->run(()->service.add(session.document(),session.history(),LayerType.RASTER)));ink.setOnAction(e->run(()->service.add(session.document(),session.history(),LayerType.INK)));duplicate.setOnAction(e->run(()->service.duplicate(session.document(),session.history(),session.document().getActiveLayerId())));merge.setOnAction(e->run(()->service.mergeDown(session.document(),session.history(),session.document().getActiveLayerId())));delete.setOnAction(e->run(()->service.delete(session.document(),session.history(),session.document().getActiveLayerId())));up.setOnAction(e->move(1));down.setOnAction(e->move(-1));
        ToolBar buttons=new ToolBar(raster,ink,duplicate,merge,up,down,delete);buttons.setStyle("-fx-background-color:transparent;-fx-padding:0;");opacity.setShowTickLabels(true);opacity.valueProperty().addListener((o,a,b)->{if(!updating&&session!=null)opacity.setUserData(b.doubleValue());});opacity.setOnMouseReleased(e->run(()->service.update(session.document(),session.history(),session.document().getActiveLayerId(),"Layer opacity",l->l.setOpacity(opacity.getValue()))));
        blend.setOnAction(e->{if(!updating&&blend.getValue()!=null)run(()->service.update(session.document(),session.history(),session.document().getActiveLayerId(),"Layer blend",l->l.setBlend(blend.getValue())));});
        VBox.setVgrow(list,Priority.ALWAYS);getChildren().addAll(title,list,buttons,new Label("Opacity"),opacity,new Label("Blend mode"),blend);
    }
    public void setSession(ProjectSession value,Runnable changed){session=value;this.changed=changed;refresh();}
    public void refresh(){
        updating=true;try{if(session==null){list.getItems().clear();return;}ArrayList<Layer>layers=new ArrayList<>(session.document().getLayers());Collections.reverse(layers);list.setItems(FXCollections.observableArrayList(layers));Layer active=session.document().getActiveLayer();list.getSelectionModel().select(active);syncProperties();}finally{updating=false;}
    }
    private void syncProperties(){if(session==null||session.document().getActiveLayer()==null)return;Layer l=session.document().getActiveLayer();updating=true;try{opacity.setValue(l.getOpacity());blend.setValue(l.getBlend());}finally{updating=false;}}
    private void move(int direction){if(session==null)return;int index=session.document().getLayers().indexOf(session.document().getActiveLayer());run(()->service.move(session.document(),session.history(),session.document().getActiveLayerId(),index+direction));}
    private void run(Runnable action){if(session==null)return;try{action.run();refresh();if(changed!=null)changed.run();}catch(RuntimeException e){new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,e.getMessage()).showAndWait();}}
    private class LayerCell extends ListCell<Layer>{
        @Override protected void updateItem(Layer layer,boolean empty){super.updateItem(layer,empty);if(empty||layer==null){setText(null);setGraphic(null);return;}Label name=new Label(layer.getName());name.setMaxWidth(145);Button visible=new Button(layer.isVisible()?"●":"○");visible.setStyle("-fx-background-color:transparent;-fx-text-fill:#b7c3d8;");visible.setOnAction(e->{LayerPanel.this.session.document().setActiveLayerId(layer.getId());run(()->service.update(session.document(),session.history(),layer.getId(),"Layer visibility",l->l.setVisible(!l.isVisible())));});HBox box=new HBox(7,visible,name);setGraphic(box);setText(null);}
    }
}

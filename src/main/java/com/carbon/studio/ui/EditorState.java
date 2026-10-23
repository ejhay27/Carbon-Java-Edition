package com.carbon.studio.ui;

import com.carbon.studio.tools.Brush;
import com.carbon.studio.tools.Symmetry;
import com.carbon.studio.tools.ToolType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** UI tool settings only; project artwork colors remain document data. */
public class EditorState {
    private final ObjectProperty<ToolType>tool=new SimpleObjectProperty<>(ToolType.BRUSH);
    private final ObjectProperty<Brush>brush=new SimpleObjectProperty<>();
    private final ObjectProperty<Brush>eraser=new SimpleObjectProperty<>();
    private final StringProperty color=new SimpleStringProperty("#ff5c8a");
    private final ObjectProperty<Symmetry>symmetry=new SimpleObjectProperty<>(new Symmetry());
    public ObjectProperty<ToolType>toolProperty(){return tool;}public ToolType tool(){return tool.get();}public void setTool(ToolType value){tool.set(value);}
    public ObjectProperty<Brush>brushProperty(){return brush;}public Brush brush(){return brush.get();}public void setBrush(Brush value){brush.set(value);}
    public ObjectProperty<Brush>eraserProperty(){return eraser;}public Brush eraser(){return eraser.get();}public void setEraser(Brush value){eraser.set(value);}
    public StringProperty colorProperty(){return color;}public String color(){return color.get();}public void setColor(String value){color.set(value);}
    public ObjectProperty<Symmetry>symmetryProperty(){return symmetry;}public Symmetry symmetry(){return symmetry.get();}public void setSymmetry(Symmetry value){symmetry.set(value);}
}

package com.carbon.studio.tools;

/** Tools present in Carbon 7.9.9's tool model. */
public enum ToolType {
    BRUSH("brush", "Brush", "B"),
    ERASER("eraser", "Eraser", "E"),
    PICKER("picker", "Color Picker", "I"),
    FILL("fill", "Fill", "G"),
    SELECT("select", "Selection", "M"),
    TRANSFORM("transform", "Transform", "T"),
    MOVE("move", "Move", "V"),
    LINE("line", "Line", "L"),
    RECT("rect", "Rectangle", "U"),
    ELLIPSE("ellipse", "Ellipse", "O"),
    TEXT("text", "Text", "X"),
    INK_EDIT("inkedit", "Crisp Ink Edit", "A"),
    HAND("hand", "Hand / Pan", "H"),
    ZOOM("zoom", "Zoom", "Z"),
    ROTATE("rotate", "Rotate View", "R"),
    SYMMETRY("symmetry", "Symmetry", "Y"),
    GRADIENT("gradient", "Gradient", ""),
    LIQUIFY("liquify", "Liquify", ""),
    COMIC("comic", "Comic", "");

    private final String id;
    private final String label;
    private final String shortcut;

    ToolType(String id, String label, String shortcut) {
        this.id = id;
        this.label = label;
        this.shortcut = shortcut;
    }

    public String id() { return id; }
    public String label() { return label; }
    public String shortcut() { return shortcut; }

    public static ToolType fromId(String id) {
        for (ToolType tool : values()) if (tool.id.equals(id)) return tool;
        throw new IllegalArgumentException("Unknown Carbon tool: " + id);
    }
}

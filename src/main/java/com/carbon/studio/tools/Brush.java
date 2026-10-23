package com.carbon.studio.tools;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-compatible Carbon brush definition. Gson populates this type from the audited
 * 7.9.9 resource; rendering code never depends on TypeScript.
 */
public class Brush implements Cloneable {
    String id = "custom-brush";
    String name = "Custom Brush";
    String category = "Custom";
    List<String> tags = new ArrayList<>();
    String description = "";
    boolean builtin;
    int version = 1;
    BrushMode mode = BrushMode.NORMAL;
    TipShape tip = TipShape.ROUND;
    String stamp;
    double size = 12;
    double minSize = 0.2;
    double maxSize = 120;
    double opacity = 1;
    double flow = 1;
    double spacing = 0.08;
    double hardness = 0.9;
    double smoothing = 0.25;
    double pressureSize = 1;
    double pressureOpacity = 0;
    boolean tiltAngle;
    double tiltSize;
    double angle;
    boolean followDirection;
    double roundness = 1;
    double scatter;
    int count = 1;
    double sizeJitter;
    double angleJitter;
    double opacityJitter;
    double hueJitter;
    double satJitter;
    double valJitter;
    double grain;
    GrainType grainType = GrainType.PAPER;
    double grainScale = 1;
    double wetEdges;
    double granulation;
    double bloom;
    double wetness;
    double pickup;
    double taperStart;
    double taperEnd;
    double speedSize;
    StrokeComposite composite = StrokeComposite.SOURCE_OVER;
    JsonObject layer2;
    JsonObject recipe;

    public String getId() { return id; }
    public void setId(String id) { this.id = requireText(id, "Brush id"); }
    public String getName() { return name; }
    public void setName(String name) { this.name = requireText(name, "Brush name"); }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category == null ? "Custom" : category; }
    public List<String> getTags() { return List.copyOf(tags == null ? List.of() : tags); }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public boolean isBuiltin() { return builtin; }
    public int getVersion() { return version; }
    public BrushMode getMode() { return mode == null ? BrushMode.NORMAL : mode; }
    public void setMode(BrushMode mode) { this.mode = mode == null ? BrushMode.NORMAL : mode; }
    public TipShape getTip() { return tip == null ? TipShape.ROUND : tip; }
    public void setTip(TipShape tip) { this.tip = tip == null ? TipShape.ROUND : tip; }
    public String getStamp() { return stamp; }
    public void setStamp(String stamp) { this.stamp = stamp; }
    public double getSize() { return size; }
    public void setSize(double size) { this.size = clamp(size, 0.5, Math.max(0.5, maxSize)); }
    public double getMinSize() { return minSize; }
    public void setMinSize(double minSize) { this.minSize = clamp(minSize, 0, 1); }
    public double getMaxSize() { return maxSize; }
    public void setMaxSize(double maxSize) { this.maxSize = clamp(maxSize, 0.5, 1000); setSize(size); }
    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = clamp01(opacity); }
    public double getFlow() { return flow; }
    public void setFlow(double flow) { this.flow = clamp01(flow); }
    public double getSpacing() { return spacing; }
    public void setSpacing(double spacing) { this.spacing = clamp(spacing, 0.005, 4); }
    public double getHardness() { return hardness; }
    public void setHardness(double hardness) { this.hardness = clamp01(hardness); }
    public double getSmoothing() { return smoothing; }
    public void setSmoothing(double smoothing) { this.smoothing = clamp01(smoothing); }
    public double getPressureSize() { return pressureSize; }
    public void setPressureSize(double value) { pressureSize = clamp01(value); }
    public double getPressureOpacity() { return pressureOpacity; }
    public void setPressureOpacity(double value) { pressureOpacity = clamp01(value); }
    public boolean isTiltAngle() { return tiltAngle; }
    public void setTiltAngle(boolean value) { tiltAngle = value; }
    public double getTiltSize() { return tiltSize; }
    public void setTiltSize(double value) { tiltSize = clamp(value, 0, 10); }
    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
    public boolean isFollowDirection() { return followDirection; }
    public void setFollowDirection(boolean value) { followDirection = value; }
    public double getRoundness() { return roundness; }
    public void setRoundness(double value) { roundness = clamp(value, 0.05, 1); }
    public double getScatter() { return scatter; }
    public void setScatter(double value) { scatter = clamp(value, 0, 20); }
    public int getCount() { return count; }
    public void setCount(int value) { count = Math.max(1, Math.min(64, value)); }
    public double getGrain() { return grain; }
    public void setGrain(double value) { grain = clamp01(value); }
    public GrainType getGrainType() { return grainType; }
    public void setGrainType(GrainType value) { grainType = value == null ? GrainType.PAPER : value; }
    public double getPickup() { return pickup; }
    public void setPickup(double value) { pickup = clamp01(value); }
    public double getWetness() { return wetness; }
    public void setWetness(double value) { wetness = clamp01(value); }
    public double getTaperStart() { return taperStart; }
    public void setTaperStart(double value) { taperStart = clamp(value, 0, 10); }
    public double getTaperEnd() { return taperEnd; }
    public void setTaperEnd(double value) { taperEnd = clamp(value, 0, 10); }
    public StrokeComposite getComposite() { return composite == null ? StrokeComposite.SOURCE_OVER : composite; }
    public void setComposite(StrokeComposite value) { composite = value == null ? StrokeComposite.SOURCE_OVER : value; }
    public JsonObject getLayer2() { return layer2 == null ? null : layer2.deepCopy(); }
    public JsonObject getRecipe() { return recipe == null ? null : recipe.deepCopy(); }

    /** Behavior-relevant values used by the stroke renderer. Package access avoids a second parallel parameter model. */
    double sizeJitter() { return sizeJitter; }
    double angleJitter() { return angleJitter; }
    double opacityJitter() { return opacityJitter; }
    double hueJitter() { return hueJitter; }
    double satJitter() { return satJitter; }
    double valJitter() { return valJitter; }
    double grainScale() { return grainScale; }
    double wetEdges() { return wetEdges; }
    double granulation() { return granulation; }
    double bloom() { return bloom; }
    double speedSize() { return speedSize; }

    public Brush copy() {
        try {
            Brush b = (Brush) super.clone();
            b.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
            b.layer2 = layer2 == null ? null : layer2.deepCopy();
            b.recipe = recipe == null ? null : recipe.deepCopy();
            return b;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public void validate() {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Brush id is missing.");
        if (name == null || name.isBlank()) name = "Imported Brush";
        if (tags == null) tags = new ArrayList<>();
        if (mode == null) mode = BrushMode.NORMAL;
        if (tip == null) tip = TipShape.ROUND;
        if (grainType == null) grainType = GrainType.PAPER;
        if (composite == null) composite = StrokeComposite.SOURCE_OVER;
        maxSize = clamp(maxSize, 0.5, 1000);
        size = clamp(size, 0.5, maxSize);
        opacity = clamp01(opacity); flow = clamp01(flow); hardness = clamp01(hardness);
        spacing = clamp(spacing, 0.005, 4); smoothing = clamp01(smoothing);
        roundness = clamp(roundness, 0.05, 1); count = Math.max(1, Math.min(64, count));
    }

    static double clamp01(double v) { return clamp(v, 0, 1); }
    static double clamp(double v, double lo, double hi) {
        if (!Double.isFinite(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank.");
        return value;
    }
}

package com.carbon.studio.layers;

/** Non-destructive adjustment values used by Carbon adjustment layers. */
public class Adjustment {
    public double hue;
    public double sat = 1;
    public double bright = 1;
    public double contrast = 1;
    public double invert;
    public double gray;
    public double sepia;
    public double blur;

    public Adjustment copy() {
        Adjustment a = new Adjustment();
        a.hue = hue; a.sat = sat; a.bright = bright; a.contrast = contrast;
        a.invert = invert; a.gray = gray; a.sepia = sepia; a.blur = blur;
        return a;
    }
}

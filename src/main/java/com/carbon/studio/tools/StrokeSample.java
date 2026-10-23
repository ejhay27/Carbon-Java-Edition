package com.carbon.studio.tools;

/** One rounded input sample; the same values can later be journaled for replay. */
public record StrokeSample(double x,double y,double pressure,double tiltX,double tiltY,long timeMillis) {
    public StrokeSample {
        x=round2(x);y=round2(y);pressure=Brush.clamp01(pressure);
    }
    public StrokeSample(double x,double y,double pressure){this(x,y,pressure,0,0,System.currentTimeMillis());}
    private static double round2(double v){return Math.round(v*100)/100.0;}
}

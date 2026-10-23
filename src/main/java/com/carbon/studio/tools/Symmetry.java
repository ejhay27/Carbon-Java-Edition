package com.carbon.studio.tools;

/** Symmetry center is normalized to document coordinates, as in Carbon 7.9.9. */
public class Symmetry {
    private SymmetryMode mode = SymmetryMode.OFF;
    private int count = 6;
    private double cx = 0.5;
    private double cy = 0.5;

    public Symmetry() {}
    public Symmetry(SymmetryMode mode, int count, double cx, double cy) {
        this.mode = mode; this.count = Math.max(2, count); this.cx = cx; this.cy = cy;
    }
    public SymmetryMode getMode() { return mode; }
    public void setMode(SymmetryMode mode) { this.mode = mode == null ? SymmetryMode.OFF : mode; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = Math.max(2, Math.min(64, count)); }
    public double getCx() { return cx; }
    public void setCx(double cx) { this.cx = clamp01(cx); }
    public double getCy() { return cy; }
    public void setCy(double cy) { this.cy = clamp01(cy); }
    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
    public Symmetry copy() { return new Symmetry(mode, count, cx, cy); }
}

package com.carbon.studio.layers;

import com.google.gson.annotations.SerializedName;

/** The 18 blend names serialized by Carbon .carbon v1. */
public enum BlendMode {
    @SerializedName("source-over") SOURCE_OVER,
    @SerializedName("multiply") MULTIPLY,
    @SerializedName("screen") SCREEN,
    @SerializedName("overlay") OVERLAY,
    @SerializedName("darken") DARKEN,
    @SerializedName("lighten") LIGHTEN,
    @SerializedName("color-dodge") COLOR_DODGE,
    @SerializedName("color-burn") COLOR_BURN,
    @SerializedName("hard-light") HARD_LIGHT,
    @SerializedName("soft-light") SOFT_LIGHT,
    @SerializedName("difference") DIFFERENCE,
    @SerializedName("exclusion") EXCLUSION,
    @SerializedName("hue") HUE,
    @SerializedName("saturation") SATURATION,
    @SerializedName("color") COLOR,
    @SerializedName("luminosity") LUMINOSITY,
    @SerializedName("lighter") LIGHTER,
    @SerializedName("linear-burn") LINEAR_BURN;
    public String getCanvasName(){return switch(this){case SOURCE_OVER->"source-over";case COLOR_DODGE->"color-dodge";case COLOR_BURN->"color-burn";case HARD_LIGHT->"hard-light";case SOFT_LIGHT->"soft-light";case LINEAR_BURN->"linear-burn";default->name().toLowerCase().replace('_','-');};}
    public static BlendMode from(String value){for(BlendMode mode:values())if(mode.getCanvasName().equals(value))return mode;return SOURCE_OVER;}
}

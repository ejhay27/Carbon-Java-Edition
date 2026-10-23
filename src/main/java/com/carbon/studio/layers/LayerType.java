package com.carbon.studio.layers;

import com.google.gson.annotations.SerializedName;

public enum LayerType {
    @SerializedName("raster") RASTER,
    @SerializedName("ink") INK,
    @SerializedName("group") GROUP,
    @SerializedName("adjust") ADJUST,
    @SerializedName("comic") COMIC
}

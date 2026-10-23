package com.carbon.studio.tools;

import com.google.gson.annotations.SerializedName;

public enum GrainType {
    @SerializedName("paper") PAPER,
    @SerializedName("canvas") CANVAS,
    @SerializedName("fabric") FABRIC,
    @SerializedName("noise") NOISE,
    @SerializedName("rough") ROUGH
}

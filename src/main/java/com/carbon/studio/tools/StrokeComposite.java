package com.carbon.studio.tools;

import com.google.gson.annotations.SerializedName;

public enum StrokeComposite {
    @SerializedName("source-over") SOURCE_OVER,
    @SerializedName("multiply") MULTIPLY,
    @SerializedName("screen") SCREEN,
    @SerializedName("lighter") LIGHTER,
    @SerializedName("darken") DARKEN
}

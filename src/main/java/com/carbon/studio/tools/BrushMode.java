package com.carbon.studio.tools;

import com.google.gson.annotations.SerializedName;

public enum BrushMode {
    @SerializedName("normal") NORMAL,
    @SerializedName("wash") WASH,
    @SerializedName("smudge") SMUDGE,
    @SerializedName("mix") MIX,
    @SerializedName("crisp") CRISP
}

package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class MFALevel {
    @SerializedName("default")
    DEFAULT,

    @SerializedName("optional")
    OPTIONAL,

    @SerializedName("mandatory")
    MANDATORY,

    @SerializedName("none")
    NONE
}
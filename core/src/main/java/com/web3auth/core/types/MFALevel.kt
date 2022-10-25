package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

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
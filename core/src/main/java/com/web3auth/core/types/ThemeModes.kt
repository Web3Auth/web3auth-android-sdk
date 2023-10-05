package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

enum class ThemeModes {
    @SerializedName("light")
    LIGHT,

    @SerializedName("dark")
    DARK,

    @SerializedName("auto")
    AUTO
}
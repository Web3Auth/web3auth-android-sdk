package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class ThemeModes {
    @SerializedName("light")
    LIGHT,

    @SerializedName("dark")
    DARK,

    @SerializedName("auto")
    AUTO
}
package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class ChainNamespace {
    @SerializedName("eip155")
    EIP155,

    @SerializedName("solana")
    SOLANA
}
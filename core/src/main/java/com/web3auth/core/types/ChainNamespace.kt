package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

enum class ChainNamespace {
    @SerializedName("eip155")
    EIP155,

    @SerializedName("solana")
    SOLANA
}
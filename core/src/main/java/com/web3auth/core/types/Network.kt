package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

enum class Network {
    @SerializedName("mainnet")
    MAINNET,

    @SerializedName("testnet")
    TESTNET,

    @SerializedName("cyan")
    CYAN,

    @SerializedName("aqua")
    AQUA
}
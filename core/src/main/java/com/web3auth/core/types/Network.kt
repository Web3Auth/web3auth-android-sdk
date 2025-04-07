package com.web3auth.core.types

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
enum class Network {
    @SerializedName("mainnet")
    MAINNET,

    @SerializedName("testnet")
    TESTNET,

    @SerializedName("cyan")
    CYAN,

    @SerializedName("aqua")
    AQUA,

    @SerializedName("sapphire_devnet")
    SAPPHIRE_DEVNET,

    @SerializedName("sapphire_mainnet")
    SAPPHIRE_MAINNET
}
package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

enum class Curve {
    @SerializedName("secp256k1")
    SECP256K1,

    @SerializedName("ed25519")
    ED25519
}
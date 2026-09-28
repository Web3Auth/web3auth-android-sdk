package com.web3auth.core.types

import com.google.gson.annotations.SerializedName

data class WalletUiConfig(
    val enablePortfolioWidget: Boolean? = null,
    val enableConfirmationModal: Boolean? = null,
    val enableWalletConnect: Boolean? = null,
    val enableTokenDisplay: Boolean? = null,
    val enableNftDisplay: Boolean? = null,
    val enableShowAllTokensButton: Boolean? = null,
    val enableBuyButton: Boolean? = null,
    val enableSendButton: Boolean? = null,
    val enableSwapButton: Boolean? = null,
    val enableReceiveButton: Boolean? = null,
    val enableDefiPositionsDisplay: Boolean? = null,
    val portfolioWidgetPosition: ButtonPositionType? = null,
    val defaultPortfolio: DefaultPortfolioType? = null
)

enum class ButtonPositionType {
    @SerializedName("bottom-left")
    BOTTOM_LEFT,

    @SerializedName("top-left")
    TOP_LEFT,

    @SerializedName("bottom-right")
    BOTTOM_RIGHT,

    @SerializedName("top-right")
    TOP_RIGHT
}

enum class DefaultPortfolioType {
    @SerializedName("token")
    TOKEN,

    @SerializedName("nft")
    NFT,

    @SerializedName("defi")
    DEFI
}
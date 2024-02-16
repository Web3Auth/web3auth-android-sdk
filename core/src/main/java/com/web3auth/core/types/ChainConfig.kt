package com.web3auth.core.types

data class ChainConfig(
    val chainNamespace: ChainNamespace = ChainNamespace.EIP155,
    val decimals: Int = 18,
    val blockExplorerUrl: String? = null,
    val chainId: String,
    val displayName: String? = null,
    val logo: String? = null,
    val rpcTarget: String,
    val ticker: String,
    val tickerName: String? = null,
)
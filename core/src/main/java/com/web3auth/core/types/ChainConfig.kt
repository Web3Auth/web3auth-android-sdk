package com.web3auth.core.types

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class ChainConfig(
    @Keep val chainNamespace: ChainNamespace = ChainNamespace.EIP155,
    @Keep val decimals: Int? = 18,
    @Keep val blockExplorerUrl: String? = null,
    @Keep val chainId: String,
    @Keep val displayName: String? = null,
    @Keep val logo: String? = null,
    @Keep val rpcTarget: String,
    @Keep val ticker: String? = null,
    @Keep val tickerName: String? = null,
) : Serializable
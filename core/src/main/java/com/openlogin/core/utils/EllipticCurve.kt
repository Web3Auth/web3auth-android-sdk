package com.openlogin.core.utils

import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.ECDSASignature
import java.security.Security

fun installBouncyCastle() {
    val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        ?: return // Web3j will set up the provider lazily when it's first used.

    if (provider.javaClass == BouncyCastleProvider::class.java)
        return // BC with same package name, shouldn't happen in real life.

    // Android registers its own BC provider. As it might be outdated and might not include
    // all needed ciphers, we substitute it with a known BC bundled in the app.
    // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
    // of that it's possible to have another BC implementation loaded in VM.
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
    Security.insertProviderAt(BouncyCastleProvider(), 1)
}

fun ECDSASignature.toDER() = ECKey.ECDSASignature(r, s).encodeToDER()
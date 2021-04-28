package com.openlogin.core.utils

import org.web3j.utils.Numeric
import java.math.BigInteger

fun ByteArray.toHexString() = Numeric.toHexStringNoPrefix(this)

fun BigInteger.toHexString() = toString(16)

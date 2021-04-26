package com.openlogin.core

import org.web3j.crypto.Hash

fun keccak256(bytes: ByteArray) = Hash.sha3(bytes)

fun keccak256(s: String) = keccak256(s.toByteArray(Charsets.UTF_8))

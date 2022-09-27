package com.web3auth.core.types

class TorusException : Exception {
    constructor(msg: String?) : super(msg) {}
    constructor(msg: String?, err: Throwable?) : super(msg, err) {}
}
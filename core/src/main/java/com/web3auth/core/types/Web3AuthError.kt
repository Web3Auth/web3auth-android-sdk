package com.web3auth.core.types

object Web3AuthError {

    fun getError(errorCode: ErrorCode): String {
        return when (errorCode) {
            ErrorCode.NOUSERFOUND -> {
                "No user found, please login again!"
            }
            ErrorCode.ENCODING_ERROR -> {
                "Encoding Error"
            }
            ErrorCode.DECODING_ERROR -> {
                "Decoding Error"
            }
            ErrorCode.SOMETHING_WENT_WRONG -> {
                "Something went wrong!"
            }
            ErrorCode.RUNTIME_ERROR -> {
                "Runtime Error"
            }
            ErrorCode.APP_CANCELLED -> {
                "App Cancelled"
            }
        }
    }
}

enum class ErrorCode {
    NOUSERFOUND,
    ENCODING_ERROR,
    DECODING_ERROR,
    RUNTIME_ERROR,
    APP_CANCELLED,
    SOMETHING_WENT_WRONG,
}
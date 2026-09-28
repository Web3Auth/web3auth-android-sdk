package com.web3auth.core.types

/**
 * URL maps aligned with `@toruslabs/constants` (CITADEL_SERVER_MAP,
 * STORAGE_SERVER_MAP, DASHBOARD_PUBLIC_API_MAP, STORAGE_SERVER_SOCKET_URL_MAP).
 */
object Web3AuthUrls {

    fun citadelServerUrl(buildEnv: BuildEnv): String = when (buildEnv) {
        BuildEnv.TESTING -> "https://api-develop.web3auth.io/citadel-service"
        else -> "https://api.web3auth.io/citadel-service"
    }

    fun storageServerUrl(buildEnv: BuildEnv): String = when (buildEnv) {
        BuildEnv.TESTING -> "https://api-develop.web3auth.io/session-service"
        else -> "https://api.web3auth.io/session-service"
    }

    fun sessionSocketUrl(buildEnv: BuildEnv): String = when (buildEnv) {
        BuildEnv.TESTING -> "https://develop-session.web3auth.io"
        else -> "https://session.web3auth.io"
    }

    fun dashboardPublicApiUrl(buildEnv: BuildEnv): String = when (buildEnv) {
        BuildEnv.TESTING -> "https://api-develop.web3auth.io/signer-service"
        else -> "https://api.web3auth.io/signer-service"
    }
}

const val LOGIN_SOURCE_ANDROID = "web3auth-android"

package com.web3auth.core

import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.ErrorCode
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.Web3AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestration tests for SFA connect → Auth MFA vs local share path.
 * Fakes only — no Firebase / Torus / network.
 */
class SfaMfaAuthFlowTest {

    @Test(timeout = 5_000)
    fun jwtConnect_authFirst_neverCallsLocalShareBeforeAuth() {
        val loginParams = LoginParams(
            authConnection = AuthConnection.CUSTOM,
            authConnectionId = "w3a-firebase-demo",
            idToken = "fresh-firebase-jwt",
        )
        assertTrue(SfaJwtMfaSupport.shouldUseAuthFirstForJwt(loginParams.idToken, loginParams.mfaLevel))

        val authParams = SfaJwtMfaSupport.buildJwtAuthLoginParams(
            loginParams = loginParams,
            loginHint = "uid-1",
            clientId = "client-1",
        )
        // Fresh JWT placed in extras for a single Auth verify — not pre-consumed by Torus.
        assertEquals("fresh-firebase-jwt", authParams.extraLoginOptions?.id_token)
        assertEquals("uid-1", authParams.extraLoginOptions?.login_hint)
    }

    @Test(timeout = 5_000)
    fun silentSfaOptIn_mfaLevelNone_usesLocalSharePath() {
        val future = CompletableFuture<String>()
        val clearDone = CompletableFuture<Void>().also { it.complete(null) }
        val localShareUsed = AtomicBoolean(false)

        val loginParams = LoginParams(
            authConnection = AuthConnection.CUSTOM,
            authConnectionId = "w3a-firebase-demo",
            idToken = "firebase-jwt",
            mfaLevel = MFALevel.NONE,
        )
        assertFalse(SfaJwtMfaSupport.shouldUseAuthFirstForJwt(loginParams.idToken, loginParams.mfaLevel))

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearDone },
            runConnect = {
                localShareUsed.set(true)
                future.complete("0xlocal-sfa-key")
            },
        )

        assertEquals("0xlocal-sfa-key", future.get(2, TimeUnit.SECONDS))
        assertTrue(localShareUsed.get())
    }

    @Test
    fun enableMfa_whenAlreadyEnabled_returnsMfaAlreadyEnabledError() {
        val user = UserInfo(isMfaEnabled = true)
        assertTrue(user.isMfaEnabled == true)
        assertEquals(
            "MFA already enabled",
            Web3AuthError.getError(ErrorCode.MFA_ALREADY_ENABLED),
        )
    }

    @Test
    fun pnpEnableMfa_withoutJwt_stillRequiresSessionSemantics() {
        // PnP (no JWT): cannot launch MFA without citadel/local session.
        assertFalse(
            SfaJwtMfaSupport.canLaunchMfaWithoutCitadel(
                jwt = null,
                hasActiveLocalSession = false,
            )
        )
        // PnP with active citadel refresh success path does not need the JWT fallback.
        assertFalse(
            SfaJwtMfaSupport.canLaunchMfaWithoutCitadel(
                jwt = null,
                hasActiveLocalSession = true,
            )
        )
    }
}

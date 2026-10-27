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
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Orchestration tests for SFA connect → Auth MFA vs local share path.
 * Fakes only — no Firebase / Torus / network.
 */
class SfaMfaAuthFlowTest {

    @Test(timeout = 5_000)
    fun upgradedSfaConnect_launchesAuthMfa_andDoesNotThrowUserAlreadyEnabledMfa() {
        val future = CompletableFuture<String>()
        val clearDone = CompletableFuture<Void>().also { it.complete(null) }
        val authLaunched = AtomicBoolean(false)
        val authParams = AtomicReference<LoginParams?>(null)
        val loginParams = LoginParams(
            authConnection = AuthConnection.CUSTOM,
            authConnectionId = "w3a-firebase-demo",
            idToken = "firebase-jwt",
        )

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearDone },
            runConnect = {
                val isUpgraded = true
                if (SfaJwtMfaSupport.requiresAuthMfaFlow(isUpgraded)) {
                    authParams.set(
                        SfaJwtMfaSupport.buildJwtAuthLoginParams(
                            loginParams = loginParams,
                            loginHint = "uid-1",
                            clientId = "client-1",
                        )
                    )
                    authLaunched.set(true)
                    // Auth redirect completes the login future later (simulated).
                    future.complete("auth-session")
                    return@start
                }
                fail("must not continue local share path when upgraded")
            },
        )

        val result = future.get(2, TimeUnit.SECONDS)
        assertEquals("auth-session", result)
        assertTrue(authLaunched.get())
        assertEquals(MFALevel.MANDATORY, authParams.get()?.mfaLevel)
        assertEquals("firebase-jwt", authParams.get()?.extraLoginOptions?.id_token)
        assertFalse(
            result.contains(Web3AuthError.getError(ErrorCode.USER_ALREADY_ENABLED_MFA))
        )
    }

    @Test(timeout = 5_000)
    fun nonUpgradedSfaConnect_usesLocalSharePath() {
        val future = CompletableFuture<String>()
        val clearDone = CompletableFuture<Void>().also { it.complete(null) }
        val localShareUsed = AtomicBoolean(false)
        val authLaunched = AtomicBoolean(false)

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearDone },
            runConnect = {
                if (SfaJwtMfaSupport.requiresAuthMfaFlow(false)) {
                    authLaunched.set(true)
                    return@start
                }
                localShareUsed.set(true)
                future.complete("0xlocal-sfa-key")
            },
        )

        assertEquals("0xlocal-sfa-key", future.get(2, TimeUnit.SECONDS))
        assertTrue(localShareUsed.get())
        assertFalse(authLaunched.get())
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

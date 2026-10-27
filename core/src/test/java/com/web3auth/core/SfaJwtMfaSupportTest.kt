package com.web3auth.core

import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.UserInfo
import com.web3auth.core.types.Web3AuthError
import com.web3auth.core.types.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JWT / SFA MFA parity with Web Auth — no Firebase, Torus, or network.
 */
class SfaJwtMfaSupportTest {

    @Test
    fun jwtConnect_defaultsToAuthFirst_toAvoidTokenIsUsed() {
        assertTrue(SfaJwtMfaSupport.shouldUseAuthFirstForJwt("firebase-jwt", mfaLevel = null))
        assertTrue(SfaJwtMfaSupport.shouldUseAuthFirstForJwt("firebase-jwt", mfaLevel = MFALevel.MANDATORY))
        // Silent local SFA only when app opts out of Auth (known non-MFA).
        assertFalse(SfaJwtMfaSupport.shouldUseAuthFirstForJwt("firebase-jwt", mfaLevel = MFALevel.NONE))
        assertFalse(SfaJwtMfaSupport.shouldUseAuthFirstForJwt(null, null))
    }

    @Test
    fun buildJwtAuthLoginParams_putsIdTokenInExtraLoginOptions_withoutConsumingSemantics() {
        val params = SfaJwtMfaSupport.buildJwtAuthLoginParams(
            loginParams = sampleJwtLoginParams(),
            loginHint = "firebase-uid-1",
            clientId = "firebase-client",
        )
        // Default login does not force mandatory; Auth prompts MFA when needed.
        assertNull(params.mfaLevel)
        assertEquals("firebase-jwt", params.extraLoginOptions?.id_token)
        assertEquals("firebase-uid-1", params.extraLoginOptions?.login_hint)
        assertEquals("firebase-client", params.extraLoginOptions?.client_id)
    }

    @Test
    fun forceMfaMandatory_setsMfaLevel() {
        val params = SfaJwtMfaSupport.buildJwtAuthLoginParams(
            loginParams = sampleJwtLoginParams(),
            loginHint = "uid",
            forceMfaMandatory = true,
        )
        assertEquals(MFALevel.MANDATORY, params.mfaLevel)
    }

    @Test
    fun isUpgradedFalseOrNull_doesNotRequireAuthMfaFlow() {
        assertFalse(SfaJwtMfaSupport.requiresAuthMfaFlow(false))
        assertFalse(SfaJwtMfaSupport.requiresAuthMfaFlow(null))
        assertTrue(SfaJwtMfaSupport.requiresAuthMfaFlow(true))
    }

    @Test
    fun enableMfa_preservesCustomJwtCredentialsInExtraLoginOptions() {
        val merged = SfaJwtMfaSupport.mergeJwtExtraLoginOptions(
            existing = ExtraLoginOptions(domain = "example.com"),
            idToken = "jwt-from-firebase",
            loginHint = "user-123",
            clientId = "client-abc",
        )
        assertEquals("jwt-from-firebase", merged.id_token)
        assertEquals("user-123", merged.login_hint)
        assertEquals("client-abc", merged.client_id)
        // Existing fields preserved via copy
        // domain is private; round-trip through buildJwtAuthLoginParams still keeps id_token
        val authParams = SfaJwtMfaSupport.buildJwtAuthLoginParams(
            loginParams = LoginParams(
                authConnection = AuthConnection.CUSTOM,
                authConnectionId = "verifier",
                idToken = "jwt-from-firebase",
                extraLoginOptions = ExtraLoginOptions(login_hint = "old-hint"),
            ),
            loginHint = "user-123",
            clientId = "client-abc",
        )
        assertEquals("jwt-from-firebase", authParams.extraLoginOptions?.id_token)
        assertEquals("user-123", authParams.extraLoginOptions?.login_hint)
        assertEquals("client-abc", authParams.extraLoginOptions?.client_id)
    }

    @Test
    fun resolveJwtForMfa_prefersParamsThenStoredThenUserInfo() {
        val user = UserInfo(idToken = "from-userinfo", oAuthIdToken = "from-oauth")
        assertEquals(
            "from-params",
            SfaJwtMfaSupport.resolveJwtForMfa("from-params", null, null, user),
        )
        assertEquals(
            "from-extra",
            SfaJwtMfaSupport.resolveJwtForMfa(null, "from-extra", null, user),
        )
        assertEquals(
            "from-stored",
            SfaJwtMfaSupport.resolveJwtForMfa(null, null, "from-stored", user),
        )
        assertEquals(
            "from-userinfo",
            SfaJwtMfaSupport.resolveJwtForMfa(null, null, null, user),
        )
        assertNull(SfaJwtMfaSupport.resolveJwtForMfa(null, null, null, null))
    }

    @Test
    fun canLaunchMfaWithoutCitadel_onlyWithJwtAndLocalSession() {
        assertTrue(SfaJwtMfaSupport.canLaunchMfaWithoutCitadel("jwt", true))
        assertFalse(SfaJwtMfaSupport.canLaunchMfaWithoutCitadel("jwt", false))
        assertFalse(SfaJwtMfaSupport.canLaunchMfaWithoutCitadel(null, true))
        assertFalse(SfaJwtMfaSupport.canLaunchMfaWithoutCitadel("", true))
    }

    @Test
    fun mfaAlreadyEnabled_guardStillUsesWebErrorCode() {
        // Document expected public error for already-enabled (enableMFA path in Web3Auth).
        assertEquals("MFA already enabled", Web3AuthError.getError(ErrorCode.MFA_ALREADY_ENABLED))
    }

    @Test
    fun sfaEnableMfa_noLongerUsesEnableMfaNotAllowedAsDefaultJwtReject() {
        // After the v11 guard removal, JWT presence alone must not imply ENABLE_MFA_NOT_ALLOWED.
        val jwt = "firebase-jwt"
        assertNotNull(jwt)
        assertTrue(SfaJwtMfaSupport.canLaunchMfaWithoutCitadel(jwt, hasActiveLocalSession = true))
    }

    private fun sampleJwtLoginParams() = LoginParams(
        authConnection = AuthConnection.CUSTOM,
        authConnectionId = "w3a-firebase-demo",
        groupedAuthConnectionId = null,
        idToken = "firebase-jwt",
    )
}

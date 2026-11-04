package com.web3auth.core

import com.web3auth.core.types.ExtraLoginOptions
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.MFALevel
import com.web3auth.core.types.UserInfo

/**
 * Pure helpers that align Android SFA / custom-JWT MFA with Web Auth (`@web3auth/auth`).
 * Extracted for unit tests without Firebase, Torus, or UI.
 */
internal object SfaJwtMfaSupport {

    /**
     * Torus `metadata.isUpgraded == true` means the user already has MFA factors.
     * Prefer Auth `/start` with a fresh JWT — never reuse a JWT already passed to
     * [retrieveShares] (backend returns "token is used").
     */
    fun requiresAuthMfaFlow(isUpgraded: Boolean?): Boolean = isUpgraded == true

    /**
     * Custom JWT login goes Auth-first (Web `connectWithJwtLogin`) by default.
     * Local Torus retrieval first consumes the JWT so Auth MFA then fails with
     * "token is used". Opt into silent local SFA with [MFALevel.NONE] only when
     * the user is known not to have MFA.
     */
    fun shouldUseAuthFirstForJwt(idToken: String?, mfaLevel: MFALevel? = null): Boolean {
        if (idToken.isNullOrBlank()) return false
        if (mfaLevel == MFALevel.NONE) return false
        return true
    }

    /**
     * Build Auth `/start` login params for custom JWT (Web parity).
     * Puts credentials in [ExtraLoginOptions]; does not force MFA level unless set.
     */
    fun buildJwtAuthLoginParams(
        loginParams: LoginParams,
        loginHint: String?,
        clientId: String? = null,
        forceMfaMandatory: Boolean = false,
    ): LoginParams {
        val idToken = loginParams.idToken
        val hint = loginHint?.takeIf { it.isNotBlank() }
            ?: loginParams.loginHint
            ?: loginParams.extraLoginOptions?.login_hint

        return loginParams.copy(
            mfaLevel = when {
                forceMfaMandatory -> MFALevel.MANDATORY
                else -> loginParams.mfaLevel
            },
            loginHint = hint,
            // Top-level idToken stays for SDK callers; Auth reads id_token from extras.
            extraLoginOptions = mergeJwtExtraLoginOptions(
                existing = loginParams.extraLoginOptions,
                idToken = idToken,
                loginHint = hint,
                clientId = clientId,
            ),
        )
    }

    /**
     * Merge JWT credentials into extraLoginOptions (mirrors Web:
     * `id_token`, `login_hint`, optional `client_id`).
     */
    fun mergeJwtExtraLoginOptions(
        existing: ExtraLoginOptions?,
        idToken: String?,
        loginHint: String?,
        clientId: String? = null,
    ): ExtraLoginOptions {
        val base = existing ?: ExtraLoginOptions()
        return base.copy(
            id_token = idToken?.takeIf { it.isNotBlank() } ?: base.id_token,
            login_hint = loginHint?.takeIf { it.isNotBlank() } ?: base.login_hint,
            client_id = clientId?.takeIf { it.isNotBlank() } ?: base.client_id,
        )
    }

    /**
     * Resolve a JWT for MFA Auth from enable/manage params or the current user.
     *
     * Prefer [UserInfo.oAuthIdToken] over [UserInfo.idToken]: after Auth-first login,
     * `idToken` is often the Web3Auth session JWT, while `oAuthIdToken` holds the
     * provider / Firebase JWT needed in `extraLoginOptions.id_token`.
     */
    fun resolveJwtForMfa(
        paramsIdToken: String?,
        paramsExtraIdToken: String?,
        storedLoginIdToken: String?,
        userInfo: UserInfo?,
        storedLoginExtraIdToken: String? = null,
    ): String? {
        return listOf(
            paramsIdToken,
            paramsExtraIdToken,
            storedLoginIdToken,
            storedLoginExtraIdToken,
            userInfo?.oAuthIdToken,
            // Last resort only — may be a session JWT after Auth rehydrate.
            userInfo?.idToken,
        ).firstOrNull { !it.isNullOrBlank() }
    }

    /**
     * Whether in-memory / storage state still counts as a local session for MFA
     * fallback after citadel refresh fails. Use a **pre-refresh** snapshot —
     * [refreshSession] wipes [Web3AuthResponse] on failure.
     */
    fun hasLocalSessionForMfaFallback(
        storageSessionId: String?,
        privateKey: String?,
        factorKey: String?,
    ): Boolean {
        return !storageSessionId.isNullOrBlank() ||
            !privateKey.isNullOrBlank() ||
            !factorKey.isNullOrBlank()
    }

    /**
     * When citadel refresh fails, JWT users may still open Auth MFA (unlike a true no-session case).
     */
    fun canLaunchMfaWithoutCitadel(jwt: String?, hasActiveLocalSession: Boolean): Boolean {
        return !jwt.isNullOrBlank() && hasActiveLocalSession
    }
}

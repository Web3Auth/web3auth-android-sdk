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
     * Torus `metadata.isUpgraded == true` means the user already has MFA factors —
     * Web opens Auth UI; Android must not throw [ErrorCode.USER_ALREADY_ENABLED_MFA].
     */
    fun requiresAuthMfaFlow(isUpgraded: Boolean?): Boolean = isUpgraded == true

    /**
     * Build Auth `/start` login params for an MFA-upgraded JWT user.
     * Preserves connection ids and JWT credentials in [ExtraLoginOptions].
     */
    fun buildJwtAuthLoginParams(
        loginParams: LoginParams,
        loginHint: String?,
        clientId: String? = null,
    ): LoginParams {
        val idToken = loginParams.idToken
        val hint = loginHint?.takeIf { it.isNotBlank() }
            ?: loginParams.loginHint
            ?: loginParams.extraLoginOptions?.login_hint

        return loginParams.copy(
            mfaLevel = MFALevel.MANDATORY,
            loginHint = hint,
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
     */
    fun resolveJwtForMfa(
        paramsIdToken: String?,
        paramsExtraIdToken: String?,
        storedLoginIdToken: String?,
        userInfo: UserInfo?,
    ): String? {
        return listOf(
            paramsIdToken,
            paramsExtraIdToken,
            storedLoginIdToken,
            userInfo?.idToken,
            userInfo?.oAuthIdToken,
        ).firstOrNull { !it.isNullOrBlank() }
    }

    /**
     * When citadel refresh fails, JWT users may still open Auth MFA (unlike a true no-session case).
     */
    fun canLaunchMfaWithoutCitadel(jwt: String?, hasActiveLocalSession: Boolean): Boolean {
        return !jwt.isNullOrBlank() && hasActiveLocalSession
    }
}

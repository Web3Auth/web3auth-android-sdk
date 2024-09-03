package com.web3auth.core.types

import androidx.credentials.CreateCredentialResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class PasskeyServiceParams(
    val web3authClientId: String,
    val web3authNetwork: Network,
    val buildEnv: BuildEnv,
    val rpID: String,
    val rpName: String
)

data class RegistrationOptionsParams(
    val oAuthVerifier: String,
    val oAuthVerifierId: String,
    val signatures: List<String>,
    val username: String,
    val passkeyToken: String? = null,
    val authenticatorAttachment: AuthenticatorAttachment? = null
)

enum class AuthenticatorAttachment {
    PLATFORM, CROSS_PLATFORM
}

data class RegistrationOptionsData(
    val options: Map<String, Any>,
    val trackingId: String
)

data class RegistrationOptionsRequest(
    val web3auth_client_id: String,
    val verifier_id: String,
    val verifier: String,
    val rp: Rp,
    val username: String,
    val network: String,
    val signatures: List<String>
)

data class RegistrationOptionsResponse(
    val success: Boolean,
    val data: RegistrationOptionsData
)

data class Rp(
    val name: String,
    val id: String
)

data class PasskeyServiceEndpoints(
    val register: RegisterEndpoints,
    val authenticate: AuthenticateEndpoints,
    val crud: CrudEndpoints
)

data class RegisterEndpoints(
    val options: String,
    val verify: String
)

data class AuthenticateEndpoints(
    val options: String,
    val verify: String
)

data class CrudEndpoints(
    val list: String
)

data class RegistrationResponse(
    val success: Boolean,
    val data: Data
)

// Data object containing the registration options and tracking ID
data class Data(
    val options: Options,
    val trackingId: String
)

// Options object containing various registration parameters
data class Options(
    val challenge: String,
    val rp: Rp,
    val user: User,
    val pubKeyCredParams: List<PubKeyCredParam>,
    val timeout: Long,
    val attestation: String,
    val excludeCredentials: List<Any>,
    val authenticatorSelection: AuthenticatorSelection,
    val extensions: Extensions
)

// User object containing user information
data class User(
    val id: String,
    val name: String,
    val displayName: String
)

// PubKeyCredParam object representing public key credential parameters
data class PubKeyCredParam(
    val alg: Int,
    val type: String
)

// AuthenticatorSelection object representing authenticator selection criteria
data class AuthenticatorSelection(
    val userVerification: String,
    val residentKey: String,
    val requireResidentKey: Boolean
)

// Extensions object containing any additional extensions
data class Extensions(
    val credProps: Boolean
)

data class AuthParamsData(
    val rpIdHash: ByteArray,
    val flagsBuf: ByteArray,
    val flags: Flags,
    val counter: Long,
    val counterBuf: ByteArray,
    val aaguid: ByteArray,
    val credID: ByteArray,
    val COSEPublicKey: ByteArray
)

data class Flags(
    val up: Boolean,
    val uv: Boolean,
    val at: Boolean,
    val ed: Boolean,
    val flagsInt: Int
)

data class MetadataInfo(
    val privKey: String,
    val userInfo: UserInfo
)

data class VerifyRequest(
    val web3auth_client_id: String,
    val tracking_id: String,
    val verification_data: CreateCredentialResponse,
    val network: String,
    val signatures: List<String>,
    val metadata: String
)

data class VerifyRegistrationResponse(
    val verified: Boolean,
    val error: String? = null,
    val data: ChallengeData? = null
)

data class ChallengeData(
    val challenge_timestamp: String,
    val credential_public_key: String
)

data class RegistrationResponseJson(
    val rawId: String,
    val authenticatorAttachment: String,
    val type: String,
    val id: String,
    val response: Response,
    val clientExtensionResults: ClientExtensionResults,
)

data class Response(
    @JsonProperty("clientDataJSON")
    val clientDataJson: String,
    val attestationObject: String,
    val transports: List<String>,
    val authenticatorData: String,
    val publicKeyAlgorithm: Long,
    val publicKey: String,
)

data class ClientExtensionResults(
    val credProps: CredProps,
)

data class CredProps(
    val rk: Boolean,
)

data class AuthenticationOptionsRequest(
    val web3auth_client_id: String,
    val rp_id: String,
    val authenticator_id: String?,
    val network: String
)

data class AuthenticationOptionsResponse(
    val success: Boolean,
    val data: AuthenticationOptionsData
)

data class AuthenticationOptionsData(
    val options: AuthOptions,
    val trackingId: String
)

data class AuthOptions(
    val challenge: String,
    val allowCredentials: List<Any?>,
    val timeout: Long,
    val userVerification: String,
    val rpId: String,
)

data class VerifyAuthenticationRequest(
    val web3auth_client_id: String,
    val tracking_id: String,
    val verification_data: RegistrationResponseJson,
    val network: String,
)

data class VerifyAuthenticationResponse(
    val verified: Boolean,
    val data: AuthData? = null,
    val error: String? = null
)

data class AuthData(
    val challenge: String,
    val transports: List<String>,
    val publicKey: String,
    val idToken: String,
    val metadata: String,
    val verifierId: String
)

// Kotlin Data Class for ExtraVerifierParams
data class ExtraVerifierParams(
    val signature: String? = null,
    val clientDataJSON: String,
    val authenticatorData: String,
    val publicKey: String,
    val challenge: String,
    val rpOrigin: String,
    val rpId: String,
    val credId: String
)

data class PassKeyLoginParams(
    val verifier: String,
    val verifierId: String,
    val idToken: String,
    val extraVerifierParams: ExtraVerifierParams
)

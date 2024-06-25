package com.web3auth.core.api

import com.web3auth.core.types.AuthenticationOptionsRequest
import com.web3auth.core.types.AuthenticationOptionsResponse
import com.web3auth.core.types.ProjectConfigResponse
import com.web3auth.core.types.RegistrationOptionsRequest
import com.web3auth.core.types.RegistrationResponse
import com.web3auth.core.types.VerifyAuthenticationRequest
import com.web3auth.core.types.VerifyAuthenticationResponse
import com.web3auth.core.types.VerifyRegistrationResponse
import com.web3auth.core.types.VerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("/api/configuration")
    suspend fun fetchProjectConfig(
        @Query("project_id") project_id: String, @Query("network") network: String,
        @Query("whitelist") whitelist: String = "true"
    ): Response<ProjectConfigResponse>

    @POST("/api/v3/auth/passkey/fast/register/options")
    suspend fun getRegistrationOptions(
        @Body registrationOptionsRequest: RegistrationOptionsRequest,
        @Header("Authorization") token: String
    ): Response<RegistrationResponse>

    @POST("/api/v3/auth/passkey/fast/register/verify")
    suspend fun verifyRegistration(
        @Body verifyRequest: VerifyRequest,
        @Header("Authorization") token: String
    ): Response<VerifyRegistrationResponse>

    @POST("/api/v3/auth/passkey/fast/authenticate/options")
    suspend fun getAuthenticationOptions(@Body request: AuthenticationOptionsRequest)
            : Response<AuthenticationOptionsResponse>

    @POST("/api/v3/auth/passkey/fast/authenticate/verify")
    suspend fun verifyAuthentication(
        @Body request: VerifyAuthenticationRequest
    ): Response<VerifyAuthenticationResponse>

}
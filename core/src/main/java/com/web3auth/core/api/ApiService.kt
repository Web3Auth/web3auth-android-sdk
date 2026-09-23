package com.web3auth.core.api

import com.web3auth.core.types.ProjectConfigResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Relative path (no leading '/') so Retrofit keeps baseUrl path `/signer-service/`.
    @GET("api/v2/configuration")
    suspend fun fetchProjectConfig(
        @Query("project_id") project_id: String,
        @Query("network") network: String,
        @Query("build_env") build_env: String,
        @Query("aa_provider") aa_provider: String? = null,
    ): Response<ProjectConfigResponse>
}

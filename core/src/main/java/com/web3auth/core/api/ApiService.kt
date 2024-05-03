package com.web3auth.core.api

import com.web3auth.core.types.ProjectConfigResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("/api/configuration")
    suspend fun fetchProjectConfig(
        @Query("project_id") project_id: String, @Query("network") network: String,
        @Query("whitelist") whitelist: String = "true"
    ): Response<ProjectConfigResponse>
}
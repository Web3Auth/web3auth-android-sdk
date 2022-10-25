package com.web3auth.core.api

import com.web3auth.core.api.models.LogoutApiRequest
import com.web3auth.core.api.models.StoreApiResponse
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Web3AuthApi {
    @GET("/store/get")
    suspend fun authorizeSession(@Query("key") key: String): Response<StoreApiResponse>

    @POST("/store/set")
    suspend fun logout(@Body logoutApiRequest: LogoutApiRequest): Response<JSONObject>
}
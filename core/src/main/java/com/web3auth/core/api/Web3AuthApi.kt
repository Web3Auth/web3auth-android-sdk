package com.web3auth.core.api
import com.web3auth.core.api.models.StoreApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface Web3AuthApi {
    @GET("/store/get")
    suspend fun authorizeSession(@Query("key") key: String) : Response<StoreApiResponse>
}
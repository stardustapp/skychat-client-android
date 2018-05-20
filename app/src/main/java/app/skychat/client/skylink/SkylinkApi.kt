package app.skychat.client.skylink

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SkylinkApi {
    @GET("/~~export/ping")
    fun ping(): Single<NetResponse>

    @POST("/~~export")
    fun rpc(@Body request: NetRequest): Single<NetResponse>
}
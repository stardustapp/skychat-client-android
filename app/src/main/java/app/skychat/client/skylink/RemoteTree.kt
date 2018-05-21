package app.skychat.client.skylink

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy.UPPER_CAMEL_CASE
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory



class RemoteTree constructor(private val apiUrl: String) {

    private val okHttpClient = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(ObjectMapper()
                    .registerModule(KotlinModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setPropertyNamingStrategy(UPPER_CAMEL_CASE)))
            .build()

    private val skylink = retrofit.create(SkylinkApi::class.java)

    fun doOp(request: NetRequest): NetResponse {
        return skylink.rpc(request)
                .blockingGet()
    }

    fun ping(): Boolean {
        return doOp(NetRequest("ping", null, null, null, null)).ok
    }

    fun invoke(path: String, input: NetEntry?): NetEntry? {
        return doOp(NetRequest(
                "invoke", path, null, input, null)).output
    }
}
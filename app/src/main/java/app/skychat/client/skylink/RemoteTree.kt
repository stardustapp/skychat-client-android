package app.skychat.client.skylink

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy.UPPER_CAMEL_CASE
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory


class RemoteTree constructor(apiUrl: String) {

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

    @Suppress("unused")
    fun ping(): Boolean {
        return doOp(NetRequest("ping", null, null, null, null)).ok
    }

    fun invoke(path: String, input: NetEntry?): NetEntry? {
        return doOp(NetRequest(
                "invoke", path, null, input, null)).output
    }

    fun enumerate(path: String, depth: Int = 1): List<NetEntry> {
        val output = doOp(NetRequest(
                "enumerate", path, null, null, depth)).output

        val outType = output?.type ?: "Empty"
        if (outType == "Folder") {
            return output!!.children!!
        }
        throw Exception("enumerate() on '$path' returned '$outType' instead of Folder")
    }

    fun getStringRx(path: String): Maybe<String> {
        return skylink
                .rpc(NetRequest("get", path, null, null, null))
                .subscribeOn(Schedulers.io())
                .filter { r -> r.ok && r.output?.type == "String" }
                .map { r -> r.output!!.stringValue ?: "" }
    }

    fun invokeRx(path: String, input: NetEntry?): Maybe<NetEntry> {
        return skylink
                .rpc(NetRequest("invoke", path, null, input, null))
                .subscribeOn(Schedulers.io())
                .filter { r -> r.ok && r.output != null }
                .map { x -> x.output }
    }

    fun enumerateRx(path: String, depth: Int = 1): Observable<NetEntry> {
        return skylink
                .rpc(NetRequest("enumerate", path, null, null, depth))
                .subscribeOn(Schedulers.io())
                .filter { r -> r.ok && r.output?.type == "Folder" }
                .flatMapObservable { r -> Observable.fromIterable(r.output?.children) }
    }

}
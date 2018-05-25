package app.skychat.client.utils

import android.os.AsyncTask
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers

abstract class ReactiveAsyncTask<A, B> : AsyncTask<A, Void, B>() {
    fun asMaybe(vararg param: A): Maybe<B> {
        return Maybe.fromCallable({
            doInBackground(*param)
        }).subscribeOn(Schedulers.io())
    }
}
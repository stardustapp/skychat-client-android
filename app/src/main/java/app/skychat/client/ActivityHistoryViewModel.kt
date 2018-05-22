package app.skychat.client

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import app.skychat.client.skylink.RemoteTree
import app.skychat.client.skylink.remoteTreeFor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class ActivityHistoryViewModel constructor(
        app: Application
) : AndroidViewModel(app) {
    private lateinit var domainName: String
    private lateinit var remoteTree: RemoteTree
    private lateinit var logPath: String

    private var isStarted = false
    private val horizonPartSubject = BehaviorSubject.createDefault<String>("")
    private val latestPartSubject = BehaviorSubject.createDefault<String>("")
    private val allHistorySubject = BehaviorSubject.createDefault<List<ActivityEntry>>(emptyList())

    private var latestPartModel: PartitionModel? = null

    fun watchForSnapshots(): Observable<List<ActivityEntry>> {
        return allHistorySubject
                .map { list ->
                    var currentAuthor: String? = null
                    list.forEach { entry ->
                        if (entry.isMessage) {
                            if (currentAuthor == entry.prefixName) {
                                entry.isContinuedMessage = true
                            } else {
                                currentAuthor = entry.prefixName
                                entry.isContinuedMessage = false
                            }
                        } else {
                            currentAuthor = null
                            entry.isContinuedMessage = false
                        }
                    }
                    list
                }
    }

    fun startIfIdle(domainName: String, logPath: String) {
        if (isStarted) return
        isStarted = true

        this.domainName = domainName
        this.remoteTree = remoteTreeFor(domainName)
        this.logPath = logPath

        remoteTree.getStringRx("$logPath/log/horizon")
                .subscribe({ horizon -> horizonPartSubject.onNext(horizon) })
        remoteTree.getStringRx("$logPath/log/latest")
                .subscribe({ latest -> latestPartSubject.onNext(latest) })

        latestPartSubject
                .filter({ x -> !x.isEmpty() })
                .subscribe({ latest ->
                    latestPartModel = PartitionModel(latest)
                })
    }

    inner class PartitionModel(val partId: String) {
        init {
            if (partId.isEmpty())
                throw Exception("PartitionModel was given an empty partition ID")
        }
        val partPath = "$logPath/log/$partId"

        private val horizonSubject = BehaviorSubject.createDefault<String>("")
        private val latestSubject = BehaviorSubject.createDefault<String>("")
        private val entryMaybes = HashMap<Int, Single<ActivityEntry>>()

        init {

            remoteTree.getStringRx("$partPath/horizon")
                    .subscribe({ horizon -> horizonSubject.onNext(horizon) })
            remoteTree.getStringRx("$partPath/latest")
                    .subscribe({ latest -> latestSubject.onNext(latest) })
            Observable.interval(2, TimeUnit.SECONDS)
                    .forEach {
                        remoteTree.getStringRx("$partPath/latest")
                                .subscribe({ latest -> latestSubject.onNext(latest) })
                    }
            horizonSubject
                    .filter({ x -> !x.isEmpty() })
                    .firstElement()
                    .flatMapObservable { horizon ->
                        latestSubject
                                .filter({ x -> !x.isEmpty() })
                                .map { latest ->
                                    IdRange(horizon, latest)
                                }
                                .distinctUntilChanged()
                    }
                    //.firstElement()
                    //.observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ range ->
                        var firstId = range.horizon.toInt()
                        var lastId = range.latest.toInt()
                        if (firstId < lastId - 15) {
                            firstId = lastId - 15
                        }
                        for (i in firstId..lastId) {
                            entryMaybes[i] = entryMaybes[i] ?: remoteTree
                                    .enumerateRx("$partPath/$i", 2)
                                    .filter({ e -> e.type == "String" })
                                    .collectInto(HashMap<String, String>(), { map, entry ->
                                        map[entry.name] = entry.stringValue ?: ""
                                    })
                                    .map { props ->
                                        ActivityEntry(i, "$partPath/$i", props)
                                    }
                        }
                        Observable.fromIterable(entryMaybes.values)
                                .flatMapSingle { x -> x }
                                .observeOn(AndroidSchedulers.mainThread())
                                .toSortedList { o1, o2 -> o1.idx - o2.idx }
                                .subscribe({ all ->
                                    allHistorySubject.onNext(all)
                                })
                    })
        }
    }

    data class IdRange(val horizon: String, val latest: String)
}
package app.skychat.client.utils

@Suppress("unused")
object Stopwatch {
    inline fun elapse(callback: () -> Unit): Long {
        val start = System.currentTimeMillis()
        callback()
        return System.currentTimeMillis() - start
    }

    inline fun elapseNano(callback: () -> Unit): Long {
        val start = System.nanoTime()
        callback()
        return System.nanoTime() - start
    }
}
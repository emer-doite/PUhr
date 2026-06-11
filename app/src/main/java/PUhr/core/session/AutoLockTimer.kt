package PUhr.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoLockTimer @Inject constructor() {

    @Volatile
    private var timerJob: Job? = null

    fun start(durationSeconds: Int, onLock: () -> Unit) {
        cancel()
        timerJob = CoroutineScope(Job()).launch {
            delay(durationSeconds * 1000L)
            onLock()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }
}

package com.opengamer.sharedupdatertest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedCacheUpdater<Result : Any>(
    private val updateScope: CoroutineScope,
    private val updater: suspend () -> Result,
) {

    private val sharedMutex = Mutex()

    private val shared = MutableSharedFlow<Result>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND,
    ).apply {
        updateScope.launch {
            subscriptionCount
                .collectLatest {
                    println("subscribers $it")
                    if (it > 0) {
                        ensureActiveUpdate()
                    } else {
                        updaterJob = null
                    }
                }
        }
    }

    @Volatile
    private var updaterJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    suspend fun getShared(): Result = shared.first()

    private suspend fun ensureActiveUpdate() {
        println("ensureActiveUpdate")
        sharedMutex.withLock {
            if (updaterJob == null || updaterJob?.isActive == false) {
                startUpdate()
            }
        }
    }

    private fun startUpdate() {
        println("startUpdate")
        updaterJob = updateScope.launch {
            val newValue = updater.invoke()
            sharedMutex.withLock {
                shared.emit(newValue)
                updaterJob = null
            }
        }
    }
}

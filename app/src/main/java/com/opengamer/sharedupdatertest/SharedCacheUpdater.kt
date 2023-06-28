package com.opengamer.sharedupdatertest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SharedCacheUpdater<Result : Any>(
    private val updateScope: CoroutineScope,
    private val updater: suspend () -> Result,
) {

    // TODO: replace by kotlinx.atomicfu.locks.ReentrantLock
    private val reentrantLock = ReentrantLock()

    private val shared = MutableSharedFlow<Result>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND,
    ).apply {
        var prevSubscriberCount = 0
        updateScope.launch {
            subscriptionCount
                .collectLatest {currentSubscribersCount ->
                    if (currentSubscribersCount > 0) {
                        // start job only if count of somebody subscribe
                        if (currentSubscribersCount > prevSubscriberCount) {
                            ensureActiveUpdate()
                        }
                    } else {
                        updaterJob = null
                    }
                    prevSubscriberCount = currentSubscribersCount
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

    private fun ensureActiveUpdate() {
        println("ensureActiveUpdate")
        reentrantLock.withLock {
            if (updaterJob == null || updaterJob?.isActive == false) {
                startUpdate()
            }
        }
    }

    private fun startUpdate() {
        println("startUpdate")
        updaterJob = updateScope.launch {
            val newValue = updater.invoke()
            reentrantLock.withLock {
                updateScope.launch {
                    shared.emit(newValue)
                }
                updaterJob = null
            }
        }
    }
}

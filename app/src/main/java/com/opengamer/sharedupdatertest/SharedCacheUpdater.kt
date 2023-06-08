package com.opengamer.sharedupdatertest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.atomic.AtomicReference

class SharedCacheUpdater<Result : Any>(
    private val updateScope: CoroutineScope,
    private val updater: suspend () -> Result,
) {

    private val sharedUpdater: AtomicReference<Flow<Result>?> = AtomicReference(null)

    suspend fun getShared(): Result = (
        sharedUpdater.get() ?: sharedUpdater.updateAndGet {
            if (it != null) return@updateAndGet it
            createShared()
        }!!
        ).first()

    private fun createShared(): Flow<Result> = flow<Result> {
        emit(updater.invoke())
    }.shareIn(
        scope = updateScope,
        started = SharingStarted.WhileSubscribed(),
    )
        .onCompletion {
            sharedUpdater.set(null)
        }
}

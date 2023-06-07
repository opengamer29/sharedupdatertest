package com.opengamer.sharedupdatertest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

class SharedCacheUpdater<Result : Any>(
    updateScope: CoroutineScope,
    private val updater: suspend () -> Result,
) {

    private val value = flow<Result> {
        emit(updater.invoke())
    }.shareIn(
        scope = updateScope,
        started = SharingStarted.WhileSubscribed(
            0,
            0,
        ),
    )

    suspend fun getShared(): Result = value.first()
}

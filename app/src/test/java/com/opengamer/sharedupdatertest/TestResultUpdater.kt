package com.opengamer.sharedupdatertest

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

class TestResultUpdater<Result>(
    private val valueProvider: suspend (Int) -> Result,
) :
    suspend () -> Result {

    companion object {

        fun delayImplementation(value: Int, delayMillis: Long) = TestResultUpdater { _ ->
            delay(delayMillis)
            value
        }
    }

    val totalStarts: Int
        get() = totalStartsAtomic.get()

    private val totalStartsAtomic: AtomicInteger = AtomicInteger(0)

    val totalUpdates: Int
        get() = totalUpdatesAtomic.get()

    private val totalUpdatesAtomic: AtomicInteger = AtomicInteger(0)

    override suspend fun invoke(): Result {
        val value = valueProvider(totalStartsAtomic.incrementAndGet())
        totalUpdatesAtomic.incrementAndGet()
        return value
    }
}

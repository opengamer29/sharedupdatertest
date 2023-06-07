package com.opengamer.sharedupdatertest

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus

private const val UpdaterResult = 1
private const val UpdaterDelay = 50L

@Suppress("InjectDispatcher")
class SharedCacheUpdaterTest : BehaviorSpec() {

    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerTest

    init {
        Given("Shared test updater with small delay") {
            val testUpdater = TestResultUpdater.delayImplementation(UpdaterResult, UpdaterDelay)
            val sharedCacheUpdater = SharedCacheUpdater<Int>(
                updateScope = MainScope() + newSingleThreadContext("updater context"),
                updater = testUpdater,
            )

            When("Request two values simultaneously") {
                val firstValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                val secondValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                val firstValue = firstValueDef.await()
                val secondValue = secondValueDef.await()

                Then("First value is Updater value") {
                    firstValue shouldBe UpdaterResult
                }

                Then("Second value is Updater value") {
                    secondValue shouldBe UpdaterResult
                }

                Then("Updater was called once") {
                    testUpdater.totalStarts shouldBe 1
                }
            }

            When("Request two values simultaneously but canceled") {
                val firstValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                val secondValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                delay(10)

                firstValueDef.cancel()
                secondValueDef.cancel()

                delay(UpdaterDelay * 2)

                Then("Updater was started") {
                    testUpdater.totalStarts shouldBe 1
                }

                Then("Updater wasn't finished") {
                    testUpdater.totalUpdates shouldBe 0
                }
            }

            When("Request two values with delay more that updater delay") {
                val firstValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                delay(UpdaterDelay * 2)

                val secondValueDef = async(Dispatchers.IO) {
                    sharedCacheUpdater.getShared()
                }

                val firstValue = firstValueDef.await()
                val secondValue = secondValueDef.await()

                Then("First value is Updater value") {
                    firstValue shouldBe UpdaterResult
                }

                Then("Second value is Updater value") {
                    secondValue shouldBe UpdaterResult
                }

                Then("Updater was called twice") {
                    testUpdater.totalStarts shouldBe 2
                }
            }

            // concurrency test, main purpose is to check that there are no deadlocks
            When("Request two values with delay equals to updater delay") {
                repeat(100) {
                    val firstValueDef = async(Dispatchers.IO) {
                        val value = sharedCacheUpdater.getShared()
                        value
                    }

                    delay(UpdaterDelay)

                    val secondValueDef = async(Dispatchers.IO) {
                        println("$it Second get ${Thread.currentThread().name} ${System.currentTimeMillis()}")
                        val value = sharedCacheUpdater.getShared()
                        value
                    }

                    val firstValue = firstValueDef.await()
                    val secondValue = secondValueDef.await()

                    firstValue shouldBe UpdaterResult
                    secondValue shouldBe UpdaterResult

                    // don't check testUpdater.totalStarts here it can be either 1 or 2
                }
            }
        }
    }
}

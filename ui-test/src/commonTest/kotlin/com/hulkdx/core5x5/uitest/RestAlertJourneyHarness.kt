package com.hulkdx.core5x5.uitest

import com.hulkdx.core5x5.app.AppLifecycleHandler
import com.hulkdx.core5x5.app.RestAlertExpiryHandler
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.data.RestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.RestAlertState
import com.hulkdx.core5x5.core.restalerts.data.RestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.feature.workout.domain.RestAlertCoordinator
import com.hulkdx.core5x5.feature.workout.domain.RestAlertObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.dsl.module

/**
 * Runs production observation, coordination and expiry against real workout/preferences storage.
 * Only native registration/presentation and the alert ledger are simulated; this is not an OS test.
 */
class RestAlertJourneyHarness(private val nowEpochMillis: () -> Long) {
    private val state = MutableStateFlow<RestAlertState?>(null)
    val pending = MutableStateFlow<RestAlertRequest?>(null)
    val delivered = MutableStateFlow<List<RestAlertRequest>>(emptyList())
    val submissionAttempts = MutableStateFlow(0)
    var rejectSubmissions = false

    val module = module {
        single { RestAlertCoordinator(get(), nowEpochMillis) }
        single { RestAlertObserver(get(), get(), get(), get(), get()) }
        single {
            PersistedRestAlertScheduler(
                stateStore = object : RestAlertStateStore {
                    override suspend fun read() = state.value
                    override suspend fun write(state: RestAlertState) { this@RestAlertJourneyHarness.state.value = state }
                },
                platform = object : RestAlertPlatform {
                    override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) {
                        submissionAttempts.update { it + 1 }
                        check(!rejectSubmissions) { "Injected native alert submission failure" }
                        pending.value = request
                    }
                    override suspend fun cancel(id: RestAlertId) {
                        pending.update { it?.takeUnless { request -> request.id == id } }
                    }
                    override suspend fun cancelNotification() = Unit
                },
                nowEpochMillis = nowEpochMillis,
            )
        }
        single<RestAlertScheduler> { get<PersistedRestAlertScheduler>() }
        single { RestAlertExpiryHandler(get(), get(), get(), get(), get()) }
    }

    fun resume(koin: Koin) = runBlocking { koin.get<AppLifecycleHandler>().onResume() }

    fun expire(koin: Koin, id: RestAlertId) = runBlocking {
        koin.get<RestAlertExpiryHandler>().handle(id) { request ->
            pending.value = null
            delivered.update { it + request }
        }
    }
}

fun runRestAlertUiTest(test: AppUiTest.(RestAlertJourneyHarness) -> Unit) {
    lateinit var alerts: RestAlertJourneyHarness
    runAppUiTest(prepare = { alerts = enableRestAlertVerification() }) { test(alerts) }
}

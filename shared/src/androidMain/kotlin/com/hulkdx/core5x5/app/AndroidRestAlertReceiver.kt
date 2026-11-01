package com.hulkdx.core5x5.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertIntent
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AndroidRestAlertReceiver : BroadcastReceiver(), KoinComponent {
    private val applicationScope: CoroutineScope by inject()
    private val handler: RestAlertExpiryHandler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val id = AndroidRestAlertIntent.decode(intent) ?: return
        val scope = applicationScope
        val pendingResult = goAsync()
        launchAndroidRestAlertReceiverWork(
            scope = scope,
            work = {
                handler.handle(
                    id = id,
                    present = { request ->
                        AndroidRestAlertNotification.show(context, request.soundEnabled)
                    }
                )
            },
            onFailure = { error ->
                Logger.warn("RestAlerts", "Could not deliver rest notification", error)
            },
            onFinished = pendingResult::finish,
        )
    }
}

/** Owns the async receiver boundary so failures and pre-start cancellation still finish goAsync. */
fun launchAndroidRestAlertReceiverWork(
    scope: CoroutineScope,
    work: suspend () -> Unit,
    onFailure: (Throwable) -> Unit,
    onFinished: () -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Job {
    val job = scope.launch(dispatcher) {
        runCatchingSuspend(work).onFailure(onFailure)
    }
    job.invokeOnCompletion {
        onFinished()
    }
    return job
}

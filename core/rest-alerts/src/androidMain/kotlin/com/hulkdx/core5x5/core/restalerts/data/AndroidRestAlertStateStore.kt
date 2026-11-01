package com.hulkdx.core5x5.core.restalerts.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stores native delivery bookkeeping separately from workout data and training preferences. */
class AndroidRestAlertStateStore(context: Context) : RestAlertStateStore {
    private val preferences = context.getSharedPreferences("rest_alerts", Context.MODE_PRIVATE)

    override suspend fun read(): RestAlertState? = withContext(Dispatchers.IO) {
        RestAlertState.decode(preferences.getString(KEY, null))
    }

    override suspend fun write(state: RestAlertState) {
        withContext(Dispatchers.IO) {
            check(preferences.edit().putString(KEY, state.encode()).commit()) {
                "Unable to save rest alert state"
            }
        }
    }

    private companion object {
        const val KEY = "current_alert"
    }
}

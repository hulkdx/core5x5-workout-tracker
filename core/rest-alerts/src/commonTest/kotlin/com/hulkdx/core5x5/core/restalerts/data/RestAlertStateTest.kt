package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class RestAlertStateTest {
    @Test
    fun preservesTheExistingFormatAndFullWidthIdentitiesForEveryStatus() {
        for (status in RestAlertState.Status.entries) {
            for (deadline in listOf(Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE)) {
                val state = RestAlertState(RestAlertId(Long.MAX_VALUE, deadline), status)
                assertEquals("${Long.MAX_VALUE}:$deadline:${status.name}", state.encode())
                assertEquals(state, RestAlertState.decode(state.encode()))
            }
        }
        assertEquals(
            RestAlertState(RestAlertId(1, 1234), RestAlertState.Status.ARMED),
            RestAlertState.decode("1:1234:ARMED"),
        )
    }

    @Test
    fun absentAndMalformedRecordsAreNotEligibleAlerts() {
        for (value in listOf(
            null, "", "1:2", "1:2:ARMED:extra", "0:2:ARMED", "-1:2:ARMED",
            "9223372036854775808:2:ARMED", "1:9223372036854775808:ARMED",
            "session:2:ARMED", "1:deadline:ARMED", "1:2:armed", "1:2:UNKNOWN",
        )) {
            assertNull(RestAlertState.decode(value), value)
        }
        assertFailsWith<IllegalArgumentException> { RestAlertId(0, 1) }
    }
}

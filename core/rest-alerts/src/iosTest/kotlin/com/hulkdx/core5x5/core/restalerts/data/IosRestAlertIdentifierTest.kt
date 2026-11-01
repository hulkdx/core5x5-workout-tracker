package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosRestAlertIdentifierTest {
    @Test
    fun nativeIdentifiersPreserveFullWidthSessionAndDeadlineValues() {
        for (deadline in listOf(Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE)) {
            val id = RestAlertId(Long.MAX_VALUE, deadline)
            val identifier = IosRestAlertIdentifier.encode(id)
            assertEquals("core5x5.rest.${Long.MAX_VALUE}.$deadline", identifier)
            assertTrue(IosRestAlertIdentifier.owns(identifier))
            assertEquals(id, IosRestAlertIdentifier.decode(identifier))
        }
    }

    @Test
    fun rejectsForeignAndMalformedIdentifiersButRecognizesOwnedOrphansForCleanup() {
        assertFalse(IosRestAlertIdentifier.owns("foreign.rest.1.2"))
        assertTrue(IosRestAlertIdentifier.owns("core5x5.rest.malformed"))
        for (value in listOf(
            "", "foreign.rest.1.2", "core5x5.rest.1", "core5x5.rest.1.2.extra",
            "core5x5.rest.0.2", "core5x5.rest.-1.2", "core5x5.rest.session.2",
            "core5x5.rest.1.deadline", "core5x5.rest.9223372036854775808.2",
            "core5x5.rest.1.9223372036854775808",
        )) {
            assertNull(IosRestAlertIdentifier.decode(value), value)
        }
    }
}

package com.hulkdx.core5x5.core.common

import kotlin.test.Test

class LoggerIosTest {
    @Test
    fun warningAcceptsUnicodePercentCharactersAndThrowableDetailsWithoutCrashing() {
        Logger.warn("RestAlerts 100%", "Échec: %@ %s 你好", IllegalStateException("disk 50% full"))
    }
}

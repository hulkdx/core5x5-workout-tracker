@file:Suppress("CAST_NEVER_SUCCEEDS")

package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosRestAlertStateStoreTest {
    @Test
    fun nativeFileSurvivesStoreRecreationForEveryStatusAndFullWidthIdentity() = runTest {
        withStateFile { path ->
            assertNull(IosRestAlertStateStore(path).read())
            for (status in RestAlertState.Status.entries) {
                val state = RestAlertState(RestAlertId(Long.MAX_VALUE, Long.MIN_VALUE), status)
                IosRestAlertStateStore(path).write(state)
                assertEquals(state, IosRestAlertStateStore(path).read())
            }
        }
    }

    @Test
    fun malformedAndEmptyFilesReadAsAbsent() = runTest {
        withStateFile { path ->
            for (value in listOf("", "malformed", "0:2:ARMED", "1:2:unknown")) {
                assertTrue((value as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null))
                assertNull(IosRestAlertStateStore(path).read())
            }
        }
    }

    @Test
    fun nativeReadAndAtomicWriteFailuresAreErrorsInsteadOfMissingStateOrSuccess() = runTest {
        withStateFile { path ->
            assertTrue(NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null))
            assertFailsWith<IllegalStateException> { IosRestAlertStateStore(path).read() }
            assertFailsWith<IllegalStateException> {
                IosRestAlertStateStore(path).write(RestAlertState(RestAlertId(7, 2_000), RestAlertState.Status.ARMED))
            }
        }
    }

    private suspend fun withStateFile(test: suspend (String) -> Unit) {
        val directory = "${NSTemporaryDirectory()}/core5x5-rest-alert-${NSUUID().UUIDString}"
        assertTrue(NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null))
        try {
            test("$directory/state")
        } finally {
            assertTrue(NSFileManager.defaultManager.removeItemAtPath(directory, error = null))
        }
    }
}

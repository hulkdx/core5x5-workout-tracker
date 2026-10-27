package com.hulkdx.core5x5.core.restalerts.data

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSData
import platform.Foundation.NSDataWritingAtomic
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

/** Native file IO only; the shared state owns its format and the scheduler owns transitions. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosRestAlertStateStore(private val stateFilePath: String? = null) : RestAlertStateStore {
    override suspend fun read(): RestAlertState? = withContext(Dispatchers.IO) {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            error.value = null
            val data = NSData.dataWithContentsOfFile(filePath(), options = 0u, error = error.ptr)
            if (data == null) {
                if (error.value?.domain == NSCocoaErrorDomain && error.value?.code == NSFileReadNoSuchFileError) {
                    return@memScoped null
                }
                throw IllegalStateException("Unable to read rest alert state: ${error.value?.localizedDescription}")
            }
            check(data.length <= Int.MAX_VALUE.toULong()) { "Rest alert state is too large" }
            val value = if (data.length == 0uL) "" else {
                checkNotNull(data.bytes).reinterpret<ByteVar>().readBytes(data.length.toInt()).decodeToString()
            }
            RestAlertState.decode(value)
        }
    }

    override suspend fun write(state: RestAlertState) {
        withContext(Dispatchers.IO) {
            val bytes = state.encode().encodeToByteArray()
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                check(data.writeToFile(filePath(), options = NSDataWritingAtomic, error = error.ptr)) {
                    "Unable to save rest alert state: ${error.value?.localizedDescription}"
                }
            }
        }
    }

    private fun filePath(): String = memScoped {
        // Native IO fixtures supply an isolated file; production resolves Application Support on IO.
        stateFilePath?.let { return@memScoped it }
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = error.ptr,
        )
        val path = checkNotNull(directory?.path) {
            "Unable to locate rest alert state: ${error.value?.localizedDescription}"
        }
        "$path/rest-alert-state"
    }
}

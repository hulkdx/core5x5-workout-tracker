package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSinceNow
import platform.Foundation.timeZoneForSecondsFromGMT
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger

/** Native operations for requests accepted by PersistedRestAlertScheduler. */
@OptIn(ExperimentalForeignApi::class)
class IosRestAlertPlatform(
    private val notificationCenter: IosRestAlertNotificationCenter = IosUserNotificationCenterAdapter(),
) : RestAlertPlatform {
    override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) {
        val identifier = IosRestAlertIdentifier.encode(request.id)
        if (wasArmed) {
            val pending = notificationCenter.pendingRequests().firstOrNull { it.identifier == identifier }
            if (pending != null && (pending.content.sound != null) == request.soundEnabled) return
            // A saved attempt may never have reached iOS. Recover it while its target is future.
        }
        val nativeTrigger = trigger(request.id.deadlineEpochMillis)
        // Recovery and sound changes must not create an immediate replay of an earlier attempt.
        if (wasArmed && nativeTrigger == null) {
            notificationCenter.removePendingNotificationRequests(listOf(identifier))
            notificationCenter.removeDeliveredNotifications(listOf(identifier))
            return
        }
        val content = UNMutableNotificationContent().apply {
            setTitle("Set complete")
            setBody("Ready for your next set.")
            setSound(if (request.soundEnabled) UNNotificationSound.defaultSound else null)
        }
        val nativeRequest = UNNotificationRequest.requestWithIdentifier(
            identifier,
            content,
            nativeTrigger,
        )
        notificationCenter.addNotificationRequest(nativeRequest)
    }

    override suspend fun cancel(id: RestAlertId) {
        notificationCenter.removePendingNotificationRequests(listOf(IosRestAlertIdentifier.encode(id)))
    }

    override suspend fun cancelNotification() {
        // The state file may be missing or malformed; remove owned orphan requests too.
        val pendingIdentifiers = notificationCenter.pendingRequests()
            .map { it.identifier }
            .filter(IosRestAlertIdentifier::owns)
        if (pendingIdentifiers.isNotEmpty()) {
            notificationCenter.removePendingNotificationRequests(pendingIdentifiers)
        }
        val identifiers = notificationCenter.deliveredNotificationIdentifiers()
            .filter(IosRestAlertIdentifier::owns)
        if (identifiers.isNotEmpty()) {
            notificationCenter.removeDeliveredNotifications(identifiers)
        }
    }

    private fun trigger(deadlineEpochMillis: Long): UNNotificationTrigger? {
        // Round in integer arithmetic so a large saved deadline cannot round down in Double.
        val roundedSeconds = deadlineEpochMillis / 1000 + if (deadlineEpochMillis % 1000 > 0) 1 else 0
        val interval = roundedSeconds.toDouble()
        check(interval.toLong() == roundedSeconds) { "Unable to represent rest alert deadline" }
        val date = NSDate.dateWithTimeIntervalSince1970(interval)
        val calendar = checkNotNull(NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian))
        calendar.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
            fromDate = date,
        )
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        if (trigger.nextTriggerDate() != null) return trigger

        // The shared scheduler can accept a future rest that becomes due during persistence.
        // A null native trigger delivers that accepted request immediately instead of dropping it.
        check(date.timeIntervalSinceNow <= 0.0) { "Unable to represent rest alert deadline" }
        return null
    }
}

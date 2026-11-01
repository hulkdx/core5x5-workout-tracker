package com.hulkdx.core5x5.core.restalerts.domain

/**
 * Owns one current native alert. Durable delivery bookkeeping stays private to the implementation,
 * outside the workout database.
 * Operations are idempotent. Failures propagate independently of workout persistence.
 */
interface RestAlertScheduler {
    /**
     * Arms a still-future rest, replacing the previous request. Repeating a request or changing
     * its policies must not duplicate delivery or replay an identity already handled.
     * Replacement removes obsolete pending and delivered alerts; callers need not cancel first.
     * Use current native timing access even when the request data is unchanged.
     * The owner reconciles the saved rest, policies and notification permission before scheduling.
     * Expired-rest recovery cancels instead of scheduling a catch-up alert.
     * App-executed expiry callbacks must revalidate these inputs before presenting an alert;
     * OS-controlled delivery remains subject to system permissions and delivery constraints.
     * Native presentation follows the platform's notification behavior and the request's sound policy.
     */
    suspend fun schedule(request: RestAlertRequest)

    /** Removes pending and delivered alerts, allowing a still-future rest to be re-enabled. */
    suspend fun cancel()
}

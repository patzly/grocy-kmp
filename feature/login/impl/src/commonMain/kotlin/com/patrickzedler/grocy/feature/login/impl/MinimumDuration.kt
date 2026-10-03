package com.patrickzedler.grocy.feature.login.impl

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** Long enough for the eye to register a loading indicator, short enough not to feel slow. */
internal val MINIMUM_LOADING_DURATION = 500.milliseconds

/**
 * Runs [block] and suspends until at least [minimum] has passed, whether [block] succeeds or
 * throws. Cancellation is not delayed.
 */
internal suspend inline fun <T> withMinimumDuration(
    minimum: Duration,
    block: () -> T,
): T {
    val start = TimeSource.Monotonic.markNow()
    try {
        return block()
    } finally {
        val remaining = minimum - start.elapsedNow()
        if (remaining.isPositive()) {
            delay(remaining)
        }
    }
}
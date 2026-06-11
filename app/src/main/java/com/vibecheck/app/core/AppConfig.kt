package com.vibecheck.app.core

/**
 * Product-level constants from the Statement of Work. Anything legal/policy
 * sensitive is centralised here so a decision change is a one-line edit.
 */
object AppConfig {
    /** Minimum age to use the app at all (SOW: 16+). */
    const val MIN_AGE_YEARS = 16

    /**
     * Whether anonymous peer chat additionally requires the 18+ age bracket.
     * SOW v1.0 says 16+, but UK Online Safety Act review may force 18+.
     * Flipping this to true is the only change needed client-side.
     */
    const val CHAT_REQUIRES_ADULT = false

    const val CHAT_DURATION_MINUTES = 5
    const val MATCH_TIMEOUT_SECONDS = 30
    const val MATCH_MOOD_WINDOW_HOURS = 2
    const val INACTIVITY_DELETE_DAYS = 90
    const val MAX_NOTE_WORDS = 5
    const val HISTORY_DAYS = 90

    /** Google Play Billing product id for the monthly subscription. */
    const val SUBSCRIPTION_PRODUCT_ID = "vibecheck_plus_monthly"
}

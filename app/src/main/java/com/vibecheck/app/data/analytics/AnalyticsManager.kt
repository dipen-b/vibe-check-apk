package com.vibecheck.app.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.vibecheck.app.core.model.AnalyticsEvent
import android.os.Bundle

/**
 * Firebase Analytics integration for freemium funnel tracking.
 * Tracks user engagement, monetization, and churn signals.
 */
class AnalyticsManager {
    private val analytics: FirebaseAnalytics = Firebase.analytics

    fun logEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.CheckinSubmitted -> {
                analytics.logEvent("checkin_submitted", Bundle().apply {
                    putString("mood", event.mood)
                    putLong("streak_length", event.streakLength)
                })
            }

            is AnalyticsEvent.MatchFound -> {
                analytics.logEvent("match_found", Bundle().apply {
                    putString("peer_mood", event.peerMood)
                    putLong("duration_seconds", event.matchDurationSeconds)
                })
            }

            is AnalyticsEvent.MatchEnded -> {
                analytics.logEvent("match_ended", Bundle().apply {
                    putBoolean("was_continued", event.wasContinued)
                    putBoolean("did_rematch", event.didReMatch)
                })
            }

            is AnalyticsEvent.ProTrialStarted -> {
                analytics.logEvent("pro_trial_started", Bundle().apply {
                    putString("source", event.source)
                })
            }

            is AnalyticsEvent.ProTrialExpired -> {
                analytics.logEvent("pro_trial_expired", Bundle().apply {
                    putBoolean("did_convert", event.didConvert)
                })
            }

            is AnalyticsEvent.PurchaseInitiated -> {
                analytics.logEvent("ecommerce_purchase_init", Bundle().apply {
                    putString("product_id", event.productId)
                    putString("price", event.price)
                })
            }

            is AnalyticsEvent.PurchaseCompleted -> {
                analytics.logEvent("purchase", Bundle().apply {
                    putString("product_id", event.productId)
                    putString("price", event.price)
                })
            }

            is AnalyticsEvent.StreakShieldUsed -> {
                analytics.logEvent("streak_shield_used", Bundle().apply {
                    putLong("streak_length", event.streakLength)
                })
            }

            is AnalyticsEvent.PotionPurchased -> {
                analytics.logEvent("potion_purchased", Bundle().apply {
                    putString("potion_type", event.potionType)
                    putDouble("price", event.price)
                })
            }

            is AnalyticsEvent.UpgradePromptShown -> {
                analytics.logEvent("upgrade_prompt_shown", Bundle().apply {
                    putString("trigger", event.trigger)
                })
            }

            is AnalyticsEvent.UpgradePromptClicked -> {
                analytics.logEvent("upgrade_prompt_clicked", Bundle().apply {
                    putString("trigger", event.trigger)
                    putBoolean("did_convert", event.didConvert)
                })
            }

            is AnalyticsEvent.StreakLost -> {
                analytics.logEvent("streak_lost", Bundle().apply {
                    putLong("streak_length", event.streakLength)
                })
            }
        }
    }

    fun setUserProperty(key: String, value: String) {
        analytics.setUserProperty(key, value)
    }

    fun setUserProperties(proSubscriber: Boolean, streakLength: Long, totalCheckins: Long) {
        analytics.setUserProperty("is_pro_subscriber", proSubscriber.toString())
        analytics.setUserProperty("current_streak", streakLength.toString())
        analytics.setUserProperty("total_checkins", totalCheckins.toString())
    }
}

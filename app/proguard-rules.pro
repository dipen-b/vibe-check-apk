# Firestore deserializes model classes via reflection.
-keepclassmembers class com.vibecheck.app.core.model.** { *; }
-keepclassmembers class com.vibecheck.app.data.remote.dto.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

package com.vibecheck.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.vibecheck.app.BuildConfig

/**
 * Single place that hands out Firebase services, pointing them at the
 * Local Emulator Suite in dev builds (USE_FIREBASE_EMULATOR=true).
 * 10.0.2.2 is the host machine as seen from the Android emulator.
 */
object FirebaseProvider {

    private const val EMULATOR_HOST = "10.0.2.2"

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().also {
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                runCatching { it.useEmulator(EMULATOR_HOST, 9099) }
            }
        }
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().also {
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                runCatching { it.useEmulator(EMULATOR_HOST, 8080) }
            }
        }
    }

    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance().also {
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                runCatching { it.useEmulator(EMULATOR_HOST, 5001) }
            }
        }
    }
}

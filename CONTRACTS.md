# VibeCheck — Architecture Contracts

Read this fully before writing any code. It defines the boundaries every
feature must respect so parallel work composes into one consistent app.

## Project facts

- Root: `/Users/qa/eclipse-workspace/vibecheck`
- Package: `com.vibecheck.app` (sources under `app/src/main/java/com/vibecheck/app/`)
- Kotlin 2.1 + Jetpack Compose (Material 3), min SDK 26, compile/target SDK 35
- Build: `cd /Users/qa/eclipse-workspace/vibecheck && ./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- No DI framework. `VibeCheckApp.container: AppContainer` is the composition root.

## Hard rules

1. **Do not modify** files outside the paths assigned to you, except where your
   task explicitly says so. Never touch: `gradle/libs.versions.toml`,
   `app/build.gradle.kts`, `settings.gradle.kts`, `core/model/*`,
   `data/Repositories.kt`, `data/AppContainer.kt`, `ui/navigation/*`,
   `ui/home/HomeScaffold.kt`, `res/values/*`.
2. **Preserve stub signatures exactly.** Each screen file contains a stub
   composable; replace the file's body but keep the public composable's
   name, parameters, and package identical — `AppNavHost`/`HomeScaffold`
   already call it.
3. **No new dependencies.** Everything you need is already in
   `gradle/libs.versions.toml` and applied in `app/build.gradle.kts`
   (Compose M3 + extended icons, navigation, Room+KSP, DataStore, WorkManager,
   Firebase auth/firestore/functions/analytics/crashlytics, Maps SDK +
   maps-compose + android-maps-utils, play-services-location, Billing 7,
   coroutines). `java.time` is fine (minSdk 26).
4. **UI strings:** hardcode English strings in composables for v0.1. Do not
   edit `res/values/strings.xml` (merge-conflict hotspot).
5. **UI code depends only on interfaces** from `data/Repositories.kt`,
   reached through the `AppContainer` parameter. Never instantiate a
   repository implementation in UI code.
6. **Privacy invariants (SOW):** no PII anywhere; no precise coordinates ever
   stored or transmitted (region ids only — see `core/Cities.kt`); no device
   identifiers; mood notes ≤ 5 words (`AppConfig.MAX_NOTE_WORDS`).
7. ViewModels are optional. If used: `androidx.lifecycle.ViewModel` created
   with `viewModel(factory = viewModelFactory { initializer { ... } })`
   capturing `container` from the composable parameter. Plain
   `remember`-based state holders are equally acceptable.

## Shared vocabulary

- Models: `core/model/` — `Mood` (6 SOW emojis + `valence` 0..1),
  `MoodCheckIn`, `UserProfile`/`AgeBracket`/`ProfileState`, `HeatmapScope`,
  `RegionInfo`, `RegionMoodAggregate`, `MatchState`, `ChatSession`,
  `ChatMessage`, `WeeklyInsights`, `MoodTrendPoint`, `MicroAction`,
  `ActionCategory`.
- Constants: `core/AppConfig.kt` (chat duration, match timeout, note word cap,
  product id `vibecheck_plus_monthly`, `CHAT_REQUIRES_ADULT` legal switch).
- Region buckets: `core/Cities.kt` (25 US/UK cities, `nearest()`, `distanceKm`).
- Valence colours: `ui/theme/Color.kt` → `ValenceLow/Mid/High`.

## Reference implementations

`data/fake/FakeAppContainer.kt` implements every repository in memory and is
the behavioural reference (validation rules, error messages, timing). Debug
builds use it by default (`BuildConfig.USE_FAKE_DATA`); the Firebase-backed
`DefaultAppContainer` is selected when that flag is false.

## Firebase wiring (for the data layer + backend)

- Placeholder `app/google-services.json` uses project id `demo-vibecheck`
  (Firebase Local Emulator Suite convention; no real project exists yet).
- When `BuildConfig.USE_FIREBASE_EMULATOR` is true, point Auth at
  `10.0.2.2:9099`, Firestore at `10.0.2.2:8080`, Functions at `10.0.2.2:5001`
  before any other Firebase call.
- Anonymous auth only. The Firebase uid is the only identity.
- Firestore layout (backend agent owns rules/functions; data layer must match):
  - `checkins/{checkInId}`: `{ uid, mood, valence, note, regionId, createdAt }`
  - `regions/{regionId}`: `{ name, countryCode, lat, lng, count24h, valenceSum24h, updatedAt }` (aggregates, maintained by Cloud Function trigger)
  - `matchQueue/{uid}`: `{ uid, mood, valence, regionId, createdAt, state: "waiting"|"matched", sessionId? }`
  - `chatSessions/{sessionId}`: `{ participants: [uidA, uidB], moods, startedAt, expiresAt, closed }`
    - subcollection `messages/{messageId}`: `{ senderUid, text, sentAt }`
  - `users/{uid}`: `{ username?, ageBracket, chatOptIn, createdAt, lastActiveAt }`
  - `reports/{reportId}`: `{ sessionId, reporterUid, reason, createdAt }`
- Subscription state is NOT trusted from the client: a callable function
  `validatePurchase` records entitlement in `users/{uid}.plusUntil`.

## File ownership map

| Area | Owns (under `app/src/main/java/com/vibecheck/app/` unless noted) |
|---|---|
| Data layer | `data/local/**`, `data/remote/**`, `data/DefaultAppContainer.kt`, `VibeCheckApp.kt` (container selection only) |
| Onboarding + check-in | `ui/onboarding/**`, `ui/checkin/**`, `core/reminder/**` |
| Heatmap | `ui/heatmap/**` |
| Micro-actions | `ui/actions/**`, `domain/actions/**`, tests in `app/src/test/.../actions/` |
| Chat | `ui/chat/**`, `domain/chat/**`, tests in `app/src/test/.../chat/` |
| Billing + insights + settings | `billing/**`, `ui/subscription/**`, `ui/insights/**`, `ui/settings/**` |
| Backend | `backend/**` (repo root), `firestore.rules`, `firestore.indexes.json`, `firebase.json`, `.firebaserc` |
| Docs | `README.md`, `docs/**` |

Cross-area class names referenced by the data layer (constructor signatures
are part of the contract):

- `com.vibecheck.app.billing.PlayBillingRepository(context: android.content.Context, externalScope: kotlinx.coroutines.CoroutineScope)` — implements `BillingRepository` (billing agent provides).
- `com.vibecheck.app.domain.actions.RuleBasedMicroActionEngine()` — implements `MicroActionEngine` (actions agent provides).
- `com.vibecheck.app.domain.chat.ProfanityFilter` — `fun clean(text: String): String` and `fun isAcceptable(text: String): Boolean` (chat agent provides; data layer and chat UI share it).
- `com.vibecheck.app.core.reminder.ReminderScheduler` — `object` with
  `fun enable(context: android.content.Context, hourOfDay: Int = 20)`,
  `fun disable(context: android.content.Context)`,
  `fun isEnabledFlow(context: android.content.Context): kotlinx.coroutines.flow.Flow<Boolean>`
  (onboarding/check-in agent provides via WorkManager; the settings screen consumes it).

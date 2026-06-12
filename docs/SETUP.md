# Setup — backend, Firebase & Maps

The app runs fully on demo data with **zero** of this. Follow these steps only
when you need the real (networked) stack.

## Data-layer switch

`VibeCheckApp` chooses the container from `BuildConfig.USE_FAKE_DATA`:

| Build | `USE_FAKE_DATA` | Container | Data |
|---|---|---|---|
| `debug` (default) | `true` | `FakeAppContainer` | in-memory demo |
| `debug -PuseFakeData=false` | `false` | `DefaultAppContainer` | Room + Firebase |
| `release` | `false` | `DefaultAppContainer` | Room + Firebase |

`USE_FIREBASE_EMULATOR` is `true` for debug, `false` for release.

## Firebase Local Emulator Suite (development)

1. Install tools: `npm i -g firebase-tools` and `cd backend/functions && npm install`.
2. Start the emulators from the repo root:
   ```bash
   firebase emulators:start --only functions,firestore,auth
   ```
3. Build the app with `-PuseFakeData=false`. When `USE_FIREBASE_EMULATOR=true`
   it points at the host automatically (`10.0.2.2` from an emulator):
   - Auth → `10.0.2.2:9099`
   - Firestore → `10.0.2.2:8080`
   - Functions → `10.0.2.2:5001`
4. Two emulated clients are needed to see an actual chat match; a single client
   will enqueue and hit the 30-second match timeout (expected).

## Real Firebase project (staging / production)

1. Create a Firebase project; add an Android app with package
   `com.vibecheck.app`.
2. Download the real `google-services.json` into `app/` (replacing the
   `demo-vibecheck` placeholder). It is git-ignored for production keys.
3. Enable **Anonymous Authentication** and **Cloud Firestore**.
4. Deploy rules, indexes, and functions:
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes,functions
   ```
   Scheduled functions (`rollupRegions`, `cleanupInactiveData`,
   `closeExpiredSessions`) require the **Blaze** plan.

## Google Maps API key

The heatmap's map view needs a real key; with the placeholder the list view
works but map tiles render blank.

1. In Google Cloud Console, enable **Maps SDK for Android** and create an API
   key (restrict it to the app's package + SHA-1).
2. Put it in `local.properties` (git-ignored):
   ```
   MAPS_API_KEY=AIza...
   ```
   It's injected via manifest placeholder at build time.

## Firestore collections (reference)

| Collection | Shape | Notes |
|---|---|---|
| `users/{uid}` | `ageBracket, chatOptIn, createdAt, lastActiveAt` | owner-only; no PII |
| `checkins/{id}` | `regionId, mood, valence, timestamp` | **anonymous**, no uid; create-only, unreadable by clients |
| `regions/{regionId}` | `name, countryCode, lat, lng, count24h, valenceSum24h, updatedAt` | server-maintained; clients read only |
| `matchQueue/{uid}` | `uid, mood, valence, regionId, createdAt, state, sessionId?` | self-only |
| `chatSessions/{id}` | `participants, moods, startedAt, expiresAt, closed` | participants read; functions write |
| `chatSessions/{id}/messages/{id}` | `senderUid, text, sentAt` | participant create, shape-locked, ≤280 chars |
| `reports/{id}` | `sessionId, reporterUid, reason, createdAt` | create-only |

See [`backend/README.md`](../backend/README.md) for the function details and
[`firestore.rules`](../firestore.rules) for the enforced access model.

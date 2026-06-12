# VibeCheck Backend (core-flow)

Firebase backend for the **core flow**: the heatmap aggregation pipeline and
privacy-driven retention cleanup. Matchmaking, chat and purchase-validation
functions belong to the social/account module and live alongside these once
that owner adds them.

## Layout

```
firebase.json            emulator + functions + firestore config
.firebaserc              project alias (demo-vibecheck)
firestore.rules          deny-by-default security rules
firestore.indexes.json   composite index for checkins(regionId, timestamp)
backend/functions/
  index.js               Cloud Functions (entry points)
  aggregate.js           pure 24h-window aggregation (unit-tested)
  regions.js             region metadata, mirrors core/Cities.kt
  test/rollup.test.js    node unit tests for the aggregation math
```

## Functions

| Function | Trigger | Purpose |
|---|---|---|
| `onCheckinCreated` | Firestore `checkins/{id}` create | Live-increments the check-in's region totals so the heatmap updates within seconds |
| `rollupRegions` | Schedule, hourly | Authoritative recompute of each region's rolling 24h `count24h` / `valenceSum24h`; self-heals the live increments and ages out old check-ins |
| `cleanupInactiveData` | Schedule, daily | SOW retention: deletes users inactive 90+ days (doc + anon auth) and purges check-ins older than 90 days |
| `requestMatch` | Callable | Claims a compatible waiting peer (same 2h mood window, valence within 0.2) and creates a 5-min `chatSessions` doc, or enqueues the caller in `matchQueue` |
| `cancelMatch` | Callable | Removes the caller's `matchQueue` entry |
| `leaveSession` | Callable | Marks a session closed (participant only) |
| `reportPeer` | Callable | Files a `reports` doc and closes the session (participant only) |
| `closeExpiredSessions` | Schedule, every 2 min | Closes past-expiry sessions and purges their messages (ephemeral chat) |

See `match.js` for the matchmaking + session lifecycle.

## Firestore data

- `checkins/{id}` `{ regionId, mood, valence, timestamp }` — **anonymous**, no uid/PII (doc id is a client-side `SHA-256(timestamp + salt)`)
- `regions/{regionId}` `{ name, countryCode, lat, lng, count24h, valenceSum24h, updatedAt }` — server-maintained aggregate the app's heatmap reads
- `users/{uid}` `{ ageBracket, chatOptIn, createdAt, lastActiveAt, ... }` — own-profile only

## Privacy (enforced by `firestore.rules`)

- Clients **cannot read** raw `checkins`; the heatmap only reads aggregated `regions`.
- A check-in create is shape-validated to `{ regionId, mood, valence, timestamp }` — any `uid`/identifier is rejected.
- `users/{uid}` rejects `email`/`phone` fields and is readable only by its owner.
- `regions` is read-only to clients; only Cloud Functions (Admin SDK) write it.

## Run locally

```bash
cd backend/functions && npm install
npm test                 # pure aggregation unit tests, no emulator
npm run serve            # functions + firestore + auth emulators
```

The Android debug build points Auth at `10.0.2.2:9099`, Firestore at
`10.0.2.2:8080` and Functions at `10.0.2.2:5001` when
`USE_FIREBASE_EMULATOR=true`.

## Deploy

```bash
firebase deploy --only firestore:rules,firestore:indexes,functions
```

> Needs a real Firebase project in `.firebaserc` (currently the
> `demo-vibecheck` emulator alias) and scheduled functions require the Blaze plan.

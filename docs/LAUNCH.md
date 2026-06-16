# Launch Checklist

Path from **feature-complete on demo data** to **live in the Google Play Store**.
The app code is done; what remains is infrastructure, accounts, compliance, and
release engineering.

Legend: 🔑 needs an account/credential (project owner) · 🛠️ engineering (in-repo) ·
⚖️ legal/compliance.

## Critical path
**Section 1 (real Firebase project + Maps key) gates everything** — no real
behaviour can be verified until it exists. Sections 2–4 can proceed in parallel
once it's up.

---

## 1. Backend infrastructure 🔑🛠️
- [ ] Create a real Firebase project on the **Blaze** plan (scheduled functions require it)
- [ ] Add Android app `com.vibecheck.app`; download the real `google-services.json` (replaces the `demo-vibecheck` placeholder in `app/`)
- [ ] Enable **Anonymous Authentication** and **Cloud Firestore**
- [ ] `firebase deploy --only firestore:rules,firestore:indexes,functions` (9 functions)
- [ ] Create a **Google Maps API key** (Maps SDK for Android, restricted to package + release SHA-1) → put in `local.properties` / CI secret
- [ ] Link the Functions service account in Play Console with the `androidpublisher` scope (for `validatePurchase`)

## 2. Google Play Console 🔑
- [ ] Play developer account ($25 one-time)
- [ ] Create the app entry (package `com.vibecheck.app`)
- [ ] Create subscription product `vibecheck_plus_monthly` with base plans at **$29.00 / £29.00**
- [ ] Store listing: title, short/full description, **screenshots**, feature graphic, icon — *draft copy: [PLAY_STORE.md](PLAY_STORE.md)*
- [ ] Content rating questionnaire
- [ ] **Data Safety** form (declare: anonymous, no PII, no precise location)
- [ ] Target audience & content: **16+**

## 3. Legal / compliance ⚖️
- [ ] **Privacy Policy** hosted at a public URL (required by Play) — COPPA + UK Online Safety Act — *draft: [legal/PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md)*
- [ ] Terms of Service — *draft: [legal/TERMS.md](legal/TERMS.md)*
- [ ] **Have both reviewed by a qualified lawyer** and fill in all `[PLACEHOLDER]`s
- [ ] "Not a medical device" + crisis-resource disclaimer (already shown in-app)
- [ ] OSA decision: does anonymous chat require **18+**? (flip `AppConfig.CHAT_REQUIRES_ADULT` if so)
- [ ] Confirm crisis helplines current (988 / 116 123)

## 4. Release engineering 🛠️
- [x] Release signing wired in `app/build.gradle.kts` (reads keystore from `local.properties`/env, debug-signing fallback) — see [RELEASE.md](RELEASE.md)
- [x] ProGuard/R8 verified against a real `bundleRelease` (minify + shrink) — AAB builds clean
- [ ] Create the signing keystore + add secrets to `local.properties` (owner holds the secret)
- [ ] Bump `versionCode` / `versionName` per release
- [ ] Build the signed **AAB** with the real keystore (`./gradlew :app:bundleRelease`)
- [ ] (Optional) CI workflow to build + run tests on PRs

## 5. Real-stack QA 🛠️ (after section 1 is live)
- [ ] End-to-end on the real backend: check-in → heatmap rollup; real matchmaking across **two devices**; purchase → `validatePurchase` → entitlement
- [ ] Verify 90-day retention + chat auto-delete jobs run
- [ ] Broaden automated test coverage
- [ ] Device/OS matrix smoke test (a few API levels)

---

## Status of the code itself ✅
All application + backend code is complete and merged to `main`:
splash, onboarding, check-in, micro-actions, heatmap, match/chat, insights,
settings, subscription; real data layer; 9 Cloud Functions; hardened billing
with server-trusted entitlement. Tests: ProfanityFilter (8), rollup (4),
entitlement (6) — green. See [README](../README.md) and [SETUP](SETUP.md).

# Contributing to VibeCheck

Welcome! This guide gets a new developer productive quickly. Read it alongside
[CONTRACTS.md](CONTRACTS.md) (the architecture contract) before your first PR.

## 1. Get it running (5 minutes)

```bash
./gradlew :app:installDebug      # builds + installs on a connected device/emulator
```

Debug builds use the **in-memory demo data layer** — no Firebase, no accounts,
no network. Every screen works with seeded fake data, so you can develop and
test UI without any backend setup. The real stack (Room + Firebase) only runs in
release builds or with `-PuseFakeData=false`; see [docs/SETUP.md](docs/SETUP.md).

Requirements: JDK 17, Android SDK (compile/target 35), a device or emulator on
API 26+.

## 2. How the code is organised

- **UI depends only on interfaces** in `data/Repositories.kt`, reached through
  the `AppContainer` passed into each screen. Never instantiate a repository
  implementation inside a composable.
- `data/fake/FakeAppContainer.kt` is the behavioural reference — it defines the
  validation rules, error messages, and timings the real implementations match.
- `DefaultAppContainer` wires the real (Room + Firebase) implementations.
- Models, constants, and region buckets live in `core/`.
- Backend (Cloud Functions, rules) lives in `backend/` + the `firestore.*` files.

## 3. Branch & PR workflow

- Branch off `main`. Use a descriptive prefix:
  `feature/…`, `fix/…`, `hardening/…`, `docs/…`.
- Keep PRs focused. Touch shared files (`AppContainer`, navigation, theme,
  `core/model`) in small, isolated commits to keep merges clean.
- Open a PR against `main` with a clear summary of **what changed, why, and how
  it was tested.** Note any caveats (e.g. "not verified against the emulator").
- Co-author trailers and AI-assistance disclosure are welcome but optional.

## 4. Before you push — checklist

```bash
./gradlew :app:assembleDebug             # compiles clean
./gradlew :app:testDebugUnitTest         # Kotlin unit tests pass
cd backend/functions && npm test         # backend logic tests pass (if you touched backend)
```

- [ ] Builds with no new warnings you introduced
- [ ] Tests pass (add tests for new logic — see `ProfanityFilterTest`, `rollup.test.js`)
- [ ] No **privacy invariant** broken (see README → Privacy invariants)
- [ ] No new dependency added without discussion (everything needed is already
      in `gradle/libs.versions.toml`)
- [ ] Hardcoded English UI strings are fine for now; **don't** edit
      `res/values/strings.xml` (merge-conflict hotspot)

## 5. Conventions

- Match the surrounding code's style, naming, and comment density.
- Compose state: plain `remember`-based holders or a `ViewModel` — both fine.
- `java.time` is available (min SDK 26).
- Coroutines for async; repositories expose `Flow`s and `suspend` functions.

## 6. Testing the real backend (optional)

You only need this if you're changing Firebase behaviour. Install the Firebase
CLI, then:

```bash
cd backend/functions && npm install
firebase emulators:start --only functions,firestore,auth
# build the app with -PuseFakeData=false; it points at 10.0.2.2 automatically
```

Full details — emulator ports, Maps key, real-project setup — are in
[docs/SETUP.md](docs/SETUP.md).

## Where to ask

Open a GitHub issue or start a draft PR with questions inline. When in doubt
about a boundary, `CONTRACTS.md` is the source of truth.

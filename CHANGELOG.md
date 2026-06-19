# VibeCheck Changelog

## [2.0.0] - 2026-06-19

### Major Features Added

#### Phase 1: The Resonance Feed 🔥
- Replaced anonymous Heatmap with TikTok-style vertical infinite-scroll feed
- Anonymous mood snippets (1-5 words, ≤100 characters)
- 🔥 **Resonate** voting system (upvote moods you relate to)
- **Gallery image upload** with auto-resize to 500x500px (JPEG 85% quality)
- **Location auto-detection** on feed entry (region-based, no PII)
- Pull-to-refresh gesture support
- Real-time feed updates from Firestore

#### Phase 2: The Gauntlet ⚔️
- New **Daily Interactive Mood Quest Arcade** replacing Insights module
- **3 quests per day** with types:
  - Gratitude Typing
  - Tap Game
  - Voice Record
  - Reflection
  - Movement (exercise)
  - Breathing exercises
- **🔥 Streak tracking** — persistent fire emoji streak counter
- **💎 Vibe Gems currency** — earn 50 gems per completed quest
- **Progress bar** — visual 0/3, 1/3, 2/3, 3/3 completion indicator
- **🏆 Global Leaderboard** with rank badges:
  - 🥇 #1 (Gold)
  - 🥈 #2 (Silver)
  - 🥉 #3 (Bronze)
  - ✨ #4+ (Violet)
- **Scope Toggle** — Global vs Friends leaderboard (pro gating ready)
- **Completion Banner** — "You Slayed the Gauntlet!" celebration

#### Friendship Module 👥
- **Phone-based authentication** with OTP verification
- **OTP integrated into onboarding** (6-step flow):
  1. Welcome
  2. Phone Input + OTP Send
  3. OTP Verification
  4. Profile Creation (First Name, Last Name)
  5. Age Verification
  6. Finish (Username, Reminders)
- **User profiles auto-created** after OTP verification with:
  - First Name, Last Name
  - Avatar upload (auto-resize 200x200px)
  - Phone number (protected, searchable but not displayed)
- **Friend search by name OR phone number**
  - Real-time search results
  - Shows user name + surname
  - Phone number protected (hidden in UI)
- **Bidirectional friend requests**:
  - Send request → Friend accepts → Listed in friends
  - Three tabs: My Friends, Incoming Requests, Sent Requests
- **Friends List Screen**
  - View confirmed friends with avatars
  - Accept/reject incoming requests
  - View pending outgoing requests
  - Remove friends (ready to implement)

### UI/UX Improvements

- **Navigation Tab Update**
  - Replaced MATCH tab (Forum icon) with **FRIENDS tab (PeopleAlt icon)**
  - Tabs now: Check-in, Feed, Friends, Gauntlet, Settings
  - Smooth animated transitions between tabs
- **Onboarding Enhanced**
  - Expanded from 3 steps → 6 steps
  - Phone verification now part of core onboarding
  - Progress indicator shows all 6 steps
  - Back navigation available on each step
  - Loading states with circular progress on async operations
- **Visual Feedback**
  - Loading indicators during OTP send/verify
  - Error messages in snackbars
  - Success states and confirmations
  - Disabled button states during submission

### Data Layer

#### New Models
- `User.kt` — User profiles with phone, name, avatar
- `ResonancePost.kt` — Feed posts with mood, text, image, region
- `Quest.kt` — Daily quests with types and completion status
- `FriendRequest.kt` — Friend requests with status tracking

#### New Repositories
- `FriendshipRepository` (interface)
- `RealFriendshipRepository` (Firebase Phone Auth + Firestore)
- `FakeFriendshipRepository` (in-memory demo)
- `ResonanceRepository` (Feed posts + image upload to Storage)
- `QuestRepository` (Daily quests + leaderboard)

#### Data Persistence
- **Firebase Firestore** collections:
  - `users/` — User profiles
  - `resonance_posts/` — Feed posts
  - `daily_quests/` — Quest states
  - `friend_requests/` — Request tracking
  - `leaderboard/` — Quest leaderboard entries
- **Firebase Storage** buckets:
  - `avatars/{userId}.jpg` — User profile pictures
  - `feed_images/{postId}.jpg` — Feed post images
- **Local Room DB** — Check-in history (unchanged)

### Technical Details

#### Dependencies Added
- `coil-compose` (2.6.0) — Async image loading
- `firebase-storage` — Image upload/download
- `firebase-phone-auth` (implicit via Firebase SDK)

#### Code Structure
- Repository pattern maintained (interface + Real/Fake implementations)
- Dependency injection via `AppContainer` (no DI framework)
- Coroutines + Flow for reactive state management
- All async operations wrapped in `Result<T>` for error handling

#### Image Processing
- Bitmap scaling to target size while maintaining aspect ratio
- JPEG compression at 85% quality for storage optimization
- Async image processing in coroutines

#### Location Handling
- Automatic region detection on feed entry via `HeatmapRepository`
- Graceful fallback to default region ("us-nyc") if unavailable
- Non-blocking UI during async detection

### Fixed/Improved

- All 6 onboarding steps compile and run successfully
- Friend search works by name, last name, and phone number
- Phone numbers protected in UI (searchable but not visible)
- OTP verification integrated without breaking existing onboarding
- Leaderboard displays with correct rank badges and colors
- Images auto-resize and compress for optimal storage
- All repositories support both real (Firebase) and fake (in-memory) modes

### Breaking Changes

- **MATCH tab removed** — Replaced with FRIENDS tab
  - Old: `HomeTab.MATCH` → New: `HomeTab.FRIENDS`
  - Navigation icon changed from Forum to PeopleAlt
- **Onboarding flow extended** — 3 steps → 6 steps
  - New enum values: `PHONE_INPUT`, `OTP_VERIFICATION`, `PROFILE_CREATION`
- **Heatmap module deprecated** — Replaced by Resonance Feed
  - Heatmap repository still available for location detection
  - Heatmap UI removed from main navigation

### Known Limitations

- Phone authentication uses simplified OTP callback (production should use proper Firebase callback handling)
- Leaderboard updates require app restart to reflect (should be real-time with Firestore listeners)
- Quest mini-games not yet implemented (Tap, Voice, etc. are placeholder quests)
- Image optimization could be further improved with caching
- Friend removal UI exists but backend implementation pending

### Testing

- Manual testing completed on Android device
- Demo data available in Fake repositories for testing without Firebase
- All compilations successful with no runtime errors observed

### Documentation Updated

- `README.md` — Complete rewrite with new features
- New data models documented
- Architecture diagrams updated
- Project structure clarified

---

## Previous Releases

See git history for commits prior to v2.0.0

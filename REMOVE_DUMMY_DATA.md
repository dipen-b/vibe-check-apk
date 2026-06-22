# Comprehensive Guide: Remove Dummy Data & Use Real Firebase

## 📌 Overview

Currently, the app has **two data layers**:

1. **Fake Layer** (Default in debug) - In-memory, seeded demo data
2. **Real Layer** (For production) - Firebase Firestore, Storage, Auth

This guide explains **how to completely remove dummy data** and switch to 100% real Firebase.

---

## 🗂️ Part 1: Understanding the Dummy Data Architecture

### Current Structure

```
AppContainer (Interface)
    ↓
    ├─ BuildConfig.USE_FAKE_DATA == true (DEBUG DEFAULT)
    │  └─ FakeAppContainer (In-memory demo data)
    │     ├─ FakeMoodRepository (seeded 7-day history)
    │     ├─ FakeChatRepository (mock conversations)
    │     ├─ FakeHeatmapRepository (synthetic regions)
    │     ├─ FakeResonanceRepository (5 seeded posts)
    │     ├─ FakeQuestRepository (demo quests + leaderboard)
    │     ├─ FakeFriendshipRepository (5 demo users)
    │     ├─ FakeBillingRepository (mock subscription)
    │     └─ FakeInsightsRepository (calculated data)
    │
    └─ BuildConfig.USE_FAKE_DATA == false (RELEASE)
       └─ DefaultAppContainer (Real Firebase)
          ├─ RealMoodRepository
          ├─ RealChatRepository
          ├─ RealHeatmapRepository
          ├─ RealResonanceRepository
          ├─ RealQuestRepository
          ├─ RealFriendshipRepository
          ├─ PlayBillingRepository
          └─ RealInsightsRepository
```

### Where Dummy Data Lives

```
app/src/main/java/com/vibecheck/app/data/
├── fake/
│   ├── FakeAppContainer.kt          ← Routes to all fake repos
│   ├── FakeMoodRepository.kt        ← Seeded 7-day check-in history
│   ├── FakeHeatmapRepository.kt     ← Synthetic 50+ cities
│   ├── FakeChatRepository.kt        ← Mock chat matching
│   ├── FakeResonanceRepository.kt   ← 5 seeded feed posts
│   ├── FakeQuestRepository.kt       ← Demo daily quests
│   ├── FakeFriendshipRepository.kt  ← 5 demo users (John, Jane, Alex, etc.)
│   ├── FakeBillingRepository.kt     ← Mock $29.99/month subscription
│   └── FakeInsightsRepository.kt    ← Calculated weekly trends
└── real/
    ├── DefaultAppContainer.kt       ← Real Firebase wiring
    ├── RealMoodRepository.kt
    ├── RealChatRepository.kt
    ├── RealResonanceRepository.kt
    ├── RealQuestRepository.kt
    └── RealFriendshipRepository.kt
```

---

## 🎯 Part 2: What Dummy Data Exists

### 🧑 Dummy Users (FakeFriendshipRepository)

```kotlin
val usersMap = mutableMapOf(
    "user-123" to User(
        userId = "user-123",
        firstName = "John",
        lastName = "Doe",
        phoneNumber = "9876543210",
        countryCode = "+1",
        avatarUrl = "",
        createdAtMillis = System.currentTimeMillis(),
    ),
    "user-456" to User(
        userId = "user-456",
        firstName = "Jane",
        lastName = "Smith",
        phoneNumber = "8765432109",
        countryCode = "+1",
        avatarUrl = "",
        createdAtMillis = System.currentTimeMillis(),
    ),
    "user-789" to User(
        userId = "user-789",
        firstName = "Alex",
        lastName = "Johnson",
        phoneNumber = "7654321098",
        countryCode = "+44",
        avatarUrl = "",
        createdAtMillis = System.currentTimeMillis(),
    ),
)
```

### 🔥 Dummy Resonance Feed (FakeResonanceRepository)

```kotlin
val now = System.currentTimeMillis()
val daySecs = 24L * 60 * 60 * 1000
posts.addAll(
    listOf(
        ResonancePost(
            id = "demo-1",
            mood = Mood.HAPPY,
            text = "Finally got the promotion!",
            regionId = "us-nyc",
            createdAtMillis = now - 5 * 60_000,
            resonateCount = 12,
        ),
        // ... 4 more seeded posts
    )
)
```

### ⚔️ Dummy Quests (FakeQuestRepository)

```kotlin
val quests = listOf(
    Quest(
        id = UUID.randomUUID().toString(),
        questNumber = 1,
        mood = mood,
        title = "Gratitude Rush",
        description = "Type 3 things you're grateful for in 60 seconds",
        questType = QuestType.GRATITUDE_TYPING,
    ),
    // ... 2 more quests
)
```

### 🏆 Dummy Leaderboard (FakeQuestRepository)

```kotlin
listOf(
    LeaderboardEntry(rank = 1, username = "You", currentStreak = 7, totalGems = 250),
    LeaderboardEntry(rank = 2, username = "Alex", currentStreak = 12, totalGems = 520),
    LeaderboardEntry(rank = 3, username = "Jordan", currentStreak = 5, totalGems = 180),
    // ... 4 more entries
)
```

### 😊 Dummy Mood Check-ins (FakeMoodRepository)

```kotlin
val moods = listOf(
    Mood.HAPPY, Mood.NEUTRAL, Mood.TIRED, Mood.SAD, Mood.HAPPY, 
    Mood.EXCITED, Mood.NEUTRAL, Mood.TIRED, Mood.HAPPY, Mood.NEUTRAL
)
val notes = listOf(
    "good coffee", null, "long day", "miss home", null,
    "friday!", null, "slept badly", "sunny walk", null
)
// Creates 10-day history automatically
```

### 💬 Dummy Chat (FakeChatRepository)

```kotlin
private val cannedReplies = listOf(
    "yeah, i get that",
    "what helped you last time?",
    "same here honestly",
    "that sounds heavy",
    "small wins still count",
    "glad we matched 🙂",
)
// Mock matching takes 2.5 seconds, sends 1 canned response
```

### 🌍 Dummy Heatmap (FakeHeatmapRepository)

```kotlin
val regions = when (scope) {
    HeatmapScope.LOCAL -> Cities.ALL.filter { /* nearby */ }
    HeatmapScope.NATIONAL -> Cities.ALL.filter { /* same country */ }
    HeatmapScope.GLOBAL -> Cities.ALL
}
// Returns 50+ synthetic region mood aggregates
```

---

## 🔧 Part 3: Step-by-Step Removal

### Phase 1: Prepare Firebase Project

#### 1.1 Create Firebase Project
```bash
# Go to https://console.firebase.google.com/
# Create new project
# Enable Firestore, Storage, Phone Auth
# Download google-services.json → place in app/
```

#### 1.2 Initialize Collections
```javascript
// In Firestore Console, create these collections (or auto-create on first write):
db.collection("users").doc("[userId]").set({...})
db.collection("resonance_posts").doc("[postId]").set({...})
db.collection("daily_quests").doc("[dateKey]").set({...})
db.collection("friend_requests").doc("[requestId]").set({...})
db.collection("leaderboard").doc("[userId]").set({...})
```

#### 1.3 Create Storage Buckets
```bash
# In Firebase Console → Storage
# Create bucket for avatars
# Create bucket for feed images
```

#### 1.4 Enable Phone Authentication
```bash
# Firebase Console → Authentication
# Enable Phone provider
# Add test numbers for development
```

---

### Phase 2: Update Build Configuration

#### 2.1 Switch Build Flag

**File:** `app/build.gradle.kts`

```kotlin
// BEFORE (uses fake data by default)
android {
    defaultConfig {
        // ...
    }
}

// AFTER (use real data)
android {
    buildTypes {
        debug {
            // Force real Firebase even in debug
            buildConfigField "boolean", "USE_FAKE_DATA", "false"
        }
        release {
            buildConfigField "boolean", "USE_FAKE_DATA", "false"
        }
    }
}
```

#### 2.2 Or Build from Command Line

```bash
# Build without fake data
./gradlew -PuseFakeData=false assembleDebug

# Check what it's using
grep -r "USE_FAKE_DATA" app/build.gradle.kts
```

---

### Phase 3: Update AppContainer

#### 3.1 Remove FakeAppContainer Usage

**File:** `app/src/main/java/com/vibecheck/app/VibeCheckApp.kt`

```kotlin
// BEFORE
val container = if (BuildConfig.USE_FAKE_DATA) {
    FakeAppContainer(this)
} else {
    DefaultAppContainer(this)
}

// AFTER (always use real)
val container = DefaultAppContainer(this)
```

#### 3.2 Ensure Real Repositories are Wired

**File:** `app/src/main/java/com/vibecheck/app/data/DefaultAppContainer.kt`

```kotlin
// Make sure these are REAL, not FAKE:
override val profileRepository = RealProfileRepository(...)
override val moodRepository = RealMoodRepository(...)
override val heatmapRepository = RealHeatmapRepository(...)
override val resonanceRepository = RealResonanceRepository(...)
override val questRepository = RealQuestRepository(...)
override val chatRepository = RealChatRepository(...)
override val friendshipRepository = RealFriendshipRepository(...)
override val billingRepository = PlayBillingRepository(...)
override val insightsRepository = RealInsightsRepository(...)
```

---

### Phase 4: Disable Fake Repositories

#### 4.1 Stop Importing Fake Repos

Remove all imports from app code:

```kotlin
// REMOVE THESE IMPORTS:
import com.vibecheck.app.data.fake.FakeMoodRepository
import com.vibecheck.app.data.fake.FakeResonanceRepository
import com.vibecheck.app.data.fake.FakeQuestRepository
import com.vibecheck.app.data.fake.FakeFriendshipRepository
import com.vibecheck.app.data.fake.FakeChatRepository
// etc...
```

#### 4.2 Don't Delete Fake Files Yet

Keep them for reference or as fallback:
- They're useful for understanding the data structure
- Can serve as test fixtures
- Help document expected data format

#### 4.3 Optional: Delete After Verification

Once fully tested with real data:

```bash
# After confident all real data works:
rm -rf app/src/main/java/com/vibecheck/app/data/fake/

# Commit this removal
git add -A
git commit -m "Remove fake data repositories - using real Firebase"
```

---

### Phase 5: Verify Real Firebase is Wired

#### 5.1 Check Firebase Initialization

**File:** `app/src/main/AndroidManifest.xml`

```xml
<!-- Ensure this exists (Firebase auto-configures via google-services.json) -->
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="default" />
```

#### 5.2 Check FirebaseProvider

**File:** `app/src/main/java/com/vibecheck/app/data/firebase/FirebaseProvider.kt`

```kotlin
object FirebaseProvider {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
    // NOT using emulator (comment out if you were)
}
```

#### 5.3 Verify google-services.json

```bash
ls -la app/google-services.json  # MUST exist

# Check it has your project ID
grep "project_id" app/google-services.json
```

---

## 🧪 Part 4: Testing Without Dummy Data

### 4.1 Build & Install

```bash
# Clean build
./gradlew clean

# Build with real data flag
./gradlew -PuseFakeData=false assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.vibecheck.app/.MainActivity
```

### 4.2 Test Each Feature

#### Onboarding
- [ ] Phone input accepts only 10+ digits
- [ ] Firebase sends real SMS with OTP
- [ ] OTP verification works with code from SMS
- [ ] Profile creation saves to Firestore
- [ ] Avatar uploads to Firebase Storage

#### Mood Check-in
- [ ] Submit mood → appears in Firestore `users/{uid}/checkins/`
- [ ] 7-day history loads from real database
- [ ] Streak calculation is accurate

#### Resonance Feed
- [ ] New post saves to `resonance_posts/` collection
- [ ] Images upload to Firebase Storage
- [ ] Feed loads real posts (not seeded)
- [ ] Resonate count increments in Firestore

#### Friends
- [ ] Search queries real `users/` collection
- [ ] Friend requests save to Firestore
- [ ] Accept/reject updates `friends/` subcollection
- [ ] Phone numbers not displayed (protected)

#### Gauntlet Quests
- [ ] Daily quests stored in `daily_quests/{dateKey}`
- [ ] Completion updates Firestore
- [ ] Leaderboard fetches from `leaderboard/` collection
- [ ] Streak persists across app restarts

#### Images
- [ ] Avatars stored in Storage `avatars/{userId}.jpg`
- [ ] Feed images in Storage `feed_images/{postId}.jpg`
- [ ] Images auto-resize to target dimensions

### 4.3 Monitor Firestore Activity

```bash
# Watch Firestore writes in real-time
adb logcat | grep "Firestore"

# Watch Firebase Auth logs
adb logcat | grep "PhoneAuth"

# Watch Storage operations
adb logcat | grep "Storage"
```

### 4.4 Check Firestore Console

In Firebase Console:
1. Go to Firestore Database
2. Trigger actions in app
3. See collections populate in real-time:
   - `users/` → new user profiles
   - `resonance_posts/` → new feed posts
   - `daily_quests/` → quest data
   - `friend_requests/` → friend requests

---

## 🔍 Part 5: What Changes After Removing Dummy Data

### Data Behavior

| Aspect | Before (Dummy) | After (Real) |
|--------|---|---|
| **Users** | 5 hardcoded users | Only authenticated users |
| **Feed Posts** | 5 seeded posts | Only user-created posts |
| **Check-in History** | Auto-generated 10 days | Only submitted check-ins |
| **Friends** | Hardcoded relationships | Real user connections |
| **Leaderboard** | Demo rankings | Real user rankings |
| **Chat Matches** | Instant mock matches | Real matching via Cloud Functions |
| **Streak** | Hardcoded value | Calculated from real check-ins |
| **Billing** | Mock subscription | Real Google Play subscription |

### Persistence

- **Before**: Data lost on app restart (in-memory)
- **After**: Data persists across app restarts (Firestore)

### Performance

- **Before**: Instant (in-memory, no network)
- **After**: Depends on network (Firebase latency)

### Scalability

- **Before**: Limited to hardcoded data
- **After**: Scales to millions of users

---

## 🚀 Part 6: Production Checklist

Before going to production, remove/replace:

### Dummy Data Removal
- [ ] Delete all `fake/` repository classes
- [ ] Remove `FakeAppContainer.kt`
- [ ] Remove all fake test data seeding
- [ ] Remove test phone numbers from Firebase

### Real Data Verification
- [ ] All features tested with real Firebase
- [ ] Real user registration works
- [ ] Phone OTP verified via SMS
- [ ] Images upload and serve from Storage
- [ ] Firestore queries work at scale
- [ ] Leaderboard queries optimized
- [ ] Friend search scales to 1000+ users

### Security Rules Production
- [ ] Update Firestore rules (from test mode)
- [ ] Update Storage rules
- [ ] Enable reCAPTCHA for phone auth
- [ ] Review and audit all data access

### Performance Optimization
- [ ] Add Firestore indexes for queries
- [ ] Cache user profiles locally
- [ ] Optimize image sizes before upload
- [ ] Implement pagination for feeds
- [ ] Add error handling for network failures

### Monitoring & Analytics
- [ ] Set up Cloud Logging
- [ ] Enable Firestore Insights
- [ ] Monitor Storage usage
- [ ] Track authentication metrics
- [ ] Alert on quota limits

---

## 🆘 Part 7: Troubleshooting Without Dummy Data

### Issue: "User profile not found"
**Cause**: Trying to load data that hasn't been created yet  
**Fix**: Create user profile during onboarding

### Issue: "Firestore is empty"
**Cause**: Using fake data flag, or no data submitted  
**Fix**: Check `USE_FAKE_DATA=false`, submit test data

### Issue: "OTP never arrives"
**Cause**: Phone auth not enabled, billing not set up, invalid number  
**Fix**: Enable phone auth in Firebase, enable billing, use test numbers

### Issue: "Images not uploading"
**Cause**: Storage bucket not created, security rules block write  
**Fix**: Create bucket, update Storage rules, check authentication

### Issue: "Leaderboard is empty"
**Cause**: No quests completed yet, no leaderboard collection  
**Fix**: Complete quests, data auto-populates in Firestore

### Issue: "Friend search returns nothing"
**Cause**: No users in database yet  
**Fix**: Create multiple user accounts first

### Issue: "App crashes on auth"
**Cause**: `google-services.json` missing or invalid  
**Fix**: Download fresh `google-services.json` from Firebase Console

---

## 📋 Part 8: Complete Removal Checklist

```
Phase 1: Firebase Setup
  [ ] Create Firebase project
  [ ] Download google-services.json
  [ ] Enable Firestore, Storage, Phone Auth
  [ ] Create security rules

Phase 2: Code Changes
  [ ] Update app/build.gradle.kts (USE_FAKE_DATA=false)
  [ ] Update VibeCheckApp.kt (remove fake routing)
  [ ] Update DefaultAppContainer.kt (verify real repos)
  [ ] Remove fake imports from screens

Phase 3: Verification
  [ ] Build with -PuseFakeData=false
  [ ] Install APK on device
  [ ] Test onboarding with real phone auth
  [ ] Test each feature (feed, friends, quests, etc.)
  [ ] Monitor Firestore in console
  [ ] Check images in Storage

Phase 4: Production
  [ ] Delete all fake/ repository classes
  [ ] Update Firestore security rules
  [ ] Update Storage security rules
  [ ] Remove test phone numbers from Firebase
  [ ] Set up monitoring and alerts
  [ ] Document Firebase quotas and costs
  [ ] Test with real users

Phase 5: Launch
  [ ] Final comprehensive testing
  [ ] Backup Firestore data
  [ ] Set up retention policies
  [ ] Enable audit logging
  [ ] Configure backup & disaster recovery
```

---

## 📊 Migration Path

```
Current State (Dummy):
  USE_FAKE_DATA=true → FakeAppContainer → 5 demo users

↓ (This Guide)

Transition State (Real):
  USE_FAKE_DATA=false → DefaultAppContainer → Real Firebase → You create users

↓

Final State (Production):
  USE_FAKE_DATA removed → Only Real Firebase → Thousands of real users
```

---

## ✅ Success Indicators

You've successfully removed dummy data when:

- ✅ App builds with `-PuseFakeData=false`
- ✅ New user registration works with real phone OTP
- ✅ Submitted data appears in Firestore collections
- ✅ Friend profiles are only from real database
- ✅ Feed posts are user-created, not seeded
- ✅ Quests/leaderboard use real data
- ✅ Images upload to Firebase Storage
- ✅ Streaks and stats calculated from real data
- ✅ No hardcoded demo users visible
- ✅ All features work without internet fails gracefully

---

## 📚 Related Docs

- See **FIREBASE_SETUP.md** for detailed Firebase setup
- See **REAL_DATA_CHECKLIST.md** for quick start
- Check **README.md** for architecture overview

---

**After completing this guide, your app will be 100% real Firebase!** 🎉

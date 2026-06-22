# Firebase Setup Guide - Real Data Configuration

## 🎯 Step 1: Create Firebase Project

### Option A: Using Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Name it (e.g., "VibeCheck")
4. Enable Google Analytics (optional)
5. Create project

### Option B: Using Firebase CLI
```bash
npm install -g firebase-tools
firebase login
firebase init
```

---

## 🔑 Step 2: Get Firebase Configuration

### In Firebase Console:
1. Go to **Project Settings** (⚙️ icon)
2. Click **Service Accounts** tab
3. Click **Generate New Private Key**
4. Save as `firebase-key.json` (keep this **SECRET**)
5. Copy **Project ID** (you'll need this)

### Android-Specific Config:
1. Go to **Project Settings** → **General**
2. Scroll down to "Your apps"
3. Find your Android app or click "Add app"
4. Select Android
5. Enter:
   - Package name: `com.vibecheck.app`
   - App nickname: `VibeCheck Android`
   - SHA-1 certificate: (Get from your signing key)
6. Download `google-services.json`
7. Move to `app/google-services.json`

---

## 📦 Step 3: Configure Firestore Database

### Create Database:
1. Go to **Firestore Database** in Firebase Console
2. Click **Create Database**
3. Choose region (closest to your users)
4. Security rules: **Start in test mode** (for development)
5. Click **Create**

### Create Collections:
Run these queries in Firestore Console (or use Cloud Functions):

```javascript
// users collection (auto-created when first user signs up)
// Structure:
{
  userId: {
    firstName: "John",
    lastName: "Doe",
    phoneNumber: "9876543210",
    countryCode: "+1",
    avatarUrl: "https://...",
    createdAtMillis: 1234567890
  }
}

// friend_requests collection
{
  requestId: {
    requestId: "uuid",
    senderId: "user123",
    senderFirstName: "John",
    senderLastName: "Doe",
    senderAvatarUrl: "https://...",
    receiverId: "user456",
    status: "PENDING", // PENDING, ACCEPTED, REJECTED
    createdAtMillis: 1234567890
  }
}

// resonance_posts collection
{
  postId: {
    id: "uuid",
    mood: "HAPPY",
    text: "Great day",
    regionId: "us-nyc",
    createdAtMillis: 1234567890,
    resonateCount: 5,
    authorId: "user123",
    imageUrl: "https://..." // optional
  }
}

// daily_quests collection
{
  dateKey: {
    userId: "user123",
    dateMillis: 1234567890,
    mood: "HAPPY",
    quests: [
      {
        id: "uuid",
        questNumber: 1,
        mood: "HAPPY",
        title: "Gratitude Rush",
        description: "Type 3 things...",
        questType: "GRATITUDE_TYPING",
        isCompleted: false
      }
    ],
    completedCount: 0,
    streakDays: 0,
    totalGemsEarned: 0
  }
}

// leaderboard collection
{
  userId: {
    rank: 1,
    username: "johndoe",
    currentStreak: 7,
    totalGems: 250,
    isFriend: true
  }
}
```

---

## 🖼️ Step 4: Configure Firebase Storage

### Create Buckets:
1. Go to **Storage** in Firebase Console
2. Click **Create bucket**
3. Name: `vibecheck-avatars` (or default)
4. Region: Same as Firestore
5. Security rules: **Start in test mode**
6. Create

Repeat for images if needed.

---

## 📱 Step 5: Enable Phone Authentication

### In Firebase Console:
1. Go to **Authentication** → **Sign-in method**
2. Click **Phone**
3. Enable it
4. Add phone numbers for testing:
   - Click **Phone numbers for testing**
   - Add test numbers: `+1234567890`, `+919876543210`, etc.
   - Set test OTP: `123456` (for testing)

### For Production:
1. Enable on your Google Cloud Project
2. Set up billing (SMS costs money)
3. Verify in production mode (remove test numbers)

---

## 🔐 Step 6: Security Rules

### Replace Firestore Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can only read/write their own profile
    match /users/{userId} {
      allow read: if request.auth.uid == userId || request.auth.uid != null;
      allow write: if request.auth.uid == userId;
      
      // Friends subcollection
      match /friends/{friendId} {
        allow read: if request.auth.uid == userId;
        allow write: if request.auth.uid == userId;
      }
    }
    
    // Friend requests
    match /friend_requests/{requestId} {
      allow read: if request.auth.uid == resource.data.senderId || 
                     request.auth.uid == resource.data.receiverId;
      allow create: if request.auth.uid == request.resource.data.senderId;
      allow update: if request.auth.uid == resource.data.receiverId;
    }
    
    // Resonance posts
    match /resonance_posts/{postId} {
      allow read: if request.auth.uid != null;
      allow create: if request.auth.uid == request.resource.data.authorId;
      allow update: if request.auth.uid == resource.data.authorId;
    }
    
    // Quests
    match /daily_quests/{questId} {
      allow read, write: if request.auth.uid == resource.data.userId;
    }
    
    // Leaderboard (read-only for users)
    match /leaderboard/{userId} {
      allow read: if request.auth.uid != null;
    }
  }
}
```

### Replace Storage Rules:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Avatars
    match /avatars/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth.uid == userId;
    }
    
    // Feed images
    match /feed_images/{postId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth.uid != null;
    }
  }
}
```

---

## 🔧 Step 7: Update Android App Code

### 1. Update `DefaultAppContainer.kt`:

```kotlin
// Make sure Firebase is initialized with your project
override val friendshipRepository = RealFriendshipRepository(
    context = app,
    auth = FirebaseProvider.auth,
    firestore = FirebaseProvider.firestore,
    storage = FirebaseStorage.getInstance(),
)

// Other repositories...
override val moodRepository = RealMoodRepository(...)
override val resonanceRepository = RealResonanceRepository(...)
override val questRepository = RealQuestRepository(...)
```

### 2. Check `FirebaseProvider.kt`:

```kotlin
object FirebaseProvider {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { 
        val fs = FirebaseFirestore.getInstance()
        // Comment out emulator for production
        // fs.useEmulator("10.0.2.2", 8080)
        fs
    }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
}
```

### 3. Ensure `google-services.json` is in place:

```bash
ls app/google-services.json  # Should exist
```

---

## 🚀 Step 8: Build with Real Data

### Switch from Fake to Real:

```bash
# Build with REAL Firebase (no fake data)
./gradlew -PuseFakeData=false assembleDebug

# Or, edit build.gradle.kts:
android {
    buildTypes {
        debug {
            buildConfigField "boolean", "USE_FAKE_DATA", "false"
        }
    }
}
```

### Install on Device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.vibecheck.app/.MainActivity
```

---

## ✅ Step 9: Test Real Data

### Test Checklist:

- [ ] **Onboarding**: Phone number → OTP → Profile creation
- [ ] **Check-in**: Submit mood, see it in Firestore
- [ ] **Feed**: Post resonance, upload image, see in Storage
- [ ] **Friends**: Search, send request, accept
- [ ] **Quests**: Complete quest, update leaderboard
- [ ] **Images**: Avatar uploads, feed images resize/store

### Debug Tips:

```bash
# View Firestore in real-time
adb logcat | grep "Firestore"

# Check Firebase Auth logs
adb logcat | grep "PhoneAuth"

# Monitor Storage uploads
adb logcat | grep "Storage"
```

---

## 🔑 Environment Variables (Optional)

Create `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
firebase.project_id=your-project-id
firebase.storage_bucket=your-project.appspot.com
```

---

## 🚨 Security Checklist

Before going to production:

- [ ] Remove test phone numbers from Firebase Console
- [ ] Update Firestore security rules (test mode → production rules)
- [ ] Update Storage security rules
- [ ] Set up Firebase billing (phone SMS costs ~$0.01 per message)
- [ ] Enable reCAPTCHA for phone auth
- [ ] Review and audit Firestore data
- [ ] Set up Cloud Firestore backups
- [ ] Enable Cloud Audit Logs
- [ ] Review user data privacy policy

---

## 📊 Expected Data Structure After First User

```
Firebase Project
├── Firestore
│   ├── users/
│   │   └── [userId]
│   │       ├── firstName: "John"
│   │       ├── lastName: "Doe"
│   │       ├── phoneNumber: "9876543210"
│   │       └── friends/
│   │           └── [friendId1]
│   ├── resonance_posts/
│   │   ├── [postId1]
│   │   └── [postId2]
│   ├── daily_quests/
│   │   └── [dateKey]
│   ├── friend_requests/
│   │   └── [requestId1]
│   └── leaderboard/
│       └── [userId]
├── Storage
│   ├── avatars/
│   │   └── [userId].jpg
│   └── feed_images/
│       └── [postId].jpg
└── Authentication
    └── [userId] (Phone Auth)
```

---

## 🆘 Troubleshooting

| Issue | Solution |
|---|---|
| **OTP not sending** | Check phone auth enabled, billing enabled, valid phone number |
| **Images not uploading** | Check Storage rules, bucket exists, user authenticated |
| **Firestore queries failing** | Check security rules, user authenticated, collection exists |
| **App crashes on auth** | Ensure google-services.json in app/ folder |
| **Data not persisting** | Check Firestore write permissions in security rules |

---

## 📚 Resources

- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/start)
- [Firebase Phone Auth](https://firebase.google.com/docs/auth/android/phone-auth)
- [Cloud Functions Deployment](https://firebase.google.com/docs/functions/get-started/deploy)

---

**Status:** Once this is complete, your app will be using **real Firebase data** instead of demo data! 🎉

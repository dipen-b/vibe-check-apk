# Real Firebase Setup - Complete Step-by-Step Guide

## 🎯 Overview

After this setup, your app will work with **REAL** data instead of dummy data.

**Total Time: ~30 minutes**

---

## ✅ Step 1: Create Firebase Project (5 min)

### 1.1 Open Firebase Console
```
Go to: https://console.firebase.google.com/
```

### 1.2 Create New Project
1. Click **"Create a project"**
2. **Project name:** `VibeCheck` (or any name)
3. Click **Continue**
4. Choose region (closest to you)
5. Click **Create project**
6. Wait for project creation (~2 minutes)

### 1.3 Get Project ID
1. Go to **Project Settings** (⚙️ icon)
2. Copy **Project ID** (looks like: `vibecheck-12345`)
3. Save it - you'll need it

---

## 📱 Step 2: Add Android App to Firebase (5 min)

### 2.1 Register Android App
1. In Firebase Console, click **Add app**
2. Select **Android**
3. Fill in:
   - **Package name:** `com.vibecheck.app`
   - **App nickname:** `VibeCheck Mobile`
   - **SHA-1 certificate hash:** (see below)
4. Click **Register app**

### 2.2 Get SHA-1 Certificate Hash
```bash
# Run this command to get your SHA-1 hash:
cd /Users/qa/eclipse-workspace/vibecheck
./gradlew signingReport

# Output will show:
# Variant: debug
# Config: debug
# Store: ~/.android/debug.keystore
# SHA-1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX

# Copy the SHA-1 value
```

Paste this SHA-1 into Firebase form in step 2.1

### 2.3 Download google-services.json
1. Click **Download google-services.json**
2. Save the file
3. Move it to your project:
```bash
cp ~/Downloads/google-services.json /Users/qa/eclipse-workspace/vibecheck/app/
```

**Verify it's there:**
```bash
ls -la /Users/qa/eclipse-workspace/vibecheck/app/google-services.json
```

---

## 🗄️ Step 3: Enable Firestore Database (3 min)

### 3.1 Create Firestore Database
1. In Firebase Console, go to **Firestore Database**
2. Click **Create Database**
3. Choose:
   - **Location:** Closest to you
   - **Security Rules:** Start in **test mode** (for now)
4. Click **Create**
5. Wait for creation (~1 minute)

### 3.2 Verify Firestore is Ready
- You should see empty collections view
- Collections will auto-create when data is written

---

## 📸 Step 4: Enable Cloud Storage (2 min)

### 4.1 Create Storage Bucket
1. In Firebase Console, go to **Storage**
2. Click **Create Bucket** (or **Get Started**)
3. Choose:
   - **Bucket name:** `vibecheck-avatars` (or default)
   - **Location:** Same as Firestore
   - **Storage class:** Standard
   - **Security rules:** Start in **test mode**
4. Click **Create**

---

## 📱 Step 5: Enable Phone Authentication (2 min)

### 5.1 Enable Phone Provider
1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Click **Phone**
4. Toggle **Enable**
5. Click **Save**

### 5.2 Add Test Phone Numbers (for development)
1. Scroll down to **Phone numbers for testing**
2. Click **Add phone number**
3. Add test numbers:
   ```
   +1 234 567 8900  (USA test)
   +91 98765 43210  (India test)
   ```
4. Set OTP for testing: `123456`
5. Click **Save**

Now SMS won't actually send, but `123456` will verify any test number!

---

## 🔑 Step 6: Enable Google Play Billing (Optional, for production)

### 6.1 Production Only
- For development: No setup needed (billing disabled)
- For production: Link Google Play account in Firebase
- Skippable now - implement later

---

## 🔧 Step 7: Update App Configuration (3 min)

### 7.1 Verify google-services.json is in place
```bash
ls -la /Users/qa/eclipse-workspace/vibecheck/app/google-services.json
# Should show the file exists
```

### 7.2 Build App with Real Firebase
```bash
cd /Users/qa/eclipse-workspace/vibecheck

# Clean build (remove old artifacts)
./gradlew clean

# Build without fake data
./gradlew -PuseFakeData=false assembleDebug
```

### 7.3 Install on Device
```bash
adb install -r /Users/qa/eclipse-workspace/vibecheck/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.vibecheck.app/.MainActivity
```

---

## 🧪 Step 8: Test Everything (5 min)

### 8.1 Onboarding Test
1. App opens → Welcome screen ✅
2. Age verification → Select age ✅
3. Finish → Enter username ✅
4. Phone Input → Enter `+1 234 567 8900` (test number) ✅
5. Click **Send OTP** → No SMS sent (test mode) ✅
6. OTP Verification → Enter `123456` ✅
7. Profile Creation → Enter name ✅
8. **✅ Login complete!**

### 8.2 Verify Data in Firestore
1. Go to Firebase Console → **Firestore Database**
2. Look for **users** collection
3. Click on user document
4. Should see:
   ```
   firstName: "John"
   lastName: "Doe"
   phoneNumber: "234567890"
   countryCode: "+1"
   createdAtMillis: 1234567890
   ```

### 8.3 Test Feed (Create Post)
1. Tap **Feed** tab
2. Enter mood + text
3. Click **Post**
4. Go to Firestore → **resonance_posts** collection
5. Should see your post saved! ✅

### 8.4 Test Avatar Upload
1. Tap **Settings**
2. Upload avatar image
3. Go to Firebase Console → **Storage**
4. Should see `avatars/[userId].jpg` ✅

---

## 📊 Expected Firestore Structure

After testing, you should see:

```
Firestore Database
├── users/
│   └── [userId]
│       ├── firstName: "John"
│       ├── lastName: "Doe"
│       ├── phoneNumber: "234567890"
│       ├── countryCode: "+1"
│       ├── createdAtMillis: 1234567890
│       └── friends/ (subcollection, auto-created when needed)
│
├── resonance_posts/
│   └── [postId]
│       ├── id: "..."
│       ├── mood: "HAPPY"
│       ├── text: "Great day"
│       ├── imageUrl: "https://..."
│       ├── resonateCount: 0
│       └── createdAtMillis: 1234567890
│
├── daily_quests/
│   └── [dateKey]
│       ├── userId: "..."
│       ├── quests: [...]
│       └── completedCount: 0
│
├── friend_requests/
│   └── (auto-created when friends added)
│
└── leaderboard/
    └── (auto-created when quests completed)
```

---

## 🛡️ Step 9: Security Rules (For Development)

### 9.1 Current Test Mode Rules
✅ Allow all reads and writes (for testing)  
⚠️ **NOT for production!**

### 9.2 Firestore Rules Update (Optional for now)
In Firebase Console → **Firestore Database** → **Rules**

Replace with:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can only read/write their own data
    match /users/{userId} {
      allow read: if request.auth.uid == userId;
      allow write: if request.auth.uid == userId;
      
      // Friends subcollection
      match /friends/{friendId} {
        allow read: if request.auth.uid == userId;
        allow write: if request.auth.uid == userId;
      }
    }
    
    // Feed posts (public read, authenticated write)
    match /resonance_posts/{postId} {
      allow read: if request.auth.uid != null;
      allow create: if request.auth.uid == request.resource.data.authorId;
      allow update: if request.auth.uid == resource.data.authorId;
    }
    
    // Quests (user only)
    match /daily_quests/{questId} {
      allow read, write: if request.auth.uid == resource.data.userId;
    }
  }
}
```

Then click **Publish**

### 9.3 Storage Rules (Optional for now)
In Firebase Console → **Storage** → **Rules**

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

Then click **Publish**

---

## ✅ Checklist: What's Now Real

- [ ] Firebase project created
- [ ] google-services.json downloaded and placed in `app/`
- [ ] Firestore database enabled
- [ ] Cloud Storage bucket created
- [ ] Phone authentication enabled with test numbers
- [ ] App built with `-PuseFakeData=false`
- [ ] App installed on device
- [ ] Onboarding completed (real Firebase auth)
- [ ] User profile saved in Firestore
- [ ] Avatar uploaded to Storage
- [ ] Feed post saved in Firestore
- [ ] Data verified in Firebase Console

---

## 🔄 Testing Real Data Flow

### Complete User Journey:
1. **Phone Auth** → Firebase Phone Auth (test SMS)
2. **Profile Creation** → Firestore `users/` collection
3. **Feed Post** → Firestore `resonance_posts/` collection
4. **Avatar Upload** → Firebase Storage
5. **Friend Request** → Firestore `friend_requests/` collection
6. **Quest Completion** → Firestore `daily_quests/` collection
7. **Leaderboard** → Firestore `leaderboard/` collection

All data **persists** and **syncs** in real-time!

---

## 🚀 Next Steps (After Setup)

### For Development:
- Keep test mode enabled (unlimited free quota)
- Test all features
- Monitor Firestore usage in console

### For Production:
- Update security rules (not test mode)
- Enable reCAPTCHA for phone auth
- Set up billing and quotas
- Enable Cloud Audit Logs
- Set up backups

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| **google-services.json not found** | Download from Firebase Console, place in `app/` |
| **OTP not arriving** | Using test numbers? Use test OTP `123456` |
| **App crashes on login** | Check `google-services.json` exists and is valid |
| **Data not in Firestore** | Check app is built with `-PuseFakeData=false` |
| **Images not uploading** | Check Storage bucket created and test mode enabled |
| **Firestore quota exceeded** | Delete test data, upgrade plan, or wait 24 hours |

---

## 📞 Support

If you get stuck:
1. Check Firebase Console → **Logs** for errors
2. Check app logs: `adb logcat | grep Firebase`
3. Verify `google-services.json` is valid JSON
4. Try rebuilding: `./gradlew clean -PuseFakeData=false assembleDebug`

---

## 🎉 Success!

When everything works:
- ✅ Real user authentication
- ✅ Real data persistence
- ✅ Real image storage
- ✅ Real friend connections
- ✅ Real mood tracking
- ✅ **Zero dummy data!**

---

**Total Setup Time: ~30 minutes**  
**Next: Production Security Setup (after testing)**

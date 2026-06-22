# Real Data Setup - Quick Checklist ✅

## 🚀 5-Minute Quick Start

### 1️⃣ Create Firebase Project
```
[ ] Go to https://console.firebase.google.com/
[ ] Click "Create Project"
[ ] Name: "VibeCheck"
[ ] Create
```

### 2️⃣ Get Android Configuration
```
[ ] Go to Project Settings
[ ] Find "Your apps" → Android
[ ] Download google-services.json
[ ] Move to: app/google-services.json
```

### 3️⃣ Enable Services
```
[ ] Firestore Database → Create Database (test mode)
[ ] Authentication → Enable Phone
[ ] Storage → Create bucket
```

### 4️⃣ Update App Code
```
[ ] Verify app/google-services.json exists
[ ] Check DefaultAppContainer.kt has RealFriendshipRepository
[ ] Ensure FirebaseProvider is initialized
```

### 5️⃣ Build & Run
```bash
./gradlew -PuseFakeData=false assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.vibecheck.app/.MainActivity
```

---

## 📋 What Gets Real

| Feature | Before (Fake) | After (Real) |
|---------|---|---|
| Phone Auth | Random UUID | Firebase Phone Auth + SMS OTP |
| User Data | In-memory | Firestore Database |
| Avatars | Local cache | Firebase Storage |
| Feed Images | Temp files | Firebase Storage |
| Friend Search | Hardcoded users | Firestore query |
| Quests | Random gen | Firestore daily_quests collection |
| Leaderboard | Demo data | Firestore leaderboard collection |

---

## 🔑 API Keys You'll Need

1. **google-services.json** - Downloaded from Firebase Console
2. **Project ID** - From Firebase Project Settings
3. **Storage Bucket** - Auto-created with Firebase project

---

## ✨ What Works Immediately

✅ Phone verification (real SMS)  
✅ User profile creation (real database)  
✅ Avatar uploads (real storage)  
✅ Friend searches (real Firestore queries)  
✅ Post submissions (real database)  

---

## ⚠️ Important Notes

- **Costs**: Phone auth SMS costs ~$0.01 per message
- **Test Numbers**: Use Firebase Console test numbers during dev (free SMS)
- **Security**: Update Firestore rules before production
- **Billing**: Enable billing in Google Cloud for SMS
- **Test OTP**: Set test phone numbers with OTP "123456" in Firebase Console

---

## 🔧 Switching Back to Fake (if needed)

```bash
./gradlew -PuseFakeData=true assembleDebug
```

---

## ✅ Verify Setup

```bash
# Check google-services.json exists
ls -la app/google-services.json

# Check build config
grep "USE_FAKE_DATA" app/build.gradle.kts

# Run the app
./gradlew -PuseFakeData=false installDebug
```

---

## 🎯 Common Issues & Fixes

| Problem | Fix |
|---------|-----|
| `google-services.json not found` | Download from Firebase Console, place in `app/` |
| `OTP not sending` | Enable Phone auth in Firebase, enable billing |
| `Images not uploading` | Check Storage bucket created, rules allow write |
| `Firestore errors` | Check security rules, enable Firestore in Firebase |
| `Auth fails` | Ensure phone number format is `+[country_code][number]` |

---

## 📞 Next Steps

1. ✅ Create Firebase project (5 min)
2. ✅ Download google-services.json (1 min)
3. ✅ Enable services in Firebase (3 min)
4. ✅ Build app with `-PuseFakeData=false` (2 min)
5. ✅ Test onboarding with real phone auth (5 min)

**Total Time: ~15 minutes** ⏱️

---

For detailed setup with security rules, see **FIREBASE_SETUP.md**

# Subscription Setup - Complete Guide

## 🎯 Overview

VibeCheck Plus subscription:
- **Price:** $29.99/month (or £29.99 in UK)
- **Features:** 
  - CSV export of mood history
  - Advanced insights
  - Priority support
- **Payment:** Google Play Billing v7

**Total Setup Time: ~45 minutes**

---

## 📋 Prerequisites

Before starting, ensure you have:
- ✅ Google Play Console account
- ✅ Google Play app registered
- ✅ Firebase project (from earlier setup)
- ✅ App built and running on device

---

## ✅ Step 1: Google Play Console Setup (10 min)

### 1.1 Open Google Play Console
```
Go to: https://play.google.com/console/
Sign in with Google account that will manage the app
```

### 1.2 Register App
1. Click **Create app**
2. App name: `VibeCheck`
3. Default language: English
4. App or game: Select **App**
5. Click **Create app**

### 1.3 Fill Required Info
In **App content** section:
- [ ] App category: Health & fitness
- [ ] Contact email: dipen@vasundharainfotechllp.com
- [ ] Privacy policy URL: (add later)
- [ ] Content rating: Rate the app

In **Locations** section:
- [ ] Select countries to distribute to
- [ ] Include US and UK minimum

---

## 💰 Step 2: Create Subscription Product (10 min)

### 2.1 Create Subscription
1. In Google Play Console, go to **Products** → **Subscriptions**
2. Click **Create subscription**
3. Fill in:
   - **Product ID:** `vibecheck_plus_monthly` (must be lowercase)
   - **Name:** `VibeCheck Plus Monthly`
   - **Description:** `Unlimited insights, CSV export, priority support`

### 2.2 Set Pricing
1. Click **Add a new price**
2. Set billing period: **Monthly**
3. Add prices:
   ```
   USD: $29.99
   GBP: £29.99
   ```
4. Set free trial: 7 days (optional)
5. Click **Save**

### 2.3 Set Renewal Settings
1. Renewal period: **1 month**
2. Grace period: **3 days** (time to fix failed payment)
3. Account hold period: **7 days** (time to retry payment)
4. Resubscription proration: Enable
5. Click **Save**

---

## 🔗 Step 3: Link Firebase to Google Play (5 min)

### 3.1 Get Credentials
1. Google Play Console → **Setup** → **API access**
2. Click **Create service account**
3. This opens Google Cloud Console
4. Create service account with:
   - Name: `vibecheck-play-services`
   - Description: `VibeCheck Play Billing`
5. Create JSON key (you'll download it)
6. Save the JSON file

### 3.2 Add Service Account to Google Play
1. Back to Google Play Console
2. Go to **Setup** → **Users and permissions**
3. Click **Invite user**
4. Paste service account email
5. Grant permissions:
   - [x] View app information and download reports
   - [x] Manage orders and subscriptions
6. Save

### 3.3 Enable Play Billing API
1. Google Cloud Console → **APIs & Services** → **Library**
2. Search for `Google Play Android Developer API`
3. Click **Enable**

---

## 🔐 Step 4: Configure Firebase for Billing (5 min)

### 4.1 Link Google Play to Firebase
1. Firebase Console → **Project Settings** → **Integrations**
2. Click **Google Play** → **Link**
3. Select your Google Play app
4. Authorize Firebase to access Google Play
5. Save

### 4.2 Verify Connection
1. Firebase Console → **Billing repository**
2. Should show your subscription products:
   - `vibecheck_plus_monthly` ✅

---

## 📱 Step 5: Update Android App (10 min)

### 5.1 Check PlayBillingRepository Already Exists
```bash
# It should already be in the codebase:
ls -la app/src/main/java/com/vibecheck/app/billing/PlayBillingRepository.kt
```

This file handles:
- ✅ Connecting to Google Play Billing
- ✅ Querying subscription products
- ✅ Starting purchase flow
- ✅ Verifying purchases
- ✅ Restoring purchases

### 5.2 Verify Billing Dependency
```bash
# Check build.gradle.kts has:
grep "com.android.billingclient" app/build.gradle.kts

# Should show:
# implementation("com.android.billingclient:billing:7.0.0")
```

If not, add it:
```kotlin
// In app/build.gradle.kts
dependencies {
    implementation("com.android.billingclient:billing:7.0.0")
    // ...
}
```

### 5.3 Update Google Play Package Name
```bash
# In Firebase Console → Project Settings
# Ensure your Android package name is registered:
Package name: com.vibecheck.app
```

---

## 🧪 Step 6: Test Subscription (10 min)

### 6.1 Set Up Test Account
1. Google Play Console → **Users and permissions**
2. Click **Manage test accounts**
3. Click **Create test account**
4. Email: `yourname+vibecheck-test@gmail.com`
5. Save

### 6.2 Add Device as Tester
1. Google Play Console → **Internal testing**
2. Create **internal test release**
3. Upload your APK:
```bash
./gradlew -PuseFakeData=false assembleRelease
# Or upload existing debug APK
```
4. Share test link with test account
5. Install from link on device

### 6.3 Test Purchase Flow
1. Open app with test account
2. Go to Settings → **Upgrade to Plus**
3. Click **Subscribe**
4. Google Play shows test pricing (no real charge)
5. Complete purchase
6. App should show subscription active ✅

---

## 💾 Step 7: Backend Verification (optional, for production)

### 7.1 Create Cloud Function for Receipt Validation
```javascript
// Firebase Cloud Function
exports.validatePurchase = functions.https.onCall(async (data, context) => {
  const packageName = 'com.vibecheck.app'
  const subscriptionId = 'vibecheck_plus_monthly'
  const purchaseToken = data.purchaseToken
  
  // Verify purchase with Google Play API
  const androidPublisher = google.androidpublisher('v3')
  
  const result = await androidPublisher.purchases.subscriptions.get({
    packageName: packageName,
    subscriptionId: subscriptionId,
    token: purchaseToken,
  })
  
  if (result.data.paymentState === 1) { // Payment received
    // Grant subscription benefits
    await admin.firestore()
      .collection('users')
      .doc(context.auth.uid)
      .update({ subscriptionActive: true })
    return { valid: true }
  }
  
  return { valid: false }
})
```

---

## 📊 Expected Firestore Structure for Billing

After purchase, your user document should have:

```javascript
users/{userId}
├── firstName: "John"
├── lastName: "Doe"
├── subscriptionActive: true          // NEW
├── subscriptionStatus: "active"      // NEW
├── subscriptionExpiry: 1234567890    // NEW
├── lastReceiptToken: "..."           // NEW
└── ... (other fields)
```

---

## 🚀 Step 8: Code Integration Check

### 8.1 Check PlayBillingRepository Implementation
```bash
grep -n "vibecheck_plus_monthly" app/src/main/java/com/vibecheck/app/billing/PlayBillingRepository.kt
```

Should show the product ID is configured.

### 8.2 Check Settings Screen Has Upgrade Option
```bash
grep -n "Upgrade\|Subscribe\|Plus" app/src/main/java/com/vibecheck/app/ui/settings/SettingsScreen.kt
```

Should show subscription UI.

### 8.3 Verify AppContainer Has BillingRepository
```bash
grep -n "BillingRepository\|PlayBillingRepository" app/src/main/java/com/vibecheck/app/data/DefaultAppContainer.kt
```

Should be wired up.

---

## ✅ Testing Checklist

### Desktop/Console Tests
- [ ] Google Play Console app created
- [ ] Subscription product created (`vibecheck_plus_monthly`)
- [ ] Pricing set ($29.99 USD, £29.99 GBP)
- [ ] Free trial enabled (7 days)
- [ ] Service account created
- [ ] Firebase linked to Google Play
- [ ] Play Billing API enabled

### Device Tests
- [ ] Internal test release created
- [ ] Test account added
- [ ] Device can access test build
- [ ] Test purchase flow works
- [ ] Subscription shows as active
- [ ] CSV export works (if Plus)
- [ ] Insights show premium data
- [ ] Receipt token saved in Firestore

---

## 🔄 Subscription Features in App

### Settings Screen
```
┌─────────────────────────────┐
│ VibeCheck Plus              │
│ $29.99/month                │
│ ✓ CSV Export                │
│ ✓ Advanced Insights         │
│ ✓ Priority Support          │
│ [Subscribe] [Restore]       │
└─────────────────────────────┘
```

### CSV Export (Plus Only)
```
If subscriptionActive == true:
  - Show "Export to CSV" button
  - Generate CSV from mood history
  - Email or download CSV file
```

### Advanced Insights (Plus Only)
```
If subscriptionActive == true:
  - Show detailed patterns
  - Mood predictions
  - Correlation analysis
```

---

## 🛡️ Security Considerations

### 1. Verify Purchases in Backend
```kotlin
// In Cloud Function, verify receipt token
val androidPublisher = google.androidpublisher('v3')
val receipt = androidPublisher.purchases.subscriptions.get(...)

// Don't trust client-side claims
```

### 2. Validate on Each Request
```kotlin
// Before serving Plus features:
if (!isSubscriptionActive(userId)) {
    throw Exception("Not a Plus subscriber")
}
```

### 3. Handle Expiry
```kotlin
// Check subscription expiry date:
if (System.currentTimeMillis() > subscriptionExpiry) {
    deactivateSubscription(userId)
}
```

---

## 📱 Production Checklist

Before going to production:

### Pre-Launch
- [ ] Subscription tested thoroughly
- [ ] Pricing correct for all regions
- [ ] Free trial period set
- [ ] Privacy policy updated
- [ ] Terms of service updated
- [ ] Support email configured
- [ ] Analytics tracking added

### Launch
- [ ] Create production release in Google Play Console
- [ ] Upload signed APK
- [ ] Set pricing
- [ ] Add screenshots
- [ ] Add description
- [ ] Target API 35+
- [ ] Request app review

### Post-Launch
- [ ] Monitor subscription metrics
- [ ] Set up refund policy
- [ ] Monitor failed payments
- [ ] Set up retention campaigns

---

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| **Product not showing in app** | Verify product ID matches exactly, refresh cache |
| **Purchase fails** | Check test account has Google Play Services, billing enabled |
| **Can't verify purchase** | Ensure service account has correct permissions |
| **Subscription not persisting** | Check Firestore update succeeded, verify expiry date |
| **Grace period not working** | Verify account hold period set correctly |

---

## 📊 Monitoring Subscriptions

### In Google Play Console
1. **Revenue dashboard** - Track MRR (Monthly Recurring Revenue)
2. **Subscription metrics** - Active, churn, retention rates
3. **Payment issues** - Failed payments, retries
4. **User reviews** - Monitor feedback

### In Firebase
1. **Firestore** - Check subscriptionActive flag
2. **Analytics** - Track conversion funnel
3. **Crash reporting** - Monitor purchase errors

---

## 🎯 Revenue Model

**VibeCheck Plus:** $29.99/month
```
Monthly Users  │ Annual Revenue
────────────────────────────
100 users      │ $36,000
500 users      │ $180,000
1,000 users    │ $360,000
5,000 users    │ $1,800,000
```

Target: 10% conversion rate = sustainable model

---

## 📚 Resources

- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Google Play Billing Library](https://developer.android.com/google/play/billing)
- [Firebase Billing Setup](https://firebase.google.com/docs/analytics/plan-your-implementation)

---

**After Setup: Recurring Revenue Stream!** 💰

Total setup time: ~45 minutes

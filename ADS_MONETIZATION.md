# Ads & Monetization Guide - VibeCheck

## 🎯 Overview

VibeCheck has a **dual monetization model**:

1. **Free Users** → See ads (Google AdMob)
2. **Plus Subscribers** → No ads (ad-free experience)

**This maximizes revenue:** Free users see ads, paying users get premium experience.

---

## 💰 Revenue Model Breakdown

```
VibeCheck Total Revenue
├─ Subscription Revenue (70%)
│  └─ Plus Subscribers: $29.99/month
│
└─ Ad Revenue (30%)
   └─ Free Users: Ad impressions + clicks
```

### **Example Revenue Mix (1,000 users)**

```
Subscribers:    100 users × $29.99 = $3,000/month (70%)
Free Users:     900 users × $3-5 ads/month = $2,700-4,500/month (30%)
────────────────────────────────────────────
Total Monthly:  $5,700-7,500
Your Net:       $4,000-5,250 (after Google Play 30%)
```

---

## 📱 Ad Implementation Strategy

### **Where Ads Appear**

```
VibeCheck Free Tier:
├─ Check-in Screen
│  └─ Banner ad after check-in
├─ Feed (Resonance Feed)
│  ├─ Banner every 5 posts
│  ├─ Interstitial after 10 scrolls
│  └─ Native ads embedded in feed
├─ Gauntlet (Quests)
│  └─ Banner below quest completion
├─ Settings
│  └─ "Upgrade to Plus" CTA
└─ Friends
   └─ Occasional banner

Plus Subscribers:
└─ No ads anywhere ✅
```

### **Where Ads DON'T Appear**

```
❌ Never show ads during:
  - Onboarding
  - OTP verification
  - Crisis/mental health resources
  - Therapy recommendations
  - Urgent mood check-ins
```

---

## 🔧 Step 1: Google AdMob Setup (15 min)

### 1.1 Create AdMob Account
```
Go to: https://admob.google.com/
Sign in with Google account
Select: "Sign up for AdMob"
```

### 1.2 Register Your App
```
1. AdMob → Apps → Add app
2. App name: VibeCheck
3. Platform: Android
4. Store URL: (leave empty for now)
5. Create app
```

### 1.3 Create Ad Units

**Banner Ad (for Feed & Check-in)**
```
1. Apps → VibeCheck → Ad units → Create
2. Type: Banner
3. Name: "Feed Banner"
4. Banner size: Adaptive
5. Save → Copy Ad Unit ID
   Example: ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

**Interstitial Ad (between screens)**
```
1. Apps → VibeCheck → Ad units → Create
2. Type: Interstitial
3. Name: "Feed Interstitial"
4. Save → Copy Ad Unit ID
```

**Rewarded Ad (optional, for quest bonuses)**
```
1. Apps → VibeCheck → Ad units → Create
2. Type: Rewarded
3. Name: "Bonus Gems Reward"
4. Save → Copy Ad Unit ID
```

### 1.4 Get Your Ad Unit IDs

Save these:
```
BANNER_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/1111111111"
INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/2222222222"
REWARDED_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/3333333333"
```

---

## 🛠️ Step 2: Add Google Mobile Ads SDK (10 min)

### 2.1 Update build.gradle.kts

```kotlin
// In app/build.gradle.kts
dependencies {
    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:22.6.0")
}
```

### 2.2 Update AndroidManifest.xml

```xml
<!-- In app/src/main/AndroidManifest.xml -->
<manifest>
    <!-- Add permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application>
        <!-- Add AdMob App ID (get from AdMob console) -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxxxxxxxxxx" />
    </application>
</manifest>
```

### 2.3 Initialize Mobile Ads in MainActivity

```kotlin
// In MainActivity.kt
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this)
        
        // Rest of onCreate...
    }
}
```

---

## 💻 Step 3: Create Ad Manager Repository (20 min)

### 3.1 Create AdManager Interface

```kotlin
// app/src/main/java/com/vibecheck/app/ads/AdManager.kt

package com.vibecheck.app.ads

import com.google.android.gms.ads.interstitial.InterstitialAd

interface AdManager {
    // Show banner ad
    fun showBannerAd(container: ViewGroup)
    
    // Load and show interstitial ad
    suspend fun showInterstitialAd(): Result<Unit>
    
    // Load rewarded ad (optional)
    suspend fun loadRewardedAd(): Result<Unit>
    
    // Check if user is ad-free (Plus subscriber)
    fun isAdFree(): Boolean
}
```

### 3.2 Create Real AdManager (with AdMob)

```kotlin
// app/src/main/java/com/vibecheck/app/ads/RealAdManager.kt

package com.vibecheck.app.ads

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.vibecheck.app.data.BillingRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RealAdManager(
    private val context: Context,
    private val billingRepository: BillingRepository,
) : AdManager {

    private var interstitialAd: InterstitialAd? = null

    // Ad Unit IDs (replace with your real IDs)
    companion object {
        const val BANNER_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/1111111111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/2222222222"
    }

    override suspend fun isAdFree(): Boolean {
        return billingRepository.isSubscribed.first()
    }

    override fun showBannerAd(container: ViewGroup) {
        // Only show if user is not Plus subscriber
        val scope = rememberCoroutineScope()
        scope.launch {
            if (isAdFree()) {
                container.removeAllViews()
                return@launch
            }

            val adView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
            }

            container.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    }

    override suspend fun showInterstitialAd(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val isAdFree = runBlocking { isAdFree() }
            if (isAdFree) {
                continuation.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }

            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        // Show ad
                        // (requires Activity, call from UI)
                        continuation.resume(Result.success(Unit))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        continuation.resume(Result.success(Unit)) // Don't fail, just skip
                    }
                }
            )
        }

    override suspend fun loadRewardedAd(): Result<Unit> = runCatching {
        if (isAdFree()) return@runCatching

        // Load rewarded ad for bonus features
        // Implementation similar to interstitial
    }
}
```

### 3.3 Create Fake AdManager (for testing)

```kotlin
// app/src/main/java/com/vibecheck/app/ads/FakeAdManager.kt

package com.vibecheck.app.ads

import android.view.ViewGroup

class FakeAdManager : AdManager {
    override fun showBannerAd(container: ViewGroup) {
        // No-op in fake mode
    }

    override suspend fun showInterstitialAd(): Result<Unit> =
        Result.success(Unit)

    override suspend fun loadRewardedAd(): Result<Unit> =
        Result.success(Unit)

    override fun isAdFree(): Boolean = false // Show ads in test
}
```

---

## 🎬 Step 4: Integrate Ads into Screens (20 min)

### 4.1 Banner Ad in Feed

```kotlin
// In ResonanceFeedScreen.kt

@Composable
fun ResonanceFeedScreen(container: AppContainer) {
    val adManager = remember { container.adManager }
    var isAdFree by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAdFree = adManager.isAdFree()
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            items(posts.size) { idx ->
                PostCard(posts[idx])
                
                // Show banner every 5 posts
                if ((idx + 1) % 5 == 0 && !isAdFree) {
                    BannerAdView(adManager)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BannerAdView(adManager: AdManager) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                adManager.showBannerAd(this)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
```

### 4.2 Interstitial Ad Between Screens

```kotlin
// When navigating between major screens

val adManager = container.adManager
scope.launch {
    adManager.showInterstitialAd()
    // Then navigate
    navigateToNextScreen()
}
```

### 4.3 Rewarded Ad (Optional: Bonus Gems)

```kotlin
// In Gauntlet, offer bonus gems for watching ad

@Composable
fun BonusGemsButton(adManager: AdManager) {
    Button(onClick = {
        scope.launch {
            adManager.loadRewardedAd().onSuccess {
                // Give user 10 bonus gems
                addGemsToUser(10)
                showSnackbar("🎁 You earned 10 bonus gems!")
            }
        }
    }) {
        Text("Watch Ad for 10 Gems")
    }
}
```

---

## 📊 Step 5: Configure Ad Networks (5 min)

### 5.1 Ad Network Setup in AdMob

```
AdMob Console → Ad units → Your app
├─ Mediation → Add networks
├─ Google Ads Network (Auto)
├─ Facebook Audience Network (Optional)
├─ Amazon Publisher Services (Optional)
└─ AppLovin (Optional)
```

### 5.2 Mediation Strategy

```
Primary: Google Ads Network (50% fill rate)
Secondary: Facebook Ads (20% fill rate)
Tertiary: Amazon & AppLovin (30% fill rate combined)

This ensures you always have ads to show (high fill rate)
```

---

## 💵 Ad Revenue Projections

### **CPM vs. CPC Rates**

```
CPM (Cost Per 1,000 Impressions):
├─ Health & Wellness apps: $2-8 CPM
├─ VibeCheck average: ~$4-6 CPM
└─ Peak (holidays): ~$8-12 CPM

CPC (Cost Per Click):
├─ Health app average: $0.30-0.60
└─ Premium brands: up to $1.00
```

### **Revenue Example: 1,000 Free Users**

```
Scenario: 900 free users + 100 Plus subscribers

Free User Engagement:
├─ Daily active: 450 users (50% of 900)
├─ Feed views per day: 15 posts × 3 sessions = 45 impressions
├─ Ads shown: Every 5 posts = 9 ads/user/day
├─ Total daily impressions: 450 × 9 = 4,050 impressions
├─ Daily revenue: 4,050 / 1,000 × $5 CPM = $20.25
└─ Monthly revenue: $20.25 × 30 = $607.50

Subscriber Revenue:
├─ 100 subscribers × $29.99 = $2,999/month
├─ Google Play takes 30%: -$900
└─ Your take: $2,099

Ads Revenue:
├─ Monthly: $607.50
├─ Google takes 32%: -$194
└─ Your take: $413.50

Total Monthly (Ad + Subscription):
├─ Ads: $413.50
├─ Subscriptions: $2,099
└─ TOTAL: $2,512.50/month
```

---

## 🎯 Ad Placement Strategy

### **GOOD Places for Ads**

```
✅ After check-in completed
✅ Between feed posts (every 5-10)
✅ Bottom of quest completion
✅ Before navigating away from screen
✅ Interstitial before sharing mood
✅ Rewarded: bonus gems for watching
```

### **BAD Places for Ads**

```
❌ During onboarding
❌ During crisis/emergency features
❌ Blocking mood entry
❌ Interrupting active sessions
❌ On every screen (annoying)
❌ When user is typing
❌ During OTP verification
```

---

## 📈 Optimizing Ad Revenue

### **1. Increase Impressions**
```
Strategy: Show more ads → more impressions
├─ Add banners in more locations
├─ More interstitials between screens
└─ Result: +30-50% revenue (but risk user experience)
```

### **2. Improve CTR (Click-Through Rate)**
```
Native Ads: Blend with content (higher CTR)
├─ Embedded ads in feed look like posts
├─ Users don't realize it's an ad
├─ CTR: 3-5% (vs. banner 0.5-1%)
└─ Revenue: 5-10x higher per impression
```

### **3. Target High-Value Users**
```
Geo-targeting: Show ads based on location
├─ US/UK users: Higher CPM ($5-8)
├─ Asia/Africa: Lower CPM ($1-3)
└─ Strategy: Premium ads for premium regions
```

### **4. Premium Ad Slots**
```
Charge advertisers more for:
├─ First-position ads (seen first)
├─ Pause-screen ads (captive audience)
├─ Notification ads
└─ Result: +100% CPM premium
```

---

## 🚫 Ad Fatigue & User Experience

### **The Balance**

```
Too Few Ads:
├─ Low revenue
└─ User happy

Sweet Spot (🎯):
├─ 1-3 ads per session
├─ Clear distinction between ads & content
└─ High revenue + happy users

Too Many Ads:
├─ Users uninstall app
├─ Negative reviews
├─ Lower revenue from fewer users
└─ Ad networks punish low-quality placements
```

### **Best Practice: 1 Ad Per Session**

```
User opens app:
├─ Check mood: No ads
├─ View feed: 1 banner
├─ Submit post: No ads
├─ View quests: 1 interstitial
├─ Complete quest: No ads
└─ Total: 2 ads per 5-minute session (acceptable)
```

---

## 💡 Monetization Optimization

### **Tier 1: Free (Most Users)**
```
├─ Ads everywhere
├─ Limited quests (1 per day)
├─ No CSV export
├─ Basic insights
└─ Revenue: $0-1/month per user (ads)
```

### **Tier 2: Plus ($29.99/month)**
```
├─ No ads
├─ Unlimited quests
├─ CSV export
├─ Advanced insights
└─ Revenue: $21/month per user (70% of subscription)
```

### **Tier 3: Premium Ads (Future)**
```
├─ Display ads to health brands
├─ Sponsored quests/insights
├─ "Learn more" health content
└─ Revenue: +$1-2/month per user
```

---

## 📊 Expected Ad Revenue Timeline

```
Month 1 (100 users, 90% free):
├─ Daily active: 45 users
├─ Monthly impressions: 121,500
├─ Revenue: ~$607.50
└─ Per free user: ~$0.67/month

Month 6 (1,000 users, 85% free):
├─ Daily active: 425 users
├─ Monthly impressions: 1,215,000
├─ Revenue: ~$6,075
└─ Per free user: ~$0.72/month

Month 12 (5,000 users, 80% free):
├─ Daily active: 2,000 users
├─ Monthly impressions: 5,400,000
├─ Revenue: ~$27,000
└─ Per free user: ~$0.68/month
```

---

## 🔒 Privacy & Compliance

### **IMPORTANT: User Privacy**

```
❌ DON'T track users for ad targeting without consent
❌ DON'T sell user mood data to advertisers
❌ DON'T use mood data to target health ads

✅ DO use contextual targeting (mood app → wellness ads)
✅ DO anonymous aggregate data
✅ DO respect GDPR/COPPA (under 13)
✅ DO ask for permission before tracking
```

### **Ad Network Privacy**

```
Google AdMob: Handles privacy automatically
├─ GDPR compliant
├─ COPPA compliant (under 13)
├─ No data sharing required
└─ Respects "Personalized Ads" settings
```

---

## 🎯 Implementation Timeline

```
Week 1:
├─ Set up AdMob account
├─ Create ad units
└─ Add Mobile Ads SDK

Week 2:
├─ Create AdManager repository
├─ Implement banner ads
└─ Test on device

Week 3:
├─ Add interstitial ads
├─ Implement mediation
└─ Monitor revenue

Week 4:
├─ Optimize placement
├─ A/B test ad formats
└─ Monitor user retention
```

---

## 💰 Combined Monetization Revenue

```
1,000 Total Users (800 free, 200 Plus):

Subscription Revenue:
├─ 200 × $29.99 = $5,998/month
├─ Google Play (30%): -$1,799.40
└─ Your take: $4,198.60/month

Ad Revenue (from 800 free users):
├─ 800 × $0.70 = $560/month
├─ Google takes (32%): -$179.20
└─ Your take: $380.80/month

────────────────────────────
TOTAL MONTHLY: $4,579.40
TOTAL ANNUAL: $54,952.80
```

---

## 🚀 Next Steps

1. ✅ Create AdMob account
2. ✅ Add Mobile Ads SDK to project
3. ✅ Create AdManager repository
4. ✅ Integrate ads into screens
5. ✅ Test on real device
6. ✅ Monitor revenue & user experience
7. ✅ Optimize placement & frequency

---

**Ads are a high-leverage monetization lever!** 💰

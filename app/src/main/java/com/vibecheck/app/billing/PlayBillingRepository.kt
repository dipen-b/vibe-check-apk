package com.vibecheck.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.data.BillingRepository
import com.vibecheck.app.data.firebase.FirebaseProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real [BillingRepository] backed by Google Play Billing 7 (monthly subscription
 * [AppConfig.SUBSCRIPTION_PRODUCT_ID]). Constructor signature is part of the data-layer
 * contract: `PlayBillingRepository(context, externalScope)`.
 *
 * State model: [isSubscribed] mirrors the local Play purchase state optimistically.
 * Per CONTRACTS.md the authoritative entitlement is server-recorded — the backend
 * `validatePurchase` callable writes `users/{uid}.plusUntil`. The Functions provider
 * now exists (FirebaseProvider.functions); wiring the callable into
 * [acknowledgeIfNeeded] is the remaining step. Until then this is a client-trusted
 * mirror, which is acceptable for gating cosmetic premium UI.
 */
class PlayBillingRepository(
    context: Context,
    private val externalScope: CoroutineScope,
) : BillingRepository, PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: Flow<Boolean> = _isSubscribed.asStateFlow()

    private val _monthlyPrice = MutableStateFlow<String?>(null)
    override val monthlyPriceFormatted: Flow<String?> = _monthlyPrice.asStateFlow()

    @Volatile
    private var cachedProduct: ProductDetails? = null

    // Serialise entitlement writes (onPurchasesUpdated vs queryEntitlement) and
    // concurrent refresh() calls (startup + Restore) so neither clobbers the other.
    private val entitlementLock = Mutex()
    private val refreshLock = Mutex()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    init {
        // Learn entitlement + price at startup so the UI reflects reality on launch.
        externalScope.launch { runCatching { refresh() } }
    }

    override suspend fun refresh() = refreshLock.withLock {
        if (!ensureConnected()) return@withLock
        loadProductDetails()
        queryEntitlement()
    }

    override suspend fun launchPurchase(activity: Activity): Result<Unit> {
        if (!ensureConnected()) {
            return Result.failure(IllegalStateException("Google Play billing is unavailable. Please try again later."))
        }
        if (cachedProduct == null) loadProductDetails()
        val product = cachedProduct
            ?: return Result.failure(IllegalStateException("Subscription isn't available right now."))
        val offerToken = product.basePlanOffer()?.offerToken
            ?: return Result.failure(IllegalStateException("No subscription offer is configured."))

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        // Success here only means the Play sheet launched; the actual result
        // arrives asynchronously in onPurchasesUpdated.
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingResponseCode.OK) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(result.debugMessage.ifBlank { "Couldn't start the purchase." }))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && purchases != null) {
            externalScope.launch {
                purchases.forEach { acknowledgeIfNeeded(it) }
                // Only ever promotes to subscribed here; demotion is decided by
                // the authoritative queryEntitlement(). Lock serialises with it.
                if (purchases.any { it.isActive() }) {
                    entitlementLock.withLock { _isSubscribed.value = true }
                }
            }
        }
        // USER_CANCELED and other codes leave the current state untouched; the
        // SubscriptionScreen surfaces the failure from launchPurchase().
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        // Bound the wait so a stuck Play connection can't hang refresh()/purchase.
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
        } ?: false
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(AppConfig.SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val product = result.productDetailsList?.firstOrNull()
            cachedProduct = product
            // Show the recurring price (last phase), never a trial/intro phase.
            _monthlyPrice.value = product?.basePlanOffer()?.recurringPrice()
        }
    }

    private suspend fun queryEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            // Filter to our SKU so unrelated subscriptions never grant entitlement.
            val purchases = result.purchasesList
                .filter { it.products.contains(AppConfig.SUBSCRIPTION_PRODUCT_ID) }
            purchases.forEach { acknowledgeIfNeeded(it) }
            // Server is authoritative (users/{uid}.plusUntil); local Play state is
            // an offline-friendly fallback so the UI isn't gated by a network hop.
            val localActive = purchases.any { it.isActive() }
            val serverActive = serverPlusActive()
            entitlementLock.withLock { _isSubscribed.value = serverActive || localActive }
        }
    }

    /** Reads the server-recorded entitlement written by the validatePurchase function. */
    private suspend fun serverPlusActive(): Boolean {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: return false
        return runCatching {
            val doc = FirebaseProvider.firestore.collection("users").document(uid).get().await()
            (doc.getLong("plusUntil") ?: 0L) > System.currentTimeMillis()
        }.getOrDefault(false)
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params)
        }
        // Record server-trusted entitlement (best-effort; local state still
        // gates the UI if this call can't reach the backend).
        runCatching {
            FirebaseProvider.functions.getHttpsCallable("validatePurchase").call(
                mapOf(
                    "productId" to AppConfig.SUBSCRIPTION_PRODUCT_ID,
                    "purchaseToken" to purchase.purchaseToken,
                )
            ).await()
        }
    }

    private fun Purchase.isActive(): Boolean =
        purchaseState == Purchase.PurchaseState.PURCHASED

    /**
     * Pick the base-plan offer deterministically. The base plan has a null
     * [ProductDetails.SubscriptionOfferDetails.getOfferId]; offers (trial/intro)
     * have a non-null one. Falling back to the last entry avoids defaulting into
     * a promotional offer if Play ever returns only offers.
     */
    private fun ProductDetails.basePlanOffer(): ProductDetails.SubscriptionOfferDetails? {
        val offers = subscriptionOfferDetails ?: return null
        return offers.firstOrNull { it.offerId == null } ?: offers.lastOrNull()
    }

    /** The ongoing recurring price = the final pricing phase (after any intro/trial). */
    private fun ProductDetails.SubscriptionOfferDetails.recurringPrice(): String? =
        pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}

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
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Real [BillingRepository] backed by Google Play Billing 7 (monthly subscription
 * [AppConfig.SUBSCRIPTION_PRODUCT_ID]). Constructor signature is part of the data-layer
 * contract: `PlayBillingRepository(context, externalScope)`.
 *
 * State model: [isSubscribed] mirrors the local Play purchase state optimistically.
 * Per CONTRACTS.md the authoritative entitlement is server-recorded — the backend
 * `validatePurchase` callable writes `users/{uid}.plusUntil`. That call should be made
 * from [acknowledgeIfNeeded] once a Functions provider is exposed; until then this is a
 * client-trusted mirror, which is acceptable for gating cosmetic premium UI.
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

    override suspend fun refresh() {
        if (!ensureConnected()) return
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
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
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
                if (purchases.any { it.isActive() }) _isSubscribed.value = true
            }
        }
        // USER_CANCELED and other codes leave the current state untouched; the
        // SubscriptionScreen surfaces the failure from launchPurchase().
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
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
            _monthlyPrice.value = product
                ?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.formattedPrice
        }
    }

    private suspend fun queryEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val purchases = result.purchasesList
            purchases.forEach { acknowledgeIfNeeded(it) }
            _isSubscribed.value = purchases.any { it.isActive() }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params)
        }
    }

    private fun Purchase.isActive(): Boolean =
        purchaseState == Purchase.PurchaseState.PURCHASED
}

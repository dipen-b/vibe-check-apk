package com.vibecheck.app.data

/**
 * Hand-rolled dependency container (no DI framework — the app is small and
 * this keeps the build simple). [com.vibecheck.app.VibeCheckApp] owns one
 * instance; screens receive it as a parameter.
 */
interface AppContainer {
    val profileRepository: ProfileRepository
    val moodRepository: MoodRepository
    val heatmapRepository: HeatmapRepository
    val resonanceRepository: ResonanceRepository
    val questRepository: QuestRepository
    val chatRepository: ChatRepository
    val billingRepository: BillingRepository
    val insightsRepository: InsightsRepository
    val microActionEngine: MicroActionEngine
    val friendshipRepository: FriendshipRepository
}

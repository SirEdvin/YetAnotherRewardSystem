package site.siredvin.yars.common.rewardshop

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.block.RewardShopBlock
import java.util.Collections
import java.util.IdentityHashMap

class RewardShopMerchant(
    private val player: Player,
    private val shopPos: BlockPos,
    private val shopId: ResourceLocation,
) : Merchant {
    private var tradingPlayer: Player? = player
    private val trades = IdentityHashMap<MerchantOffer, String>()
    private val approvedOffers = Collections.newSetFromMap(IdentityHashMap<MerchantOffer, Boolean>())
    private var offers = MerchantOffers()

    init {
        rebuildOffers()
    }

    override fun setTradingPlayer(player: Player?) {
        tradingPlayer = player
    }

    override fun getTradingPlayer(): Player? = tradingPlayer?.takeIf {
        it.distanceToSqr(shopPos.x + 0.5, shopPos.y + 0.5, shopPos.z + 0.5) <= 64.0 &&
            (it.level().getBlockState(shopPos).block as? RewardShopBlock)?.shopId == shopId
    }

    override fun getOffers(): MerchantOffers = offers

    override fun overrideOffers(offers: MerchantOffers) {
        this.offers = offers
    }

    override fun notifyTrade(offer: MerchantOffer) {
        if (!approvedOffers.remove(offer)) return
        val tradeId = trades[offer] ?: return
        RewardShopTradeHistory.increment(player, shopId, tradeId)
        rebuildOffers()
        player.sendMerchantOffers(player.containerMenu.containerId, offers, 1, 0, false, false)
    }

    fun approveTrade(offer: MerchantOffer?): Boolean {
        val tradeId = offer?.let(trades::get) ?: return false
        val trade = RewardShopTrades.find(shopId, tradeId) ?: return false
        val resolved = runCatching { trade.resolve(RewardShopTradeHistory.completed(player, shopId, tradeId)) }.getOrNull() ?: return false
        val matches = ItemStack.matches(offer.result, resolved.result) &&
            ItemStack.matches(offer.costA, resolved.firstCost) &&
            ItemStack.matches(offer.costB, resolved.secondCost ?: ItemStack.EMPTY)
        if (matches) approvedOffers += offer
        return matches
    }

    override fun notifyTradeUpdated(stack: ItemStack) = Unit

    override fun getVillagerXp(): Int = 0

    override fun overrideXp(xp: Int) = Unit

    override fun showProgressBar(): Boolean = false

    override fun getNotifyTradeSound(): SoundEvent = SoundEvents.WANDERING_TRADER_YES

    override fun isClientSide(): Boolean = player.level().isClientSide

    private fun rebuildOffers() {
        trades.clear()
        approvedOffers.clear()
        offers = MerchantOffers()
        RewardShopTrades.all(shopId).forEach { trade ->
            val resolved = try {
                trade.resolve(RewardShopTradeHistory.completed(player, shopId, trade.id))
            } catch (exception: RuntimeException) {
                YarsCore.LOGGER.error("Skipping invalid reward shop trade ${trade.id}", exception)
                null
            } ?: return@forEach
            val offer = MerchantOffer(resolved.firstCost, resolved.secondCost ?: ItemStack.EMPTY, resolved.result, 0, 1, 0, 0f)
            offers.add(offer)
            trades[offer] = trade.id
        }
    }
}

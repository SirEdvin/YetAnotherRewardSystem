package site.siredvin.yars.common.block

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import java.util.IdentityHashMap

class AutomaticRewardBoxMerchant(
    player: Player,
    private val box: AutomaticRewardBoxBlockEntity,
) : Merchant {
    private var tradingPlayer: Player? = player
    private val trades = IdentityHashMap<MerchantOffer, String>()
    private var offers = MerchantOffers()

    init {
        rebuildOffers()
    }

    override fun setTradingPlayer(player: Player?) {
        tradingPlayer = player
    }

    override fun getTradingPlayer(): Player? = tradingPlayer?.takeIf { box.canOpen(it) }
    override fun getOffers(): MerchantOffers = offers
    override fun overrideOffers(offers: MerchantOffers) {
        this.offers = offers
    }
    override fun notifyTrade(offer: MerchantOffer) = Unit
    override fun notifyTradeUpdated(stack: ItemStack) = Unit
    override fun getVillagerXp(): Int = 0
    override fun overrideXp(xp: Int) = Unit
    override fun showProgressBar(): Boolean = false
    override fun getNotifyTradeSound(): SoundEvent = SoundEvents.WANDERING_TRADER_YES
    override fun isClientSide(): Boolean = box.level?.isClientSide ?: false

    fun select(index: Int) {
        val offer = offers.getOrNull(index) ?: return
        trades[offer]?.let(box::selectTrade)
    }

    private fun rebuildOffers() {
        offers = MerchantOffers()
        trades.clear()
        val selected = box.selectedTradeId
        RewardShopTrades.all(box.shopId).sortedBy { it.id != selected }.forEach { trade ->
            val resolved = try {
                trade.resolve(box.completed(trade.id))
            } catch (exception: RuntimeException) {
                YarsCore.LOGGER.error("Skipping invalid automatic reward box trade ${trade.id}", exception)
                null
            } ?: return@forEach
            val offer = MerchantOffer(resolved.firstCost, resolved.secondCost ?: ItemStack.EMPTY, resolved.result, 0, 1, 0, 0f)
            offers += offer
            trades[offer] = trade.id
        }
    }
}

package site.siredvin.yars.common.rewardshop

import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.setup.Blocks
import java.util.IdentityHashMap

class RewardShopMerchant(
    private val player: Player,
    private val shopPos: BlockPos,
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

    override fun getTradingPlayer(): Player? = tradingPlayer?.takeIf {
        it.distanceToSqr(shopPos.x + 0.5, shopPos.y + 0.5, shopPos.z + 0.5) <= 64.0 && it.level().getBlockState(shopPos).`is`(Blocks.REWARD_SHOP.get())
    }

    override fun getOffers(): MerchantOffers = offers

    override fun overrideOffers(offers: MerchantOffers) {
        this.offers = offers
    }

    override fun notifyTrade(offer: MerchantOffer) {
        val tradeId = trades[offer] ?: return
        val trade = RewardShopTrades.find(tradeId) ?: return
        val resolved = runCatching { trade.resolve(RewardShopTradeHistory.completed(player, tradeId)) }.getOrNull() ?: return
        if (!ItemStack.matches(offer.result, resolved.result) || !ItemStack.matches(offer.costA, resolved.firstCost)) return
        if (!ItemStack.matches(offer.costB, resolved.secondCost ?: ItemStack.EMPTY)) return
        RewardShopTradeHistory.increment(player, tradeId)
        rebuildOffers()
        player.sendMerchantOffers(player.containerMenu.containerId, offers, 1, 0, false, false)
    }

    override fun notifyTradeUpdated(stack: ItemStack) = Unit

    override fun getVillagerXp(): Int = 0

    override fun overrideXp(xp: Int) = Unit

    override fun showProgressBar(): Boolean = false

    override fun getNotifyTradeSound(): SoundEvent = SoundEvents.WANDERING_TRADER_YES

    override fun isClientSide(): Boolean = player.level().isClientSide

    private fun rebuildOffers() {
        trades.clear()
        offers = MerchantOffers()
        RewardShopTrades.all().forEach { trade ->
            val resolved = try {
                trade.resolve(RewardShopTradeHistory.completed(player, trade.id))
            } catch (exception: IllegalArgumentException) {
                YarsCore.LOGGER.error("Skipping invalid reward shop trade ${trade.id}", exception)
                null
            } ?: return@forEach
            val offer = MerchantOffer(resolved.firstCost, resolved.secondCost ?: ItemStack.EMPTY, resolved.result, 0, 1, 0, 0f)
            offers.add(offer)
            trades[offer] = trade.id
        }
    }
}

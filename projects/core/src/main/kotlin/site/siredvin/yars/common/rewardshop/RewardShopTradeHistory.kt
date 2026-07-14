package site.siredvin.yars.common.rewardshop

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player

object RewardShopTradeHistory {
    const val ROOT_KEY = "yars"
    const val SHOP_KEY = "reward_shop"
    const val TRADES_KEY = "trades"

    private lateinit var persistentData: (Player) -> CompoundTag

    fun configure(persistentData: (Player) -> CompoundTag) {
        this.persistentData = persistentData
    }

    fun completed(player: Player, tradeId: String): Int {
        val value = trades(player).getInt(tradeId)
        return value.coerceAtLeast(0)
    }

    fun increment(player: Player, tradeId: String) {
        val trades = trades(player)
        trades.putInt(tradeId, completed(player, tradeId).coerceAtMost(Int.MAX_VALUE - 1) + 1)
    }

    private fun trades(player: Player): CompoundTag {
        val root = persistentData(player).getOrCreateCompound(ROOT_KEY)
        return root.getOrCreateCompound(SHOP_KEY).getOrCreateCompound(TRADES_KEY)
    }

    private fun CompoundTag.getOrCreateCompound(key: String): CompoundTag = getCompound(key).takeIf { !it.isEmpty } ?: CompoundTag().also { put(key, it) }
}

package site.siredvin.yars.common.rewardshop

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.player.Player

object RewardShopTradeHistory {
    const val ROOT_KEY = "yars"
    const val SHOP_KEY = "reward_shop"
    const val TRADES_KEY = "trades"

    private lateinit var persistentData: (Player) -> CompoundTag

    fun configure(persistentData: (Player) -> CompoundTag) {
        this.persistentData = persistentData
    }

    fun completed(player: Player, tradeId: String): Int = completed(persistentData(player), tradeId)

    internal fun completed(data: CompoundTag, tradeId: String): Int {
        val value = trades(data).getInt(tradeId)
        return value.coerceAtLeast(0)
    }

    fun increment(player: Player, tradeId: String) = increment(persistentData(player), tradeId)

    internal fun increment(data: CompoundTag, tradeId: String) {
        val trades = trades(data)
        trades.putInt(tradeId, completed(data, tradeId).coerceAtMost(Int.MAX_VALUE - 1) + 1)
    }

    private fun trades(data: CompoundTag): CompoundTag {
        val root = data.getOrCreateCompound(ROOT_KEY)
        return root.getOrCreateCompound(SHOP_KEY).getOrCreateCompound(TRADES_KEY)
    }

    private fun CompoundTag.getOrCreateCompound(key: String): CompoundTag = if (contains(key, Tag.TAG_COMPOUND.toInt())) getCompound(key) else CompoundTag().also { put(key, it) }
}

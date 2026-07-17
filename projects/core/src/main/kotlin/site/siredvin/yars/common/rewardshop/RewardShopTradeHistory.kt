package site.siredvin.yars.common.rewardshop

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

object RewardShopTradeHistory {
    const val ROOT_KEY = "yars"
    const val SHOP_KEY = "reward_shop"
    const val VERSION_KEY = "v2"
    const val SHOPS_KEY = "shops"
    const val TRADES_KEY = "trades"

    private lateinit var persistentData: (Player) -> CompoundTag

    fun configure(persistentData: (Player) -> CompoundTag) {
        this.persistentData = persistentData
    }

    fun completed(player: Player, shopId: ResourceLocation, tradeId: String): Int = completed(persistentData(player), shopId, tradeId)

    internal fun completed(data: CompoundTag, shopId: ResourceLocation, tradeId: String): Int {
        val trades = trades(data, shopId)
        if (trades.getTagType(tradeId) != Tag.TAG_INT) return 0
        val value = trades.getInt(tradeId)
        return value.coerceAtLeast(0)
    }

    fun increment(player: Player, shopId: ResourceLocation, tradeId: String) = increment(persistentData(player), shopId, tradeId)

    internal fun increment(data: CompoundTag, shopId: ResourceLocation, tradeId: String) {
        val trades = trades(data, shopId)
        trades.putInt(tradeId, completed(data, shopId, tradeId).coerceAtMost(Int.MAX_VALUE - 1) + 1)
    }

    private fun trades(data: CompoundTag, shopId: ResourceLocation): CompoundTag {
        val root = data.getOrCreateCompound(ROOT_KEY)
        return root.getOrCreateCompound(SHOP_KEY).getOrCreateCompound(VERSION_KEY).getOrCreateCompound(SHOPS_KEY)
            .getOrCreateCompound(shopId.toString()).getOrCreateCompound(TRADES_KEY)
    }

    private fun CompoundTag.getOrCreateCompound(key: String): CompoundTag = if (contains(key, Tag.TAG_COMPOUND.toInt())) getCompound(key) else CompoundTag().also { put(key, it) }
}

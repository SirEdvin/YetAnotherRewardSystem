package site.siredvin.yars.common.rewardshop

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class RewardShopTradeHistory internal constructor(
    private val counts: MutableMap<UUID, MutableMap<ResourceLocation, Int>> = mutableMapOf(),
) : SavedData() {
    companion object {
        const val FILE_ID = "yars_reward_trade_history"
        internal const val VERSION_KEY = "Version"
        internal const val PLAYERS_KEY = "Players"
        internal const val TRADES_KEY = "Trades"
        private const val VERSION = 1

        fun get(level: ServerLevel): RewardShopTradeHistory = level.server.overworld().dataStorage.computeIfAbsent(
            ::load,
            ::RewardShopTradeHistory,
            FILE_ID,
        )

        internal fun load(tag: CompoundTag): RewardShopTradeHistory {
            if (tag.getTagType(VERSION_KEY) != Tag.TAG_INT || tag.getInt(VERSION_KEY) != VERSION) return RewardShopTradeHistory()
            val counts = mutableMapOf<UUID, MutableMap<ResourceLocation, Int>>()
            val players = tag.getCompound(PLAYERS_KEY)
            players.allKeys.forEach { playerKey ->
                val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull()?.takeIf { it.toString() == playerKey } ?: return@forEach
                if (players.getTagType(playerKey) != Tag.TAG_COMPOUND) return@forEach
                val trades = players.getCompound(playerKey).getCompound(TRADES_KEY)
                trades.allKeys.forEach tradeLoop@{ tradeKey ->
                    val tradeId = ResourceLocation.tryParse(tradeKey)?.takeIf { it.toString() == tradeKey } ?: return@tradeLoop
                    if (trades.getTagType(tradeKey) != Tag.TAG_INT) return@tradeLoop
                    val value = trades.getInt(tradeKey)
                    if (value >= 0) counts.getOrPut(playerId) { mutableMapOf() }[tradeId] = value
                }
            }
            return RewardShopTradeHistory(counts)
        }
    }

    fun completed(playerId: UUID, tradeId: ResourceLocation): Int = counts[playerId]?.get(tradeId)?.coerceAtLeast(0) ?: 0

    fun increment(playerId: UUID, tradeId: ResourceLocation) {
        val trades = counts.getOrPut(playerId) { mutableMapOf() }
        trades[tradeId] = completed(playerId, tradeId).coerceAtMost(Int.MAX_VALUE - 1) + 1
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        tag.putInt(VERSION_KEY, VERSION)
        tag.put(
            PLAYERS_KEY,
            CompoundTag().also { players ->
                counts.forEach { (playerId, playerTrades) ->
                    players.put(
                        playerId.toString(),
                        CompoundTag().also { player ->
                            player.put(
                                TRADES_KEY,
                                CompoundTag().also { trades ->
                                    playerTrades.forEach { (tradeId, count) -> trades.putInt(tradeId.toString(), count.coerceAtLeast(0)) }
                                },
                            )
                        },
                    )
                }
            },
        )
        return tag
    }
}

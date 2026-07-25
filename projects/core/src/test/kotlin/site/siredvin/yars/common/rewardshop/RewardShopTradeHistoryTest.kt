package site.siredvin.yars.common.rewardshop

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class RewardShopTradeHistoryTest {
    private val firstPlayer = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val secondPlayer = UUID.fromString("abcdefab-cdef-abcd-efab-cdefabcdefab")
    private val trade = ResourceLocation("test:shared")

    @Test
    fun `isolates players while sharing one trade count across boxes`() {
        val history = RewardShopTradeHistory()
        history.increment(firstPlayer, trade)
        history.increment(firstPlayer, trade)

        assertEquals(2, history.completed(firstPlayer, trade))
        assertEquals(0, history.completed(secondPlayer, trade))
        assertTrue(history.isDirty)
    }

    @Test
    fun `loads only canonical validated versioned counts`() {
        val tag = CompoundTag().apply {
            putInt(RewardShopTradeHistory.VERSION_KEY, 1)
            put(
                RewardShopTradeHistory.PLAYERS_KEY,
                CompoundTag().apply {
                    put(firstPlayer.toString(), playerTrades(trade.toString(), 4))
                    put(secondPlayer.toString().uppercase(), playerTrades(trade.toString(), 8))
                    put("not-a-uuid", playerTrades(trade.toString(), 9))
                },
            )
        }
        val trades = tag.getCompound(RewardShopTradeHistory.PLAYERS_KEY).getCompound(firstPlayer.toString()).getCompound(RewardShopTradeHistory.TRADES_KEY)
        trades.putInt("test:negative", -1)
        trades.putString("test:malformed", "3")
        trades.putInt("Invalid Trade", 7)

        val history = RewardShopTradeHistory.load(tag)

        assertEquals(4, history.completed(firstPlayer, trade))
        assertEquals(0, history.completed(secondPlayer, trade))
        assertEquals(0, history.completed(firstPlayer, ResourceLocation("test:negative")))
        assertEquals(0, history.completed(firstPlayer, ResourceLocation("test:malformed")))
        assertEquals(0, RewardShopTradeHistory.load(CompoundTag().apply { put("shops", CompoundTag()) }).completed(firstPlayer, trade))
    }

    @Test
    fun `saturates and survives save load`() {
        val source = CompoundTag().apply {
            putInt(RewardShopTradeHistory.VERSION_KEY, 1)
            put(
                RewardShopTradeHistory.PLAYERS_KEY,
                CompoundTag().apply {
                    put(firstPlayer.toString(), playerTrades(trade.toString(), Int.MAX_VALUE))
                },
            )
        }
        val history = RewardShopTradeHistory.load(source)
        assertFalse(history.isDirty)
        history.increment(firstPlayer, trade)
        assertEquals(Int.MAX_VALUE, history.completed(firstPlayer, trade))
        assertTrue(history.isDirty)

        val loaded = RewardShopTradeHistory.load(history.save(CompoundTag()))
        assertEquals(Int.MAX_VALUE, loaded.completed(firstPlayer, trade))
        assertEquals(0, loaded.completed(firstPlayer, ResourceLocation("test:other")))
    }

    private fun playerTrades(tradeId: String, count: Int) = CompoundTag().apply {
        put(RewardShopTradeHistory.TRADES_KEY, CompoundTag().apply { putInt(tradeId, count) })
    }
}

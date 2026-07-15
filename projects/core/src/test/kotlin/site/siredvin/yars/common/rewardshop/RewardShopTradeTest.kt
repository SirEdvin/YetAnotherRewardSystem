package site.siredvin.yars.common.rewardshop

import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RewardShopTradeTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `resolves fixed capped and unlimited stages`() {
        val trade = RewardShopTradeRegistration().trade("test_trade")
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            .simple(-1, ItemStack(Items.IRON_INGOT, 4), ItemStack(Items.DIAMOND, 2))
            .build()

        assertEquals(Items.EMERALD, trade.resolve(0)!!.firstCost.item)
        assertEquals(Items.EMERALD, trade.resolve(1)!!.firstCost.item)
        assertEquals(4, trade.resolve(2)!!.firstCost.count)
        assertEquals(Items.IRON_INGOT, trade.resolve(100)!!.firstCost.item)
    }

    @Test
    fun `persisted purchase count advances through multistep limits`() {
        val trade = RewardShopTradeRegistration().trade("limited_trade")
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND, 2))
            .build()
        val persistentData = CompoundTag()

        repeat(2) {
            assertEquals(Items.EMERALD, trade.resolve(RewardShopTradeHistory.completed(persistentData, trade.id))!!.firstCost.item)
            RewardShopTradeHistory.increment(persistentData, trade.id)
        }
        assertEquals(Items.IRON_INGOT, trade.resolve(RewardShopTradeHistory.completed(persistentData, trade.id))!!.firstCost.item)
        RewardShopTradeHistory.increment(persistentData, trade.id)

        assertEquals(3, RewardShopTradeHistory.completed(persistentData, trade.id))
        assertNull(trade.resolve(RewardShopTradeHistory.completed(persistentData, trade.id)))
    }

    @Test
    fun `dynamic stages receive zero-based total purchase index`() {
        val trade = RewardShopTradeRegistration().trade("dynamic_trade")
            .simple(2, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
            .dynamic(
                2,
                { index -> ItemStack(Items.EMERALD, index + 1) },
                { index -> ItemStack(Items.DIAMOND, index + 1) },
                { index -> ItemStack(Items.GOLD_INGOT, index + 1) },
            )
            .build()

        val firstDynamic = trade.resolve(2)!!
        assertEquals(3, firstDynamic.firstCost.count)
        assertEquals(3, firstDynamic.result.count)
        assertEquals(3, firstDynamic.secondCost!!.count)
        assertEquals(4, trade.resolve(3)!!.firstCost.count)
        assertNull(trade.resolve(4))
    }

    @Test
    fun `purchase history isolates players and rejects malformed counts`() {
        val firstPlayer = CompoundTag()
        val secondPlayer = CompoundTag()

        RewardShopTradeHistory.increment(firstPlayer, "isolated")

        assertEquals(1, RewardShopTradeHistory.completed(firstPlayer, "isolated"))
        assertEquals(0, RewardShopTradeHistory.completed(secondPlayer, "isolated"))

        val trades = firstPlayer.getCompound(RewardShopTradeHistory.ROOT_KEY)
            .getCompound(RewardShopTradeHistory.SHOP_KEY)
            .getCompound(RewardShopTradeHistory.VERSION_KEY)
            .getCompound(RewardShopTradeHistory.TRADES_KEY)
        trades.putFloat("isolated", 4.5f)
        assertEquals(0, RewardShopTradeHistory.completed(firstPlayer, "isolated"))
        trades.putInt("isolated", -4)
        assertEquals(0, RewardShopTradeHistory.completed(firstPlayer, "isolated"))
    }

    @Test
    fun `purchase history saturates at integer maximum`() {
        val data = CompoundTag()
        repeat(2) { RewardShopTradeHistory.increment(data, "saturated") }
        val trades = data.getCompound(RewardShopTradeHistory.ROOT_KEY)
            .getCompound(RewardShopTradeHistory.SHOP_KEY)
            .getCompound(RewardShopTradeHistory.VERSION_KEY)
            .getCompound(RewardShopTradeHistory.TRADES_KEY)
        trades.putInt("saturated", Int.MAX_VALUE)

        RewardShopTradeHistory.increment(data, "saturated")

        assertEquals(Int.MAX_VALUE, RewardShopTradeHistory.completed(data, "saturated"))
    }

    @Test
    fun `rejects invalid fixed stacks during registration`() {
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("invalid_trade").simple(1, ItemStack.EMPTY, ItemStack(Items.DIAMOND))
        }
    }

    @Test
    fun `rejects invalid dynamic stacks during registration`() {
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("invalid_dynamic")
                .dynamic(1, { ItemStack.EMPTY }, { ItemStack(Items.DIAMOND) })
                .build()
        }
    }

    @Test
    fun `rejects invalid stage definitions`() {
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("empty").build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("fractional")
                .simple(1.5, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("unlimited_first")
                .simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
        }
    }

    @Test
    fun `copies resolved stacks`() {
        val cost = ItemStack(Items.EMERALD)
        val result = ItemStack(Items.DIAMOND)
        val trade = RewardShopTradeRegistration().trade("copies")
            .simple(-1, cost, result)
            .build()

        val resolved = trade.resolve(0)!!

        assertFalse(resolved.firstCost === cost)
        assertFalse(resolved.result === result)
        assertSame(Items.EMERALD, resolved.firstCost.item)
        assertTrue(resolved.secondCost == null)
    }

    @Test
    fun `duplicate error identifies trade`() {
        val registration = RewardShopTradeRegistration()
        registration.trade("duplicate").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        registration.trade("duplicate").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))

        val error = assertThrows(IllegalArgumentException::class.java, registration::replaceTrades)
        assertEquals("Duplicate reward shop trade ID: duplicate", error.message)
    }
}

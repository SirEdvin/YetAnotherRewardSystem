package site.siredvin.yars.common.rewardshop

import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.*
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
    fun `resolves fixed dynamic capped and unlimited stages`() {
        val trade = RewardShopTradeBuilder("test_trade")
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            .dynamic(-1, { index -> ItemStack(Items.IRON_INGOT, index + 1) }, { ItemStack(Items.DIAMOND, 2) })
            .build()

        assertEquals(Items.EMERALD, trade.resolve(1)!!.firstCost.item)
        assertEquals(3, trade.resolve(2)!!.firstCost.count)
        assertEquals(Items.IRON_INGOT, trade.resolve(3)!!.firstCost.item)
    }

    @Test
    fun `validates stage definitions and copies stacks`() {
        assertThrows(IllegalArgumentException::class.java) { RewardShopTradeBuilder("empty").build() }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeBuilder("fractional").simple(1.5, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeBuilder("invalid").simple(1, ItemStack.EMPTY, ItemStack(Items.DIAMOND))
        }
        val cost = ItemStack(Items.EMERALD)
        val resolved = RewardShopTradeBuilder("copies").simple(-1, cost, ItemStack(Items.DIAMOND)).build().resolve(0)!!
        assertNotSame(cost, resolved.firstCost)
    }

    @Test
    fun `keeps shops independent merges declarations and permits shared trade IDs`() {
        val first = ResourceLocation("test", "first.shop")
        val second = ResourceLocation("test", "second")
        val registration = RewardShopTradeRegistration()
        registration.shop(first.toString()) { it.trade("shared").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        registration.shop(first.toString()) { it.trade("later").simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT)) }
        registration.shop(second.toString()) { it.trade("shared").simple(-1, ItemStack(Items.GOLD_INGOT), ItemStack(Items.DIAMOND)) }

        registration.replaceTrades { true }

        assertEquals(listOf("shared", "later"), RewardShopTrades.all(first).map { it.id })
        assertEquals(listOf("shared"), RewardShopTrades.all(second).map { it.id })
        assertEquals(Items.EMERALD, RewardShopTrades.find(first, "shared")!!.resolve(0)!!.firstCost.item)
        assertEquals(Items.GOLD_INGOT, RewardShopTrades.find(second, "shared")!!.resolve(0)!!.firstCost.item)
    }

    @Test
    fun `rejects duplicate and invalid targets atomically`() {
        val valid = ResourceLocation("test", "valid")
        RewardShopTradeRegistration().apply {
            shop(valid.toString()) { it.trade("kept").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
            replaceTrades { true }
        }

        val duplicate = RewardShopTradeRegistration().apply {
            shop(valid.toString()) { it.trade("same").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
            shop(valid.toString()) { it.trade("same").simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND)) }
        }
        assertEquals(
            "Duplicate reward shop trade: test:valid / same",
            assertThrows(IllegalArgumentException::class.java) { duplicate.replaceTrades { true } }.message,
        )
        val invalid = RewardShopTradeRegistration().apply {
            shop("test:missing") { it.trade("new").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        }
        assertThrows(IllegalArgumentException::class.java) { invalid.replaceTrades { false } }
        assertNotNull(RewardShopTrades.find(valid, "kept"))
        assertTrue(RewardShopTrades.all(ResourceLocation("test", "missing")).isEmpty())
    }

    @Test
    fun `successful reload replaces the whole registry`() {
        val shop = ResourceLocation("test", "reload")
        RewardShopTradeRegistration().apply {
            shop(shop.toString()) { it.trade("old").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
            replaceTrades { true }
        }
        RewardShopTradeRegistration().apply {
            shop(shop.toString()) { it.trade("new").simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND)) }
            replaceTrades { true }
        }
        assertNull(RewardShopTrades.find(shop, "old"))
        assertNotNull(RewardShopTrades.find(shop, "new"))
    }

    @Test
    fun `history scopes direct keys by shop and ignores malformed and legacy values`() {
        val data = CompoundTag()
        val first = ResourceLocation("test", "daily.shop")
        val second = ResourceLocation("test", "second")
        val tradeId = "daily.trade"
        RewardShopTradeHistory.increment(data, first, tradeId)

        assertEquals(1, RewardShopTradeHistory.completed(data, first, tradeId))
        assertEquals(0, RewardShopTradeHistory.completed(data, second, tradeId))
        val shops = data.getCompound("yars").getCompound("reward_shop").getCompound("v2").getCompound("shops")
        assertTrue(shops.contains("test:daily.shop"))
        val trades = shops.getCompound(first.toString()).getCompound("trades")
        assertTrue(trades.contains(tradeId))
        trades.putFloat(tradeId, 4.5f)
        assertEquals(0, RewardShopTradeHistory.completed(data, first, tradeId))
        trades.putInt(tradeId, -4)
        assertEquals(0, RewardShopTradeHistory.completed(data, first, tradeId))
        data.getOrCreateCompound("yars").getOrCreateCompound("reward_shop").getOrCreateCompound("v1")
            .getOrCreateCompound("trades").putInt(tradeId, 99)
        assertEquals(0, RewardShopTradeHistory.completed(data, second, tradeId))
    }

    @Test
    fun `history persists and saturates`() {
        val data = CompoundTag()
        val shop = ResourceLocation("test", "shop")
        RewardShopTradeHistory.increment(data, shop, "trade")
        val reconnectedData = data.copy()
        assertEquals(1, RewardShopTradeHistory.completed(reconnectedData, shop, "trade"))
        reconnectedData.getCompound("yars").getCompound("reward_shop").getCompound("v2").getCompound("shops")
            .getCompound(shop.toString()).getCompound("trades").putInt("trade", Int.MAX_VALUE)
        RewardShopTradeHistory.increment(reconnectedData, shop, "trade")
        assertEquals(Int.MAX_VALUE, RewardShopTradeHistory.completed(reconnectedData, shop, "trade"))
    }

    private fun CompoundTag.getOrCreateCompound(key: String): CompoundTag = if (contains(key, Tag.TAG_COMPOUND.toInt())) getCompound(key) else CompoundTag().also { put(key, it) }
}

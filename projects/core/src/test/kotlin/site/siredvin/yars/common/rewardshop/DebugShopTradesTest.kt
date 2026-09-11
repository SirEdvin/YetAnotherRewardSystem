package site.siredvin.yars.common.rewardshop

import net.minecraft.SharedConstants
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.siredvin.yars.common.block.DebugShops
import java.util.UUID

class DebugShopTradesTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @BeforeEach
    fun install() = DebugShopTrades.registration().replaceTrades { true }

    private fun trade(path: String) = requireNotNull(RewardShopTrades.find(ResourceLocation("yars", "debug/$path")))

    @Test
    fun `catalog order attachments and initial offers are predictable`() {
        val shared = listOf("basic", "two_costs", "limited", "staged", "dynamic", "same_item_costs", "token_source", "token_exchange")
        val expectedResults = listOf(Items.EMERALD, Items.TORCH, Items.DIAMOND, Items.IRON_INGOT, Items.REDSTONE, Items.COPPER_INGOT, Items.PAPER, Items.AMETHYST_SHARD)
        for ((box, suffix) in listOf(DebugShops.MANUAL_ID to "manual", DebugShops.AUTOMATIC_ID to "automatic")) {
            assertEquals(shared.map { ResourceLocation("yars", "debug/shared/$it") } + ResourceLocation("yars", "debug/$suffix/limited"), RewardShopTrades.all(box).map { it.id })
            assertEquals(expectedResults + Items.APPLE, RewardShopTrades.all(box).map { it.resolve(0)!!.result.item })
        }
        val two = trade("shared/two_costs").resolve(0)!!
        assertEquals(Items.EMERALD, two.firstCost.item)
        assertEquals(2, two.secondCost!!.count)
        assertEquals(Items.STICK, two.secondCost.item)
        val same = trade("shared/same_item_costs").resolve(0)!!
        assertEquals(5, same.firstCost.count + same.secondCost!!.count)
        assertEquals(same.firstCost.item, same.secondCost.item)
    }

    @Test
    fun `limits stages dynamic counts and copied tokens remain valid`() {
        assertNotNull(trade("shared/limited").resolve(2))
        assertNull(trade("shared/limited").resolve(3))
        assertNull(trade("manual/limited").resolve(2))
        assertNull(trade("automatic/limited").resolve(2))
        assertEquals(listOf(Items.IRON_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.DIAMOND), (0..4).map { trade("shared/staged").resolve(it)!!.result.item })
        for (index in (0..100).toList() + listOf(Int.MAX_VALUE - 1, Int.MAX_VALUE)) {
            val offer = trade("shared/dynamic").resolve(index)!!
            assertEquals(1 + index % 4, offer.firstCost.count)
            assertEquals(1 + index % 3, offer.result.count)
            assertTrue(offer.firstCost.count in 1..offer.firstCost.maxStackSize)
            assertTrue(offer.result.count in 1..offer.result.maxStackSize)
        }
        val token = trade("shared/token_source").resolve(0)!!.result
        assertTrue(ItemStack.isSameItemSameTags(token, trade("shared/token_exchange").resolve(0)!!.firstCost))
        assertFalse(ItemStack.isSameItemSameTags(token, ItemStack(Items.PAPER)))
        token.orCreateTag.putString("yars_debug_token", "mutated")
        assertEquals("reward", trade("shared/token_source").resolve(0)!!.result.tag!!.getString("yars_debug_token"))
    }

    @Test
    fun `shared and independent histories persist through catalog replacement`() {
        val owner = UUID.randomUUID()
        val other = UUID.randomUUID()
        val history = RewardShopTradeHistory()
        val shared = trade("shared/staged").id
        val manual = trade("manual/limited").id
        history.increment(owner, shared)
        history.increment(owner, manual)
        repeat(2) { install() }
        assertSame(RewardShopTrades.find(DebugShops.MANUAL_ID, shared), RewardShopTrades.find(DebugShops.AUTOMATIC_ID, shared))
        assertEquals(1, history.completed(owner, shared))
        assertEquals(0, history.completed(other, shared))
        assertEquals(0, history.completed(owner, trade("automatic/limited").id))
        assertNull(RewardShopTrades.find(DebugShops.AUTOMATIC_ID, manual))
    }

    @Test
    fun `script additions coexist and invalid candidates preserve snapshot`() {
        val scriptId = ResourceLocation("test", "script")
        DebugShopTrades.registration().apply {
            trade(scriptId.toString()).simple(-1, ItemStack(Items.COAL), ItemStack(Items.GOLD_INGOT))
            attach(DebugShops.MANUAL_ID.toString(), scriptId.toString())
            replaceTrades { true }
        }
        val previous = RewardShopTrades.all(DebugShops.MANUAL_ID)
        assertEquals(10, previous.size)
        assertThrows(IllegalArgumentException::class.java) {
            DebugShopTrades.registration().apply {
                trade("yars:debug/shared/basic").simple(-1, ItemStack(Items.DIRT), ItemStack(Items.DIAMOND))
                replaceTrades { true }
            }
        }
        assertEquals(previous, RewardShopTrades.all(DebugShops.MANUAL_ID))
        assertThrows(IllegalArgumentException::class.java) {
            DebugShopTrades.registration().apply {
                attach(DebugShops.MANUAL_ID.toString(), "test:missing")
                replaceTrades { true }
            }
        }
        assertEquals(previous, RewardShopTrades.all(DebugShops.MANUAL_ID))
        install()
        assertNull(RewardShopTrades.find(scriptId))
        assertEquals(9, RewardShopTrades.all(DebugShops.MANUAL_ID).size)
    }
}

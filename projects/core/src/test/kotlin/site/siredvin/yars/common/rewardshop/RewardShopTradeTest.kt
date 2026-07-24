package site.siredvin.yars.common.rewardshop

import net.minecraft.SharedConstants
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `resolves namespaced fixed dynamic capped and unlimited stages`() {
        val id = id("test:trade")
        val trade = RewardShopTradeBuilder(id)
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            .dynamic(-1, { index -> ItemStack(Items.IRON_INGOT, index + 1) }, { ItemStack(Items.DIAMOND, 2) })
            .build()

        assertEquals(id, trade.resolve(1)!!.id)
        assertEquals(Items.EMERALD, trade.resolve(1)!!.firstCost.item)
        assertEquals(3, trade.resolve(2)!!.firstCost.count)
        assertEquals(Items.IRON_INGOT, trade.resolve(3)!!.firstCost.item)
        assertThrows(IllegalArgumentException::class.java) { RewardShopTradeRegistration().trade("plain_trade") }
    }

    @Test
    fun `validates stage definitions and copies stacks`() {
        assertThrows(IllegalArgumentException::class.java) { RewardShopTradeBuilder(id("test:empty")).build() }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeBuilder(id("test:fractional")).simple(1.5, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeBuilder(id("test:invalid")).simple(1, ItemStack.EMPTY, ItemStack(Items.DIAMOND))
        }
        val cost = ItemStack(Items.EMERALD)
        val resolved = RewardShopTradeBuilder(id("test:copies")).simple(-1, cost, ItemStack(Items.DIAMOND)).build().resolve(0)!!
        assertNotSame(cost, resolved.firstCost)
    }

    @Test
    fun `reuses global definitions in attachment order across boxes`() {
        val firstBox = id("test:first")
        val secondBox = id("test:second")
        val shared = id("test:shared")
        val later = id("test:later")
        RewardShopTradeRegistration().apply {
            trade(shared.toString()).simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            trade(later.toString()).simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
            attach(firstBox.toString(), later.toString())
            attach(firstBox.toString(), shared.toString())
            attach(secondBox.toString(), shared.toString())
            replaceTrades { true }
        }

        assertEquals(listOf(later, shared), RewardShopTrades.all(firstBox).map { it.id })
        assertEquals(listOf(shared), RewardShopTrades.all(secondBox).map { it.id })
        assertEquals(RewardShopTrades.find(shared), RewardShopTrades.find(firstBox, shared))
    }

    @Test
    fun `rejects invalid declarations atomically`() {
        val box = id("test:box")
        val kept = id("test:kept")
        register(kept, box)

        val duplicate = RewardShopTradeRegistration().apply {
            trade("test:same").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            trade("test:same").simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
        }
        assertThrows(IllegalArgumentException::class.java) { duplicate.replaceTrades { true } }

        val duplicateAttachment = RewardShopTradeRegistration().apply {
            trade("test:new").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            attach(box.toString(), "test:new")
            attach(box.toString(), "test:new")
        }
        assertThrows(IllegalArgumentException::class.java) { duplicateAttachment.replaceTrades { true } }

        val unknown = RewardShopTradeRegistration().apply { attach(box.toString(), "test:unknown") }
        assertThrows(IllegalArgumentException::class.java) { unknown.replaceTrades { true } }

        val invalidTarget = RewardShopTradeRegistration().apply {
            trade("test:new").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            attach("test:missing", "test:new")
        }
        assertThrows(IllegalArgumentException::class.java) { invalidTarget.replaceTrades { false } }
        assertNotNull(RewardShopTrades.find(box, kept))
        assertNull(RewardShopTrades.find(id("test:missing"), kept))
    }

    @Test
    fun `successful reload detaches stale offers`() {
        val oldBox = id("test:old")
        val newBox = id("test:new")
        val trade = id("test:trade")
        register(trade, oldBox)
        register(trade, newBox)

        assertNull(RewardShopTrades.find(oldBox, trade))
        assertNotNull(RewardShopTrades.find(newBox, trade))
        assertNotNull(RewardShopTrades.find(trade))
    }

    private fun register(tradeId: ResourceLocation, boxId: ResourceLocation) {
        RewardShopTradeRegistration().apply {
            trade(tradeId.toString()).simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            attach(boxId.toString(), tradeId.toString())
            replaceTrades { true }
        }
    }

    private fun id(value: String) = ResourceLocation(value)
}

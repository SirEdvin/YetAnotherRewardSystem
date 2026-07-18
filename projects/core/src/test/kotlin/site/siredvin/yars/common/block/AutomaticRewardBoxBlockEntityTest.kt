package site.siredvin.yars.common.block

import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades

class AutomaticRewardBoxBlockEntityTest {
    companion object {
        val SHOP_ID = ResourceLocation("yars_test", "automatic_reward_box")

        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `executes staged trades atomically and retains per-trade progress`() {
        val box = box()
        register {
            it.trade("staged")
                .simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
            it.trade("other").simple(-1, ItemStack(Items.COAL), ItemStack(Items.REDSTONE))
        }

        assertTrue(box.selectTrade("staged"))
        box.setItem(0, ItemStack(Items.EMERALD, 2))
        box.processTrades(true)
        assertEquals(1, box.completed("staged"))
        assertEquals(1, box.getItem(0).count)
        assertEquals(1, box.getItem(2).count)

        assertTrue(box.selectTrade("other"))
        box.setItem(1, ItemStack(Items.COAL))
        box.processTrades(true)
        assertEquals(1, box.completed("other"))
        assertTrue(box.selectTrade("staged"))
        box.setItem(1, ItemStack(Items.IRON_INGOT))
        box.processTrades(true)
        assertEquals(2, box.completed("staged"))
        assertEquals(Items.GOLD_INGOT, box.getItem(4).item)
    }

    @Test
    fun `requires both equal costs and preserves payments when output is full`() {
        val box = box()
        register { it.trade("double").simple(-1, ItemStack(Items.EMERALD, 2), ItemStack(Items.DIAMOND), ItemStack(Items.EMERALD, 3)) }
        assertTrue(box.selectTrade("double"))
        box.setItem(0, ItemStack(Items.EMERALD, 4))
        box.processTrades(true)
        assertEquals(4, box.getItem(0).count)
        box.setItem(1, ItemStack(Items.EMERALD))
        for (slot in 2 until box.containerSize) box.setItem(slot, ItemStack(Items.STONE, 64))
        box.processTrades(true)
        assertEquals(5, box.getItem(0).count + box.getItem(1).count)
        assertEquals(0, box.completed("double"))
    }

    @Test
    fun `persists inventory selection and direct-key counts while ignoring malformed counts`() {
        val original = box()
        register { it.trade("trade.with-punctuation").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        assertTrue(original.selectTrade("trade.with-punctuation"))
        original.setItem(0, ItemStack(Items.EMERALD))
        original.processTrades(true)
        val saved = original.saveWithFullMetadata()
        saved.getCompound("Completed").putString("bad", "not-an-int")

        val loaded = box()
        loaded.load(saved)
        assertEquals("trade.with-punctuation", loaded.selectedTradeId)
        assertEquals(1, loaded.completed("trade.with-punctuation"))
        assertEquals(0, loaded.completed("bad"))
        assertEquals(Items.DIAMOND, loaded.getItem(2).item)
    }

    @Test
    fun `retains state across trade removal and rejects invalid dynamic trades`() {
        val box = box()
        register { it.trade("reload").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        assertTrue(box.selectTrade("reload"))
        RewardShopTrades.replace(emptyMap())
        box.setItem(0, ItemStack(Items.EMERALD))
        box.processTrades(true)
        assertEquals(1, box.getItem(0).count)
        assertEquals("reload", box.selectedTradeId)

        var broken = false
        register { it.trade("reload").dynamic(-1, { if (broken) error("broken") else ItemStack(Items.EMERALD) }, { ItemStack(Items.DIAMOND) }) }
        broken = true
        box.processTrades(true)
        assertEquals(1, box.getItem(0).count)
        assertEquals(0, box.completed("reload"))
    }

    private fun register(callback: (site.siredvin.yars.common.rewardshop.RewardShopTradeShopRegistration) -> Unit) {
        RewardShopTradeRegistration().apply {
            shop(SHOP_ID.toString(), callback)
            replaceTrades { true }
        }
    }

    private fun box(): AutomaticRewardBoxBlockEntity = AutomaticRewardBoxBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), SHOP_ID)
}

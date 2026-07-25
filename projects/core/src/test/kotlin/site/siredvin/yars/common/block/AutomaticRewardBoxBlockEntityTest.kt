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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import site.siredvin.yars.common.rewardshop.RewardShopTradeBuilder
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import java.util.UUID

class AutomaticRewardBoxBlockEntityTest {
    companion object {
        val BOX_ID = ResourceLocation("yars_test:automatic_reward_box")
        val SECOND_BOX_ID = ResourceLocation("yars_test:second_automatic_reward_box")
        val OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222")

        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `offline boxes share stages and limits through owner history`() {
        val history = RewardShopTradeHistory()
        val first = box(BOX_ID)
        val second = box(SECOND_BOX_ID)
        val trade = ResourceLocation("test:staged")
        register(trade, listOf(BOX_ID, SECOND_BOX_ID)) {
            it.simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
        }

        assertTrue(first.selectTrade(OWNER, trade, history))
        first.setItem(0, ItemStack(Items.EMERALD))
        first.processTrades(history)
        assertEquals(1, history.completed(OWNER, trade))
        assertEquals(Items.DIAMOND, first.getItem(2).item)

        assertTrue(second.selectTrade(OWNER, trade, history))
        second.setItem(0, ItemStack(Items.IRON_INGOT, 2))
        second.processTrades(history)
        assertEquals(2, history.completed(OWNER, trade))
        assertEquals(1, second.getItem(0).count)
        assertEquals(Items.GOLD_INGOT, second.getItem(2).item)
        assertNull(second.currentTrade(history))
    }

    @Test
    fun `ownerless and non-owner selections stay inactive`() {
        val history = RewardShopTradeHistory()
        val trade = ResourceLocation("test:owned")
        register(trade, listOf(BOX_ID)) { it.simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        val ownerless = box(BOX_ID, null)
        ownerless.setItem(0, ItemStack(Items.EMERALD))

        assertFalse(ownerless.selectTrade(OWNER, trade, history))
        ownerless.processTrades(history)
        assertEquals(1, ownerless.getItem(0).count)

        val owned = box()
        assertFalse(owned.selectTrade(OTHER, trade, history))
        assertTrue(owned.selectTrade(OWNER, trade, history))
    }

    @Test
    fun `requires complete costs and output capacity before changing inventory or history`() {
        val history = RewardShopTradeHistory()
        val trade = ResourceLocation("test:double")
        val box = box()
        register(trade, listOf(BOX_ID)) {
            it.simple(-1, ItemStack(Items.EMERALD, 2), ItemStack(Items.DIAMOND), ItemStack(Items.EMERALD, 3))
        }
        assertTrue(box.selectTrade(OWNER, trade, history))
        box.setItem(0, ItemStack(Items.EMERALD, 4))
        box.processTrades(history)
        assertEquals(4, box.getItem(0).count)
        box.setItem(1, ItemStack(Items.EMERALD))
        for (slot in 2 until box.containerSize) box.setItem(slot, ItemStack(Items.STONE, 64))
        box.processTrades(history)

        assertEquals(5, box.getItem(0).count + box.getItem(1).count)
        assertEquals(0, history.completed(OWNER, trade))
    }

    @Test
    fun `persists owner inventory and namespaced selection without local history`() {
        val history = RewardShopTradeHistory()
        val trade = ResourceLocation("test:trade.with-punctuation")
        val original = box()
        register(trade, listOf(BOX_ID)) { it.simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        assertTrue(original.selectTrade(OWNER, trade, history))
        original.setItem(0, ItemStack(Items.EMERALD))
        original.processTrades(history)

        val saved = original.saveWithFullMetadata()
        val loaded = box(BOX_ID, null)
        loaded.load(saved)

        assertEquals(OWNER, loaded.ownerPlayerUUID)
        assertEquals(trade, loaded.selectedTradeId)
        assertFalse(saved.contains("Completed"))
        assertEquals(Items.DIAMOND, loaded.getItem(2).item)
        assertEquals(1, history.completed(OWNER, trade))
    }

    @Test
    fun `retains selection and inventory across detach and invalid resolution`() {
        val history = RewardShopTradeHistory()
        val trade = ResourceLocation("test:reload")
        val box = box()
        register(trade, listOf(BOX_ID)) { it.simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
        assertTrue(box.selectTrade(OWNER, trade, history))
        RewardShopTrades.replace(mapOf(trade to RewardShopTrades.find(trade)!!), emptyMap())
        box.setItem(0, ItemStack(Items.EMERALD))
        box.processTrades(history)
        assertEquals(1, box.getItem(0).count)
        assertEquals(trade, box.selectedTradeId)

        var broken = false
        register(trade, listOf(BOX_ID)) {
            it.dynamic(-1, { if (broken) error("broken") else ItemStack(Items.EMERALD) }, { ItemStack(Items.DIAMOND) })
        }
        broken = true
        box.processTrades(history)
        assertEquals(1, box.getItem(0).count)
        assertEquals(0, history.completed(OWNER, trade))
    }

    private fun register(tradeId: ResourceLocation, boxes: List<ResourceLocation>, callback: (RewardShopTradeBuilder) -> Unit) {
        RewardShopTradeRegistration().apply {
            callback(trade(tradeId.toString()))
            boxes.forEach { attach(it.toString(), tradeId.toString()) }
            replaceTrades { true }
        }
    }

    private fun box(id: ResourceLocation = BOX_ID, owner: UUID? = OWNER): AutomaticRewardBoxBlockEntity = AutomaticRewardBoxBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), id).also { it.ownerPlayerUUID = owner }
}

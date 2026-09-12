package site.siredvin.yars.common.block

import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import java.util.UUID

class AutomaticRewardBoxDisplayTest {
    companion object {
        private val SHOP = ResourceLocation("yars_test:display")
        private val TRADE = ResourceLocation("yars_test:display_trade")
        private val OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111")

        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private fun box() = AutomaticRewardBoxBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), SHOP).also { it.ownerPlayerUUID = OWNER }

    private fun register(second: ItemStack? = null) {
        RewardShopTradeRegistration().apply {
            trade(TRADE.toString()).simple(1, ItemStack(Items.EMERALD, 5), ItemStack(Items.DIAMOND, 2).setHoverName(Component.literal("Reward")), second)
                .simple(1, ItemStack(Items.GOLD_INGOT, 12), ItemStack(Items.APPLE))
            attach(SHOP.toString(), TRADE.toString())
            replaceTrades { true }
        }
    }

    @Test
    fun `new and legacy boxes default on and both settings persist`() {
        val box = box()
        assertTrue(box.showTradeDisplay)
        box.setTradeDisplayEnabled(false)
        val off = box.saveWithFullMetadata()
        val loaded = box()
        loaded.load(off)
        assertFalse(loaded.showTradeDisplay)
        loaded.setTradeDisplayEnabled(true)
        box.load(loaded.saveWithFullMetadata())
        assertTrue(box.showTradeDisplay)
        off.remove("ShowTradeDisplay")
        loaded.load(off)
        assertTrue(loaded.showTradeDisplay)
    }

    @Test
    fun `snapshot preserves exact distinct costs metadata and copy isolation`() {
        register(ItemStack(Items.EMERALD, 3))
        val box = box()
        val history = RewardShopTradeHistory()
        assertTrue(box.selectTrade(OWNER, TRADE, history))
        val stacks = box.displayedItems
        assertEquals(listOf(5, 3, 2), stacks.map { it.count })
        assertEquals(listOf(Items.EMERALD, Items.EMERALD, Items.DIAMOND), stacks.map { it.item })
        assertEquals("Reward", stacks.last().hoverName.string)
        stacks[0].count = 1
        stacks.last().setHoverName(Component.literal("Mutated"))
        assertEquals(5, box.displayedItems.first().count)
        assertEquals("Reward", box.displayedItems.last().hoverName.string)
        assertFalse(box.refreshDisplay(history))
    }

    @Test
    fun `network updates contain only presentation and never load inventory or ownership`() {
        register(ItemStack(Items.IRON_INGOT, 4))
        val server = box()
        val client = box()
        val history = RewardShopTradeHistory()
        server.selectTrade(OWNER, TRADE, history)
        client.setItem(0, ItemStack(Items.STONE, 17))
        val tag = server.updateTag
        assertEquals(setOf("TradeDisplay"), tag.allKeys)
        assertEquals(setOf("ShowTradeDisplay", "FirstCost", "SecondCost", "Result"), tag.getCompound("TradeDisplay").allKeys)
        assertEquals(tag, server.updatePacket.tag)
        client.load(tag)
        assertEquals(listOf(5, 4, 2), client.displayedItems.map { it.count })
        assertEquals(OWNER, client.ownerPlayerUUID)
        assertEquals(17, client.getItem(0).count)
        server.setTradeDisplayEnabled(false)
        client.load(server.updateTag)
        assertFalse(client.showTradeDisplay)
        assertTrue(client.displayedItems.isEmpty())
        assertEquals(17, client.getItem(0).count)
        server.setTradeDisplayEnabled(true)
        server.refreshDisplay(history)
        client.load(server.updateTag)
        assertTrue(client.showTradeDisplay)
        assertEquals(3, client.displayedItems.size)
        server.processTrades(history)
        history.increment(OWNER, TRADE)
        server.refreshDisplay(history)
        client.load(server.updateTag)
        assertEquals(listOf(12, 1), client.displayedItems.map { it.count })
    }

    @Test
    fun `processing displays next stage and clears exhaustion without losing selection`() {
        register()
        val history = RewardShopTradeHistory()
        val box = box()
        box.selectTrade(OWNER, TRADE, history)
        assertEquals(listOf(5, 2), box.displayedItems.map { it.count })
        box.setItemForTransaction(0, ItemStack(Items.EMERALD, 5))
        box.processTrades(history)
        assertEquals(listOf(Items.GOLD_INGOT, Items.APPLE), box.displayedItems.map { it.item })
        box.setItemForTransaction(0, ItemStack(Items.GOLD_INGOT, 12))
        box.processTrades(history)
        assertTrue(box.displayedItems.isEmpty())
        assertEquals(TRADE, box.selectedTradeId)
        assertEquals(2, history.completed(OWNER, TRADE))
    }

    @Test
    fun `missing owner selection detached and invalid trades clear while restoration repopulates`() {
        register()
        val box = box()
        val history = RewardShopTradeHistory()
        box.refreshDisplay(history)
        assertTrue(box.displayedItems.isEmpty())
        box.selectTrade(OWNER, TRADE, history)
        val trade = RewardShopTrades.find(TRADE)!!
        RewardShopTrades.replace(mapOf(TRADE to trade), emptyMap())
        box.refreshDisplay(history)
        assertTrue(box.displayedItems.isEmpty())
        assertEquals(TRADE, box.selectedTradeId)
        RewardShopTrades.replace(mapOf(TRADE to trade), mapOf(SHOP to listOf(TRADE)))
        box.refreshDisplay(history)
        assertEquals(2, box.displayedItems.size)
        box.ownerPlayerUUID = null
        box.refreshDisplay(history)
        assertTrue(box.displayedItems.isEmpty())
        box.ownerPlayerUUID = OWNER
        var broken = false
        RewardShopTradeRegistration().apply {
            trade(TRADE.toString()).dynamic(-1, { if (broken) error("invalid") else ItemStack(Items.EMERALD) }, { ItemStack(Items.DIAMOND) })
            attach(SHOP.toString(), TRADE.toString())
            replaceTrades { true }
        }
        box.refreshDisplay(history)
        broken = true
        box.refreshDisplay(history)
        assertTrue(box.displayedItems.isEmpty())
        assertEquals(0, history.completed(OWNER, TRADE))
    }

    @Test
    fun `visibility is independent and does not execute ready payments`() {
        register()
        val history = RewardShopTradeHistory()
        val box = box()
        val other = box()
        box.selectTrade(OWNER, TRADE, history)
        box.setItemForTransaction(0, ItemStack(Items.EMERALD, 5))
        box.setTradeDisplayEnabled(false)
        box.setTradeDisplayEnabled(true)
        assertEquals(5, box.getItem(0).count)
        assertEquals(0, history.completed(OWNER, TRADE))
        assertTrue(other.showTradeDisplay)
        box.setTradeDisplayEnabled(false)
        box.processTrades(history)
        assertEquals(1, history.completed(OWNER, TRADE))
        assertEquals(2, box.getItem(2).count)
        assertTrue(box.displayedItems.isEmpty())
    }

    @Test
    fun `history listeners observe committed counts and remain scoped and removable`() {
        val history = RewardShopTradeHistory()
        val other = RewardShopTradeHistory()
        val calls = mutableListOf<Pair<UUID, ResourceLocation>>()
        val listener = RewardShopTradeHistory.Listener { owner, trade ->
            assertEquals(1, history.completed(owner, trade))
            calls += owner to trade
        }
        history.addListener(listener)
        other.increment(OWNER, TRADE)
        assertTrue(calls.isEmpty())
        history.increment(OWNER, TRADE)
        assertEquals(listOf(OWNER to TRADE), calls)
        history.removeListener(listener)
        history.increment(OWNER, TRADE)
        assertEquals(1, calls.size)
    }
}

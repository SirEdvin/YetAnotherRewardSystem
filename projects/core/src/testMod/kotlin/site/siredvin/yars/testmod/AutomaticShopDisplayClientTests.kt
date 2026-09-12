package site.siredvin.yars.testmod

import net.minecraft.client.gui.components.Button
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import site.siredvin.testiarium.api.ClientGameTest
import site.siredvin.testiarium.api.TestGroup
import site.siredvin.testiarium.api.TestTags
import site.siredvin.testiarium.api.Timeouts
import site.siredvin.testiarium.api.sequence
import site.siredvin.yars.client.AutomaticRewardBoxRenderer
import site.siredvin.yars.client.AutomaticRewardBoxScreen
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.block.DebugShops
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot
import java.util.UUID

object AutomaticShopDisplayClientTests {
    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 90)
    @TestGroup(TestTags.CLIENT)
    fun tradeDisplayLifecycle(helper: GameTestHelper) = helper.sequence {
        val pos = helper.absolutePos(BlockPos(1, 1, 1))
        val otherPos = helper.absolutePos(BlockPos(3, 1, 1))
        val manualPos = helper.absolutePos(BlockPos(5, 1, 1))
        val nativeId = ResourceLocation("yars_test:automatic_reward_box")
        val scriptedId = ResourceLocation("kubejs:display_smoke_shop")
        val tradeId = ResourceLocation("yars_test:display_${UUID.randomUUID().toString().replace("-", "")}")
        lateinit var player: ServerPlayer
        lateinit var box: AutomaticRewardBoxBlockEntity
        lateinit var other: AutomaticRewardBoxBlockEntity
        lateinit var history: RewardShopTradeHistory
        lateinit var registration: RewardShopTradeRegistration
        var oldGuiScale = 0
        thenExecute {
            val loader = if (System.getProperty("testiarium.gametest-report").contains("/fabric/")) "fabric" else "forge"
            System.setProperty("testiarium.screenshots", "/tmp/yars-trade-display/$loader")
            player = helper.level.randomPlayer as ServerPlayer
            player.closeContainer()
            player.inventory.clearContent()
            player.abilities.flying = true
            player.onUpdateAbilities()
            player.connection.teleport(pos.x + 1.5, pos.y + 0.7, pos.z + 4.0, 180f, 8f)
            val secondId = if (BuiltInRegistries.BLOCK.containsKey(scriptedId)) scriptedId else nativeId
            helper.level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(DebugShops.AUTOMATIC_ID).defaultBlockState())
            helper.level.setBlockAndUpdate(otherPos, BuiltInRegistries.BLOCK.get(secondId).defaultBlockState())
            helper.level.setBlockAndUpdate(manualPos, BuiltInRegistries.BLOCK.get(DebugShops.MANUAL_ID).defaultBlockState())
            box = helper.level.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity
            other = helper.level.getBlockEntity(otherPos) as AutomaticRewardBoxBlockEntity
            box.ownerPlayerUUID = player.uuid
            other.ownerPlayerUUID = player.uuid
            history = RewardShopTradeHistory.get(helper.level)
            registration = RewardShopTradeRegistration().apply {
                trade(tradeId.toString())
                    .simple(1, ItemStack(Items.EMERALD, 12), ItemStack(Items.DIAMOND, 2).setHoverName(Component.literal("Display reward")), ItemStack(Items.IRON_INGOT, 3))
                    .simple(1, ItemStack(Items.GOLD_INGOT, 5), ItemStack(Items.APPLE))
                    .simple(-1, ItemStack(Items.COAL, 16), ItemStack(Items.DIAMOND))
                for (id in listOf(DebugShops.AUTOMATIC_ID, secondId, DebugShops.MANUAL_ID)) attach(id.toString(), tradeId.toString())
            }
            registration.replaceTrades()
            check(box.selectTrade(player, tradeId))
            check(other.selectTrade(player, tradeId))
            check(box.showTradeDisplay && other.showTradeDisplay)
            println("YARS_DISPLAY_TYPE $secondId")
        }
        thenIdle(20)
        thenOnClient {
            check(screen == null)
            for (p in listOf(pos, otherPos)) {
                val client = level!!.getBlockEntity(p) as AutomaticRewardBoxBlockEntity
                check(client.displayedItems.map { it.count } == listOf(12, 3, 2)) { "Initial display snapshot missing" }
                check(client.displayedItems.last().hoverName.string == "Display reward")
                check(blockEntityRenderDispatcher.getRenderer(client) is AutomaticRewardBoxRenderer) { "Automatic type has no renderer: ${client.shopId}" }
            }
        }
        thenScreenshot("three-items-quantities")
        thenExecute {
            open(player, pos)
            val menu = player.containerMenu as AutomaticRewardBoxMenu
            check(!menu.clickMenuButton(player, 99))
            val owner = box.ownerPlayerUUID
            box.ownerPlayerUUID = UUID.randomUUID()
            check(!menu.clickMenuButton(player, AutomaticRewardBoxMenu.TOGGLE_DISPLAY_BUTTON))
            box.ownerPlayerUUID = owner
            val savedPosition = player.position()
            player.setPos(pos.x + 30.0, pos.y.toDouble(), pos.z.toDouble())
            check(!menu.clickMenuButton(player, AutomaticRewardBoxMenu.TOGGLE_DISPLAY_BUTTON))
            player.setPos(savedPosition)
            check(box.showTradeDisplay)
        }
        thenIdle(10)
        thenOnClient {
            val screen = screen as AutomaticRewardBoxScreen
            val button = screen.children().filterIsInstance<Button>().single { it.message.string == "Trade display: On" }
            check(button.active && button.visible)
            screen.setFocused(button)
            check(screen.keyPressed(257, 0, 0)) { "Focused toggle did not accept Enter" }
        }
        thenIdle(10)
        thenExecute {
            check(!box.showTradeDisplay)
            check(other.showTradeDisplay)
            check(history.completed(player.uuid, tradeId) == 0)
            check(box.isEmpty)
        }
        thenOnClient {
            val menu = player!!.containerMenu as AutomaticRewardBoxMenu
            check(!menu.showTradeDisplay)
            check((level!!.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity).displayedItems.isEmpty())
            check((screen as AutomaticRewardBoxScreen).children().filterIsInstance<Button>().any { it.message.string == "Trade display: Off" })
            oldGuiScale = options.guiScale().get()
        }
        thenScreenshot("toggle-off-ui", true)
        thenExecute {
            val stale = player.containerMenu as AutomaticRewardBoxMenu
            player.closeContainer()
            check(!stale.clickMenuButton(player, AutomaticRewardBoxMenu.TOGGLE_DISPLAY_BUTTON))
            val saved = box.saveWithFullMetadata()
            helper.level.removeBlock(pos, false)
            helper.level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(DebugShops.AUTOMATIC_ID).defaultBlockState())
            box = helper.level.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity
            box.load(saved)
            check(!box.showTradeDisplay)
            open(player, pos)
        }
        thenIdle(10)
        thenOnClient {
            check(!(player!!.containerMenu as AutomaticRewardBoxMenu).showTradeDisplay)
            val button = (screen as AutomaticRewardBoxScreen).children().filterIsInstance<Button>().single { it.message.string == "Trade display: Off" }
            check(screen!!.mouseClicked(button.x + 2.0, button.y + 2.0, 0))
        }
        thenIdle(10)
        thenExecute {
            check(box.showTradeDisplay)
            player.closeContainer()
            // Display-only history notifications must not turn an external purchase into a new processing trigger.
            box.setItemForTransaction(0, ItemStack(Items.GOLD_INGOT, 5))
            other.setItem(0, ItemStack(Items.EMERALD, 12))
            other.setItem(1, ItemStack(Items.IRON_INGOT, 3))
            check(history.completed(player.uuid, tradeId) == 1)
            check(box.getItem(0).count == 5 && box.getItem(2).isEmpty)
            check(box.displayedItems.map { it.count } == listOf(5, 1))
        }
        thenIdle(10)
        thenOnClient {
            check((level!!.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity).displayedItems.map { it.count } == listOf(5, 1))
        }
        thenScreenshot("two-items-quantity-one")
        thenExecute {
            open(player, manualPos)
            val menu = player.containerMenu as MerchantMenu
            menu.setSelectionHint(0)
            menu.getSlot(0).set(ItemStack(Items.GOLD_INGOT, 5))
            check(!menu.quickMoveStack(player, 2).isEmpty)
            check(history.completed(player.uuid, tradeId) == 2)
            check(box.displayedItems.map { it.count } == listOf(16, 1))
            player.closeContainer()
            check(box.getItem(0).count == 5 && box.getItem(2).isEmpty)
            box.setItemForTransaction(0, ItemStack.EMPTY)
            check(box.setTradeDisplayEnabled(player, false))
            box.setItem(0, ItemStack(Items.COAL, 16))
            check(history.completed(player.uuid, tradeId) == 3)
            check(box.getItem(2).item == Items.DIAMOND)
            check(box.setTradeDisplayEnabled(player, true))
            val before = box.updateTag
            history.increment(UUID.randomUUID(), tradeId)
            history.increment(player.uuid, ResourceLocation("yars_test:unrelated"))
            check(box.updateTag == before)
            box.setRemoved()
            history.increment(player.uuid, tradeId)
            check(box.updateTag == before)
            box.clearRemoved()
            check(box.displayedItems.map { it.count } == listOf(16, 1))
            RewardShopTrades.replace(emptyMap(), emptyMap())
        }
        thenIdle(10)
        thenOnClient {
            check((level!!.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity).displayedItems.isEmpty())
        }
        thenExecute {
            registration.replaceTrades()
            check(box.selectedTradeId == tradeId)
            check(box.displayedItems.map { it.count } == listOf(16, 1))
            // The owner need not be online for a newly tracked observer to get resolved display data.
            val saved = box.saveWithFullMetadata()
            saved.putUUID("Owner", UUID.randomUUID())
            box.load(saved)
            check(box.displayedItems.map { it.count } == listOf(12, 3, 2))
            player.connection.teleport(pos.x + 220.5, pos.y + 2.0, pos.z + 4.0, 180f, 8f)
        }
        thenIdle(40)
        thenOnClient { check(level!!.getBlockEntity(pos) == null) { "Observer did not stop tracking the test chunk" } }
        thenExecute { player.connection.teleport(pos.x + 1.5, pos.y + 0.7, pos.z + 4.0, 180f, 8f) }
        thenIdle(40)
        thenOnClient {
            val client = level!!.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity
            check(client.displayedItems.map { it.count } == listOf(12, 3, 2)) { "Offline-owner initial tracking state missing" }
            reloadResourcePacks()
        }
        thenIdle(60)
        thenOnClient {
            for (p in listOf(pos, otherPos)) check(blockEntityRenderDispatcher.getRenderer(level!!.getBlockEntity(p) as AutomaticRewardBoxBlockEntity) is AutomaticRewardBoxRenderer)
        }
        thenScreenshot("resource-reload-adjacent-shops")
        thenExecute { player.connection.teleport(pos.x + 1.0, pos.y + 0.5, pos.z - 3.0, 0f, 0f) }
        thenIdle(10)
        thenScreenshot("opposite-view")
        thenExecute {
            box.ownerPlayerUUID = player.uuid
            player.connection.teleport(pos.x + 0.5, pos.y + 0.5, pos.z + 3.0, 180f, -20f)
        }
        thenIdle(10)
        thenScreenshot("above-block-culling")
        thenExecute { open(player, pos) }
        thenIdle(10)
        for (scale in listOf(1, 2, 3)) {
            thenOnClient {
                options.guiScale().set(scale)
                resizeDisplay()
                val screen = screen as AutomaticRewardBoxScreen
                val button = screen.children().filterIsInstance<Button>().single { it.message.string.startsWith("Trade display:") }
                check(button.x >= 0 && button.y >= 0 && button.x + button.width <= screen.width && button.y + button.height <= screen.height)
                screen.setFocused(button)
                check(button.isFocused)
            }
            thenScreenshot("toggle-ui-scale-$scale", true)
        }
        thenOnClient {
            options.guiScale().set(oldGuiScale)
            resizeDisplay()
        }
        thenExecute {
            player.closeContainer()
            helper.level.removeBlock(pos, false)
            helper.level.removeBlock(otherPos, false)
            helper.level.removeBlock(manualPos, false)
        }
    }

    private fun open(player: ServerPlayer, pos: BlockPos) {
        player.level().getBlockState(pos).use(player.level(), player, InteractionHand.MAIN_HAND, BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
    }
}

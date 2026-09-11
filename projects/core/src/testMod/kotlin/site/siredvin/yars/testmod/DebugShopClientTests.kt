package site.siredvin.yars.testmod

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import site.siredvin.testiarium.api.ClientGameTest
import site.siredvin.testiarium.api.TestGroup
import site.siredvin.testiarium.api.TestTags
import site.siredvin.testiarium.api.Timeouts
import site.siredvin.testiarium.api.sequence
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.block.DebugShops
import site.siredvin.yars.common.rewardshop.DebugShopTrades
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot
import java.util.UUID

object DebugShopClientTests {
    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 40)
    @TestGroup(TestTags.CLIENT)
    fun nativeDebugShops(helper: GameTestHelper) = helper.sequence {
        val manualPos = helper.absolutePos(BlockPos(1, 1, 1))
        val automaticPos = helper.absolutePos(BlockPos(3, 1, 1))
        lateinit var player: ServerPlayer
        lateinit var box: AutomaticRewardBoxBlockEntity
        thenExecute {
            val loader = if (System.getProperty("testiarium.gametest-report").contains("/fabric/")) "fabric" else "forge"
            System.setProperty("testiarium.screenshots", "/tmp/yars-debug-client/$loader")
            java.io.File(System.getProperty("testiarium.screenshots")).mkdirs()
            player = helper.level.randomPlayer as ServerPlayer
            player.closeContainer()
            player.inventory.clearContent()
            player.setGameMode(GameType.CREATIVE)
            for (shop in listOf(DebugShops.MANUAL_ID, DebugShops.AUTOMATIC_ID)) {
                check(player.server.commands.performPrefixedCommand(player.createCommandSourceStack().withPermission(4), "give @s $shop") == 1)
                check(player.inventory.contains(ItemStack(BuiltInRegistries.ITEM.get(shop))))
            }
            player.connection.teleport(manualPos.x + 1.5, manualPos.y + 1.0, manualPos.z + 3.5, 180f, 15f)
            DebugShopTrades.registration().replaceTrades()
            place(player, manualPos, DebugShops.MANUAL_ID)
            place(player, automaticPos, DebugShops.AUTOMATIC_ID)
            player.inventory.setItem(1, ItemStack(BuiltInRegistries.ITEM.get(DebugShops.MANUAL_ID)))
            box = helper.level.getBlockEntity(automaticPos) as AutomaticRewardBoxBlockEntity
            check(box.ownerPlayerUUID == player.uuid) { "Native placement did not assign the owner" }
            check(box.canOpen(player))
            val owner = box.ownerPlayerUUID
            box.ownerPlayerUUID = UUID.randomUUID()
            check(!box.canOpen(player))
            box.ownerPlayerUUID = owner
            player.setGameMode(GameType.SURVIVAL)
            open(player, manualPos)
            check((player.containerMenu as MerchantMenu).offers.size >= 6)
        }
        thenIdle(5)
        thenOnClient {
            check(this.player?.containerMenu is MerchantMenu)
            check(screen?.title?.string == "Debug Reward Shop")
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(level!!.enabledFeatures(), true, level!!.registryAccess())
            val tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation("minecraft", "functional_blocks"))!!
            for (shop in listOf(DebugShops.MANUAL_ID, DebugShops.AUTOMATIC_ID)) {
                val item = BuiltInRegistries.ITEM.get(shop)
                check(tab.displayItems.any { it.item == item }) { "Missing Creative entry: $shop" }
                check(itemRenderer.getModel(ItemStack(item), level, this.player, 0) !== modelManager.missingModel) { "Missing item model: $shop" }
            }
        }
        thenScreenshot("native-debug-manual-menu", true)
        thenExecute {
            val history = RewardShopTradeHistory.get(helper.level)
            val shared = id("shared/staged")
            val before = history.completed(player.uuid, shared)
            purchase(player, manualPos, "shared/staged")
            check(history.completed(player.uuid, shared) == before + 1)
            player.closeContainer()
            check(box.selectTrade(player, shared))
            val resolved = box.currentTrade()!!
            box.setItem(0, resolved.firstCost.copy())
            check(history.completed(player.uuid, shared) == before + 2)
            purchase(player, manualPos, "shared/staged")
            check(history.completed(player.uuid, shared) == before + 3)
            for (path in listOf("shared/basic", "shared/two_costs", "shared/dynamic", "shared/same_item_costs", "shared/token_source", "shared/token_exchange")) {
                purchase(player, manualPos, path)
            }
            for (path in listOf("shared/limited", "manual/limited")) {
                while (RewardShopTrades.find(id(path))!!.resolve(history.completed(player.uuid, id(path))) != null) purchase(player, manualPos, path)
            }
            check(history.completed(player.uuid, id("automatic/limited")) == 0)
            check(history.completed(UUID.randomUUID(), shared) == 0)
            player.closeContainer()
            box.clearContent()
            check(box.selectTrade(player, id("shared/token_exchange")))
            check(!box.canPlaceItem(0, ItemStack(Items.PAPER)))
            check(box.canPlaceItem(0, DebugShopTrades.token()))
            check(box.selectTrade(player, id("shared/same_item_costs")))
            box.setItem(0, ItemStack(Items.COBBLESTONE, 4))
            check(box.getItem(0).count == 4 && box.getItem(2).isEmpty)
            check(box.selectTrade(player, id("shared/dynamic")))
            check(box.getItem(0).count == 4) { "Changing selection discarded unmatched inputs" }
            check(!box.canPlaceItem(0, ItemStack(Items.COBBLESTONE)))
            check(box.selectTrade(player, id("shared/same_item_costs")))
            box.setItem(0, ItemStack(Items.COBBLESTONE, 5))
            check(box.getItem(0).isEmpty && box.getItem(2).item == Items.COPPER_INGOT)
            box.clearContent()
            check(box.selectTrade(player, id("shared/basic")))
            for (slot in 2..5) box.setItem(slot, ItemStack(Items.EMERALD, 64))
            box.setItem(0, ItemStack(Items.COBBLESTONE))
            check(box.getItem(0).count == 1)
            box.extractOutput(2, 1, false)
            check(box.getItem(0).isEmpty && box.getItem(2).count == 64)
            box.clearContent()
            open(player, automaticPos)
            check(player.containerMenu is AutomaticRewardBoxMenu)
        }
        thenIdle(5)
        thenOnClient {
            check(this.player?.containerMenu is AutomaticRewardBoxMenu)
            check(screen?.title?.string == "Debug Automatic Reward Box")
        }
        thenScreenshot("native-debug-automatic-menu", true)
        thenOnClient {
            val automaticScreen = screen as site.siredvin.yars.client.AutomaticRewardBoxScreen
            automaticScreen.mouseClicked(((automaticScreen.width - 276) / 2 + 10).toDouble(), ((automaticScreen.height - 166) / 2 + 40).toDouble(), 0)
        }
        thenWaitUntil {
            if (box.selectedTradeId != id("shared/two_costs")) throw net.minecraft.gametest.framework.GameTestAssertException("Waiting for native trade selection packet")
        }
        thenExecute {
            player.closeContainer()
            player.setGameMode(GameType.CREATIVE)
            player.abilities.flying = true
            player.onUpdateAbilities()
            player.connection.teleport(automaticPos.x + 0.5, automaticPos.y + 1.5, automaticPos.z + 2.5, 180f, 45f)
        }
        thenIdle(60)
        thenScreenshot("native-debug-automatic-jade", true)
        thenExecute {
            player.closeContainer()
            check(box.selectTrade(player, id("shared/basic")))
            helper.level.setBlockAndUpdate(automaticPos.above(), Blocks.HOPPER.defaultBlockState())
            helper.level.setBlockAndUpdate(automaticPos.below(), Blocks.HOPPER.defaultBlockState())
            (helper.level.getBlockEntity(automaticPos.above()) as HopperBlockEntity).setItem(0, ItemStack(Items.COBBLESTONE, 2))
        }
        thenIdle(50)
        thenExecute {
            val output = helper.level.getBlockEntity(automaticPos.below()) as HopperBlockEntity
            check((0 until output.containerSize).sumOf { output.getItem(it).count } == 2) { "Native hopper transfer failed" }
            check(output.getItem(0).item == Items.EMERALD)
            helper.level.removeBlock(automaticPos.above(), false)
            helper.level.removeBlock(automaticPos.below(), false)
            player.connection.teleport(manualPos.x + 1.5, manualPos.y + 1.0, manualPos.z + 3.5, 180f, 15f)
        }
        thenIdle(10)
        thenScreenshot("native-debug-shops-world", false)
        thenOnClient { reloadResourcePacks() }
        thenIdle(60)
        thenScreenshot("native-debug-shops-reloaded", false)
        thenExecute {
            box.clearContent()
            check(box.selectTrade(player, id("shared/token_exchange")))
            val contents = listOf(ItemStack(Items.COBBLESTONE, 3), ItemStack(Items.STICK, 4), ItemStack(Items.DIAMOND, 2))
            contents.forEachIndexed { slot, stack -> box.setItem(slot, stack.copy()) }
            val bounds = net.minecraft.world.phys.AABB(automaticPos).inflate(2.0)
            fun dropped(item: net.minecraft.world.item.Item) = helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, bounds).sumOf {
                if (it.item.item == item) it.item.count else 0
            }
            val before = contents.associate { it.item to dropped(it.item) }
            helper.level.destroyBlock(automaticPos, true)
            for (stack in contents) check(dropped(stack.item) - before.getValue(stack.item) == stack.count) { "Breaking native box lost or duplicated inventory" }
        }
    }

    private fun id(path: String) = ResourceLocation("yars", "debug/$path")

    private fun place(player: ServerPlayer, pos: BlockPos, id: ResourceLocation) {
        val stack = ItemStack(BuiltInRegistries.ITEM.get(id))
        check(stack.item is BlockItem)
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)
        val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        check((stack.item as BlockItem).place(BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit)).consumesAction()) { "Could not place $id" }
    }

    private fun open(player: ServerPlayer, pos: BlockPos) {
        player.level().getBlockState(pos).use(player.level(), player, InteractionHand.MAIN_HAND, BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
    }

    private fun purchase(player: ServerPlayer, pos: BlockPos, path: String) {
        player.closeContainer()
        open(player, pos)
        val history = RewardShopTradeHistory.get(player.serverLevel())
        val tradeId = id(path)
        val available = RewardShopTrades.all(DebugShops.MANUAL_ID).filter { it.resolve(history.completed(player.uuid, it.id)) != null }
        val index = available.indexOfFirst { it.id == tradeId }
        check(index >= 0)
        val offer = available[index].resolve(history.completed(player.uuid, tradeId))!!
        val before = history.completed(player.uuid, tradeId)
        val menu = player.containerMenu as MerchantMenu
        menu.setSelectionHint(index)
        if (path == "shared/token_exchange") {
            menu.getSlot(0).set(ItemStack(Items.PAPER))
            check(menu.getSlot(2).item.isEmpty) { "Ordinary paper satisfied an NBT-sensitive payment" }
        }
        menu.getSlot(0).set(offer.firstCost.copy())
        menu.getSlot(1).set(offer.secondCost?.copy() ?: ItemStack.EMPTY)
        if (path == "shared/basic") {
            menu.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, player)
            check(!menu.carried.isEmpty) { "Normal click did not pick up the result" }
        } else {
            check(!menu.quickMoveStack(player, 2).isEmpty) { "Native purchase failed: $path" }
        }
        check(history.completed(player.uuid, tradeId) == before + 1)
    }
}

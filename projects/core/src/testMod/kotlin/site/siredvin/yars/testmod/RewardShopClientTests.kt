package site.siredvin.yars.testmod

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.MenuType
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
import site.siredvin.yars.client.AutomaticRewardBoxScreen
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot
import java.util.UUID

object RewardShopClientTests {
    private val shopId = ResourceLocation("yars_test", "reward_shop")

    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun rewardShopAppearance(helper: GameTestHelper) = helper.sequence {
        val fletchingId = ResourceLocation("yars_test", "fletching_rewards")
        val cartographerId = ResourceLocation("yars_test", "cartographer_rewards")
        lateinit var fletchingPos: BlockPos
        lateinit var cartographerPos: BlockPos
        thenExecute {
            fletchingPos = helper.absolutePos(BlockPos(1, 1, 1))
            cartographerPos = helper.absolutePos(BlockPos(3, 1, 1))
            helper.level.setBlockAndUpdate(fletchingPos, BuiltInRegistries.BLOCK.get(fletchingId).defaultBlockState())
            helper.level.setBlockAndUpdate(cartographerPos, BuiltInRegistries.BLOCK.get(cartographerId).defaultBlockState())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.connection.teleport(fletchingPos.x + 1.5, fletchingPos.y + 1.0, fletchingPos.z + 3.5, 180f, 15f)
        }
        thenIdle(2)
        thenScreenshot("reward-shop-tables", showGui = false)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            useShopBlock(player, fletchingPos)
        }
        thenIdle(2)
        thenOnClient { check(player?.containerMenu?.type == MenuType.MERCHANT) { "Fletching reward shop did not open" } }
        thenScreenshot("fletching-reward-shop-ui", showGui = true)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            useShopBlock(player, cartographerPos)
        }
        thenIdle(2)
        thenOnClient { check(player?.containerMenu?.type == MenuType.MERCHANT) { "Cartographer reward shop did not open" } }
        thenScreenshot("cartographer-reward-shop-ui", showGui = true)
    }

    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun rewardShopLifecycle(helper: GameTestHelper) = helper.sequence {
        lateinit var shopPos: BlockPos
        val cappedId = ResourceLocation("yars_test", "capped_${UUID.randomUUID().toString().replace("-", "")}")
        val staleId = ResourceLocation("yars_test", "stale_${UUID.randomUUID().toString().replace("-", "")}")
        thenExecute {
            shopPos = helper.absolutePos(BlockPos(1, 1, 1))
            helper.level.setBlockAndUpdate(shopPos, BuiltInRegistries.BLOCK.get(shopId).defaultBlockState())
            RewardShopTrades.replace(emptyMap(), emptyMap())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.connection.teleport(shopPos.x + 0.5, shopPos.y + 1.0, shopPos.z + 2.5, 180f, 0f)
            useShopBlock(player, shopPos)
        }
        thenIdle(10)
        thenOnClient {
            check(screen != null) { "Empty reward shop screen did not open" }
            val menu = player?.containerMenu as? MerchantMenu ?: error("Merchant menu did not open")
            check(menu.offers.isEmpty()) { "Unconfigured reward shop exposed offers" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            RewardShopTradeRegistration().apply {
                trade(cappedId.toString())
                    .simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                    .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
                attach(shopId.toString(), cappedId.toString())
                replaceTrades { true }
            }
            useShopBlock(player, shopPos)
        }
        thenIdle(10)
        thenOnClient {
            check(player?.containerMenu?.type == MenuType.MERCHANT) { "Configured merchant menu did not open" }
        }
        thenScreenshot("reward-shop-trade-ui", showGui = true)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.EMERALD))
            val history = RewardShopTradeHistory.get(helper.level)
            check(history.completed(player.uuid, cappedId) == 1) { "Purchase count did not increment" }
            check((player.containerMenu as MerchantMenu).offers.single().costA.item === Items.IRON_INGOT) {
                "Offers did not refresh to the second stage"
            }
            player.closeContainer()
            useShopBlock(player, shopPos)
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.IRON_INGOT))
            check(history.completed(player.uuid, cappedId) == 2) { "Second purchase count was not recorded" }
            check((player.containerMenu as MerchantMenu).offers.isEmpty()) { "Capped trade remained available" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            player.inventory.clearContent()
            RewardShopTradeRegistration().apply {
                trade(staleId.toString()).simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                attach(shopId.toString(), staleId.toString())
                replaceTrades { true }
            }
            useShopBlock(player, shopPos)
            RewardShopTradeHistory.get(helper.level).increment(player.uuid, staleId)
            val menu = player.containerMenu as MerchantMenu
            menu.getSlot(0).set(ItemStack(Items.EMERALD))
            val result = menu.quickMoveStack(player, 2)
            check(result.isEmpty) { "Stale offer returned a result" }
            check(player.inventory.countItem(Items.DIAMOND) == 0) { "Stale offer granted its result" }
            check(menu.getSlot(0).item.count == 1) { "Stale offer consumed its payment" }
            check(RewardShopTradeHistory.get(helper.level).completed(player.uuid, staleId) == 1) { "Stale offer changed purchase history" }
        }
    }

    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun automaticRewardBoxSelectionAndExecution(helper: GameTestHelper) = helper.sequence {
        val automaticId = ResourceLocation("yars_test", "automatic_reward_box")
        val selectedId = ResourceLocation("yars_test", "selected_${UUID.randomUUID().toString().replace("-", "")}")
        val replacementId = ResourceLocation("yars_test", "replacement_${UUID.randomUUID().toString().replace("-", "")}")
        val offlineId = ResourceLocation("yars_test", "offline_${UUID.randomUUID().toString().replace("-", "")}")
        val offlineOwner = UUID.randomUUID()
        lateinit var boxPos: BlockPos
        lateinit var interactivePos: BlockPos
        thenExecute {
            boxPos = helper.absolutePos(BlockPos(1, 1, 1))
            interactivePos = helper.absolutePos(BlockPos(3, 1, 1))
            helper.level.setBlockAndUpdate(boxPos, BuiltInRegistries.BLOCK.get(automaticId).defaultBlockState())
            helper.level.setBlockAndUpdate(interactivePos, BuiltInRegistries.BLOCK.get(shopId).defaultBlockState())
            RewardShopTrades.replace(emptyMap(), emptyMap())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.connection.teleport(boxPos.x + 0.5, boxPos.y + 1.0, boxPos.z + 2.5, 180f, 0f)
            useShopBlock(player, boxPos)
            check(player.containerMenu !is AutomaticRewardBoxMenu) { "Ownerless automatic reward box opened" }
            (helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity).player = player
            useShopBlock(player, boxPos)
        }
        thenIdle(10)
        thenOnClient {
            val menu = player?.containerMenu as? AutomaticRewardBoxMenu ?: error("Automatic reward box menu did not open")
            check(net.minecraft.client.Minecraft.getInstance().screen is AutomaticRewardBoxScreen) { "Automatic reward box did not open its merchant-derived screen" }
            check(menu.offers.isEmpty()) { "Unconfigured automatic reward box exposed offers" }
            check(menu.slots.size == 45) { "Automatic reward box menu did not expose all six storage slots" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            RewardShopTradeRegistration().apply {
                trade(selectedId.toString())
                    .simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                    .simple(-1, ItemStack(Items.EMERALD, 4), ItemStack(Items.DIAMOND))
                trade(replacementId.toString()).simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
                attach(shopId.toString(), selectedId.toString())
                attach(automaticId.toString(), selectedId.toString())
                attach(automaticId.toString(), replacementId.toString())
                replaceTrades { true }
            }
            useShopBlock(player, interactivePos)
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.EMERALD))
            check(RewardShopTradeHistory.get(helper.level).completed(player.uuid, selectedId) == 1) { "Interactive trade did not advance shared history" }
            player.closeContainer()
            player.inventory.setItem(9, ItemStack(Items.EMERALD, 4))
            useShopBlock(player, boxPos)
        }
        thenIdle(10)
        thenOnClient {
            val screen = net.minecraft.client.Minecraft.getInstance().screen as? AutomaticRewardBoxScreen ?: error("Automatic reward box screen closed")
            screen.mouseClicked(((screen.width - 276) / 2 + 10).toDouble(), ((screen.height - 166) / 2 + 20).toDouble(), 0)
            check(screen.menu.getSlot(0).item.isEmpty && screen.menu.getSlot(1).item.isEmpty) { "Trade selection moved payment into hidden merchant slots" }
        }
        thenWaitUntil {
            val box = helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity
            if (box.selectedTradeId != selectedId) throw GameTestAssertException("Offer selection has not reached the server")
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            val menu = player.containerMenu as MerchantMenu
            val box = helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity
            check(menu.getSlot(0).item.isEmpty && menu.getSlot(2).item.isEmpty) { "Selection interface moved payment or result items" }
            check(!menu.quickMoveStack(player, 3).isEmpty) { "Payment could not be shift-clicked into box storage" }
            check(box.getItem(0).isEmpty && box.getItem(2).item === Items.DIAMOND) { "Selected trade did not execute into output storage" }
            menu.setSelectionHint(1)
            check(box.selectedTradeId == replacementId) { "Replacement selection was not stored" }
            player.closeContainer()
            useShopBlock(player, boxPos)
        }
        thenIdle(10)
        thenOnClient {
            val menu = player?.containerMenu as? AutomaticRewardBoxMenu ?: error("Automatic reward box did not reopen")
            check(menu.selectedTradeIndex == 0) { "Reopened automatic reward box did not restore its selected trade" }
        }
        thenScreenshot("automatic-reward-box-selection", showGui = true)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            val menu = player.containerMenu as MerchantMenu
            RewardShopTrades.replace(emptyMap(), emptyMap())
            menu.setSelectionHint(0)
            val box = helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity
            check(box.selectedTradeId == replacementId) { "Stale offer changed the selection" }
            check(box.getItem(2).item === Items.DIAMOND) { "Reload removed stored output" }
            check(!menu.quickMoveStack(player, 41).isEmpty) { "Output could not be shift-clicked into player inventory" }
            check(box.getItem(2).isEmpty && player.inventory.contains(ItemStack(Items.DIAMOND))) { "Shift-click did not transfer stored output" }
            RewardShopTradeRegistration().apply {
                trade(selectedId.toString()).simple(-1, ItemStack(Items.EMERALD, 4), ItemStack(Items.DIAMOND))
                trade(replacementId.toString()).simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
                trade(offlineId.toString()).simple(-1, ItemStack(Items.COAL), ItemStack(Items.REDSTONE))
                attach(automaticId.toString(), selectedId.toString())
                attach(automaticId.toString(), replacementId.toString())
                attach(automaticId.toString(), offlineId.toString())
                replaceTrades { true }
            }
            check(box.selectTrade(player, selectedId)) { "Restored trade could not be selected for Jade display" }
            check(box.selectTrade(player, offlineId)) { "Offline test trade could not be selected" }
            box.ownerPlayerUUID = offlineOwner
            box.setItem(0, ItemStack(Items.COAL))
            check(RewardShopTradeHistory.get(helper.level).completed(offlineOwner, offlineId) == 1) { "Offline-owner trade did not execute" }
            check(box.getItem(2).item === Items.REDSTONE) { "Offline-owner result was not stored" }
            player.closeContainer()
            player.connection.teleport(boxPos.x + 0.5, boxPos.y + 1.5, boxPos.z + 2.5, 180f, 45f)
        }
        thenIdle(100)
        thenScreenshot("automatic-reward-box-jade", showGui = true)
    }
}

object YarsTests {
    @JvmStatic
    fun register() {
        site.siredvin.testiarium.Testiarium.register(RewardShopClientTests::class.java)
        site.siredvin.testiarium.Testiarium.register(DebugShopClientTests::class.java)
        site.siredvin.testiarium.Testiarium.register(AutomaticShopDisplayClientTests::class.java)
    }
}

private fun useShopBlock(player: net.minecraft.world.entity.player.Player, shopPos: BlockPos) {
    val state = player.level().getBlockState(shopPos)
    state.use(
        player.level(),
        player,
        InteractionHand.MAIN_HAND,
        BlockHitResult(Vec3.atCenterOf(shopPos), Direction.UP, shopPos, false),
    )
}

private fun completeTrade(menu: MerchantMenu, player: net.minecraft.world.entity.player.Player, payment: ItemStack) {
    player.inventory.add(payment)
    menu.setSelectionHint(0)
    menu.tryMoveItems(0)
    check(!menu.getSlot(2).item.isEmpty) { "Payment did not match an offer" }
    check(!menu.quickMoveStack(player, 2).isEmpty) { "Trade did not produce its result" }
}

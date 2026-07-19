package site.siredvin.yars.testmod

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
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
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot

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
        lateinit var savedPlayer: CompoundTag
        thenExecute {
            shopPos = helper.absolutePos(BlockPos(1, 1, 1))
            helper.level.setBlockAndUpdate(shopPos, BuiltInRegistries.BLOCK.get(shopId).defaultBlockState())
            RewardShopTrades.replace(emptyMap())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            persistentData(player).remove(RewardShopTradeHistory.ROOT_KEY)
            player.connection.teleport(shopPos.x + 0.5, shopPos.y + 1.0, shopPos.z + 2.5, 180f, 0f)
            useShopBlock(player, shopPos)
        }
        thenIdle(2)
        thenOnClient {
            check(screen != null) { "Empty reward shop screen did not open" }
            val menu = player?.containerMenu as? MerchantMenu ?: error("Merchant menu did not open")
            check(menu.offers.isEmpty()) { "Unconfigured reward shop exposed offers" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            RewardShopTradeRegistration().apply {
                shop(shopId.toString()) {
                    it.trade("capped")
                        .simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                        .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
                }
                replaceTrades { true }
            }
            useShopBlock(player, shopPos)
        }
        thenIdle(2)
        thenOnClient {
            check(player?.containerMenu?.type == MenuType.MERCHANT) { "Configured merchant menu did not open" }
        }
        thenScreenshot("reward-shop-trade-ui", showGui = true)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.EMERALD))
            check(RewardShopTradeHistory.completed(player, shopId, "capped") == 1) { "Purchase count did not increment" }
            check((player.containerMenu as MerchantMenu).offers.single().costA.item === Items.IRON_INGOT) {
                "Offers did not refresh to the second stage"
            }
            savedPlayer = player.saveWithoutId(CompoundTag()).copy()
            persistentData(player).remove(RewardShopTradeHistory.ROOT_KEY)
            check(RewardShopTradeHistory.completed(player, shopId, "capped") == 0) { "Test could not clear live history" }
            player.load(savedPlayer)
            check(RewardShopTradeHistory.completed(player, shopId, "capped") == 1) { "Purchase count did not survive player reload" }
            player.closeContainer()
            useShopBlock(player, shopPos)
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.IRON_INGOT))
            check(RewardShopTradeHistory.completed(player, shopId, "capped") == 2) { "Second purchase count was not recorded" }
            check((player.containerMenu as MerchantMenu).offers.isEmpty()) { "Capped trade remained available" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            player.inventory.clearContent()
            RewardShopTradeRegistration().apply {
                shop(shopId.toString()) { it.trade("stale").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND)) }
                replaceTrades { true }
            }
            useShopBlock(player, shopPos)
            RewardShopTradeHistory.increment(player, shopId, "stale")
            val menu = player.containerMenu as MerchantMenu
            menu.getSlot(0).set(ItemStack(Items.EMERALD))
            val result = menu.quickMoveStack(player, 2)
            check(result.isEmpty) { "Stale offer returned a result" }
            check(player.inventory.countItem(Items.DIAMOND) == 0) { "Stale offer granted its result" }
            check(menu.getSlot(0).item.count == 1) { "Stale offer consumed its payment" }
            check(RewardShopTradeHistory.completed(player, shopId, "stale") == 1) { "Stale offer changed purchase history" }
        }
    }

    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun automaticRewardBoxSelectionAndExecution(helper: GameTestHelper) = helper.sequence {
        val automaticId = ResourceLocation("yars_test", "automatic_reward_box")
        lateinit var boxPos: BlockPos
        thenExecute {
            boxPos = helper.absolutePos(BlockPos(1, 1, 1))
            helper.level.setBlockAndUpdate(boxPos, BuiltInRegistries.BLOCK.get(automaticId).defaultBlockState())
            RewardShopTrades.replace(emptyMap())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.connection.teleport(boxPos.x + 0.5, boxPos.y + 1.0, boxPos.z + 2.5, 180f, 0f)
            useShopBlock(player, boxPos)
        }
        thenIdle(2)
        thenOnClient {
            val menu = player?.containerMenu as? MerchantMenu ?: error("Automatic reward box menu did not open")
            check(menu.offers.isEmpty()) { "Unconfigured automatic reward box exposed offers" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            RewardShopTradeRegistration().apply {
                shop(automaticId.toString()) {
                    it.trade("selected").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                    it.trade("replacement").simple(-1, ItemStack(Items.IRON_INGOT), ItemStack(Items.GOLD_INGOT))
                }
                replaceTrades { true }
            }
            useShopBlock(player, boxPos)
            val menu = player.containerMenu as MerchantMenu
            menu.setSelectionHint(0)
            menu.tryMoveItems(0)
            val box = helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity
            check(box.selectedTradeId == "selected") { "Offer selection was not stored" }
            check(menu.getSlot(0).item.isEmpty && menu.getSlot(2).item.isEmpty) { "Selection interface moved payment or result items" }
            box.setItem(0, ItemStack(Items.EMERALD))
            check(box.getItem(0).isEmpty && box.getItem(2).item === Items.DIAMOND) { "Selected trade did not execute into output storage" }
            menu.setSelectionHint(1)
            check(box.selectedTradeId == "replacement") { "Replacement selection was not stored" }
        }
        thenIdle(2)
        thenScreenshot("automatic-reward-box-selection", showGui = true)
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            val menu = player.containerMenu as MerchantMenu
            RewardShopTrades.replace(emptyMap())
            menu.setSelectionHint(0)
            val box = helper.level.getBlockEntity(boxPos) as AutomaticRewardBoxBlockEntity
            check(box.selectedTradeId == "replacement") { "Stale offer changed the selection" }
            check(box.getItem(2).item === Items.DIAMOND) { "Reload removed stored output" }
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

private fun persistentData(player: net.minecraft.world.entity.player.Player): CompoundTag = player.javaClass.getMethod("kjs\$getPersistentData").invoke(player) as CompoundTag

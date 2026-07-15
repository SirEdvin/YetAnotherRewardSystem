package site.siredvin.yars.testmod

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
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
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.common.setup.Blocks
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot

object RewardShopClientTests {
    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun rewardShopLifecycle(helper: GameTestHelper) = helper.sequence {
        lateinit var shopPos: BlockPos
        lateinit var savedPlayer: CompoundTag
        thenExecute {
            shopPos = helper.absolutePos(BlockPos(1, 1, 1))
            helper.level.setBlockAndUpdate(shopPos, Blocks.REWARD_SHOP.get().defaultBlockState())
            RewardShopTrades.replace(emptyList())
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
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
                trade("capped")
                    .simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                    .simple(1, ItemStack(Items.IRON_INGOT), ItemStack(Items.DIAMOND))
                replaceTrades()
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
            check(RewardShopTradeHistory.completed(player, "capped") == 1) { "Purchase count did not increment" }
            check((player.containerMenu as MerchantMenu).offers.single().costA.item === Items.IRON_INGOT) {
                "Offers did not refresh to the second stage"
            }
            savedPlayer = player.saveWithoutId(CompoundTag()).copy()
            persistentData(player).remove(RewardShopTradeHistory.ROOT_KEY)
            check(RewardShopTradeHistory.completed(player, "capped") == 0) { "Test could not clear live history" }
            player.load(savedPlayer)
            check(RewardShopTradeHistory.completed(player, "capped") == 1) { "Purchase count did not survive player reload" }
            player.closeContainer()
            useShopBlock(player, shopPos)
            completeTrade(player.containerMenu as MerchantMenu, player, ItemStack(Items.IRON_INGOT))
            check(RewardShopTradeHistory.completed(player, "capped") == 2) { "Second purchase count was not recorded" }
            check((player.containerMenu as MerchantMenu).offers.isEmpty()) { "Capped trade remained available" }
        }
        thenExecute {
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.closeContainer()
            player.inventory.clearContent()
            RewardShopTradeRegistration().apply {
                trade("stale").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
                replaceTrades()
            }
            useShopBlock(player, shopPos)
            RewardShopTradeHistory.increment(player, "stale")
            val menu = player.containerMenu as MerchantMenu
            menu.getSlot(0).set(ItemStack(Items.EMERALD))
            val result = menu.quickMoveStack(player, 2)
            check(result.isEmpty) { "Stale offer returned a result" }
            check(player.inventory.countItem(Items.DIAMOND) == 0) { "Stale offer granted its result" }
            check(menu.getSlot(0).item.count == 1) { "Stale offer consumed its payment" }
            check(RewardShopTradeHistory.completed(player, "stale") == 1) { "Stale offer changed purchase history" }
        }
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
    menu.getSlot(0).set(payment)
    check(!menu.quickMoveStack(player, 2).isEmpty) { "Trade did not produce its result" }
}

private fun persistentData(player: net.minecraft.world.entity.player.Player): CompoundTag = player.javaClass.getMethod("kjs\$getPersistentData").invoke(player) as CompoundTag

package site.siredvin.yars.testmod

import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import site.siredvin.testiarium.api.ClientGameTest
import site.siredvin.testiarium.api.TestGroup
import site.siredvin.testiarium.api.TestTags
import site.siredvin.testiarium.api.Timeouts
import site.siredvin.testiarium.api.sequence
import site.siredvin.yars.common.rewardshop.RewardShopMerchant
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.setup.Blocks
import site.siredvin.yars.testmod.client.thenOnClient
import site.siredvin.yars.testmod.client.thenScreenshot

object RewardShopClientTests {
    @JvmStatic
    @ClientGameTest(template = "empty", timeoutTicks = Timeouts.SECOND * 20)
    @TestGroup(TestTags.CLIENT)
    fun tradeScreen(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            val shopPos = helper.absolutePos(net.minecraft.core.BlockPos(1, 1, 1))
            helper.level.setBlockAndUpdate(shopPos, Blocks.REWARD_SHOP.get().defaultBlockState())
            RewardShopTradeRegistration().apply {
                trade("emeralds").simple(-1, ItemStack(Items.DIAMOND, 3), ItemStack(Items.EMERALD, 12))
                trade("golden_apple").simple(-1, ItemStack(Items.GOLD_INGOT, 8), ItemStack(Items.GOLDEN_APPLE))
                trade("beacon").simple(-1, ItemStack(Items.NETHER_STAR), ItemStack(Items.BEACON))
                replaceTrades()
            }
            val player = helper.level.randomPlayer ?: throw GameTestAssertException("Player does not exist")
            player.connection.teleport(shopPos.x + 0.5, shopPos.y + 1.0, shopPos.z + 2.5, 180f, 0f)
            RewardShopMerchant(player, shopPos).openTradingScreen(player, Component.translatable("block.yars.reward_shop"), 1)
        }
        thenIdle(5)
        thenOnClient {
            check(screen != null) { "Trade screen did not open" }
            check(player?.containerMenu?.type == MenuType.MERCHANT) { "Merchant menu did not open" }
        }
        thenScreenshot("reward-shop-trade-ui", showGui = true)
    }
}

package site.siredvin.yars.testmod

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.common.capabilities.ForgeCapabilities
import site.siredvin.testiarium.api.ClientGameTest
import site.siredvin.testiarium.api.TestGroup
import site.siredvin.testiarium.api.TestTags
import site.siredvin.testiarium.api.immediate
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import java.util.UUID

object ForgeAutomaticRewardBoxCapabilityTests {
    @JvmStatic
    @Suppress("DEPRECATION")
    @ClientGameTest(template = "empty")
    @TestGroup(TestTags.CLIENT)
    fun simulationAndCommit(helper: GameTestHelper) = helper.immediate {
        val boxId = ResourceLocation("yars_test:automatic_reward_box")
        val tradeId = ResourceLocation("yars_test", "forge_capability_${UUID.randomUUID().toString().replace("-", "")}")
        val pos = helper.absolutePos(BlockPos(1, 1, 1))
        helper.level.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(boxId).defaultBlockState())
        val player = checkNotNull(helper.level.randomPlayer)
        player.connection.teleport(pos.x + 0.5, pos.y + 1.0, pos.z + 2.5, 180f, 0f)
        val box = helper.level.getBlockEntity(pos) as AutomaticRewardBoxBlockEntity
        box.player = player
        RewardShopTradeRegistration().apply {
            trade(tradeId.toString()).simple(-1, ItemStack(Items.EMERALD, 2), ItemStack(Items.DIAMOND))
            attach(boxId.toString(), tradeId.toString())
            replaceTrades { true }
        }
        check(box.selectTrade(player, tradeId))
        val handler = box.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow { IllegalStateException("Missing item capability") }
        val history = RewardShopTradeHistory.get(helper.level)

        check(handler.insertItem(0, ItemStack(Items.EMERALD, 2), true).isEmpty)
        check(box.getItem(0).isEmpty && history.completed(player.uuid, tradeId) == 0)
        check(handler.insertItem(0, ItemStack(Items.EMERALD), false).isEmpty)
        check(handler.extractItem(0, 1, false).isEmpty)
        check(handler.insertItem(0, ItemStack(Items.COAL), false).count == 1)
        check(handler.insertItem(0, ItemStack(Items.EMERALD), false).isEmpty)
        check(history.completed(player.uuid, tradeId) == 1 && box.getItem(2).item === Items.DIAMOND)
        check(handler.extractItem(2, 1, true).item === Items.DIAMOND && box.getItem(2).item === Items.DIAMOND)
        check(handler.extractItem(2, 1, false).item === Items.DIAMOND)
        check(box.getItem(2).isEmpty)
    }
}

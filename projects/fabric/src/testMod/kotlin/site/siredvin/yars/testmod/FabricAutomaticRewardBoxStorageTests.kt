package site.siredvin.yars.testmod

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import site.siredvin.testiarium.api.ClientGameTest
import site.siredvin.testiarium.api.TestGroup
import site.siredvin.testiarium.api.TestTags
import site.siredvin.testiarium.api.immediate
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import java.util.UUID

object FabricAutomaticRewardBoxStorageTests {
    @JvmStatic
    @ClientGameTest(template = "empty")
    @TestGroup(TestTags.CLIENT)
    fun transactions(helper: GameTestHelper) = helper.immediate {
        val boxId = ResourceLocation("yars_test:automatic_reward_box")
        val tradeId = ResourceLocation("yars_test", "fabric_storage_${UUID.randomUUID().toString().replace("-", "")}")
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
        val storage = checkNotNull(ItemStorage.SIDED.find(helper.level, pos, Direction.UP))
        val emerald = ItemVariant.of(Items.EMERALD)
        val diamond = ItemVariant.of(Items.DIAMOND)
        val history = RewardShopTradeHistory.get(helper.level)

        check(StorageUtil.simulateInsert(storage, emerald, 2, null) == 2L)
        check(box.getItem(0).isEmpty && history.completed(player.uuid, tradeId) == 0)

        Transaction.openOuter().use { transaction ->
            check(storage.insert(emerald, 1, transaction) == 1L)
            transaction.commit()
        }
        Transaction.openOuter().use { transaction -> check(storage.extract(emerald, 1, transaction) == 0L) }
        check(StorageUtil.simulateInsert(storage, ItemVariant.of(Items.COAL), 1, null) == 0L)

        Transaction.openOuter().use { transaction ->
            check(storage.insert(emerald, 1, transaction) == 1L)
            transaction.commit()
        }
        check(history.completed(player.uuid, tradeId) == 1 && box.getItem(2).item === Items.DIAMOND)
        check(StorageUtil.simulateExtract(storage, diamond, 1, null) == 1L && box.getItem(2).item === Items.DIAMOND)
        Transaction.openOuter().use { transaction ->
            check(storage.extract(diamond, 1, transaction) == 1L)
            transaction.commit()
        }
        check(box.getItem(2).isEmpty)
    }
}

package site.siredvin.yars.common.block

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import site.siredvin.yars.common.rewardshop.RewardShopMenu

class RewardShopBlock : Block(BlockBehaviour.Properties.of().strength(2.5f)) {
    @Deprecated("Vanilla's Block#use is deprecated in 1.20.1")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!level.isClientSide) player.openMenu(SimpleMenuProvider({ id, inventory, _ -> RewardShopMenu(id, inventory) }, Component.translatable("block.yars.reward_shop")))
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}

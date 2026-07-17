package site.siredvin.yars.common.block

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import site.siredvin.yars.common.rewardshop.RewardShopMerchant

class RewardShopBlock(properties: Properties, val shopId: ResourceLocation) : Block(properties) {
    @Deprecated("Vanilla's Block#use is deprecated in 1.20.1")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!level.isClientSide) RewardShopMerchant(player, pos, shopId).openTradingScreen(player, state.block.name, 1)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}

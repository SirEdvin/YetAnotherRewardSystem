package site.siredvin.yars.common.block

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class AutomaticRewardBoxBlock(
    properties: Properties,
    override val shopId: ResourceLocation,
    private val entityType: () -> BlockEntityType<*>,
) : BaseEntityBlock(properties),
    RewardShopTarget {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AutomaticRewardBoxBlockEntity(entityType(), pos, state, shopId)

    @Deprecated("Vanilla's Block#getRenderShape is deprecated in 1.20.1")
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    @Deprecated("Vanilla's Block#use is deprecated in 1.20.1")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!level.isClientSide && player is ServerPlayer) {
            val box = level.getBlockEntity(pos) as? AutomaticRewardBoxBlockEntity ?: return InteractionResult.FAIL
            if (player.isShiftKeyDown) player.openMenu(box) else AutomaticRewardBoxMerchant(player, box).openTradingScreen(player, state.block.name, 1)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    @Deprecated("Vanilla's Block#onRemove is deprecated in 1.20.1")
    @Suppress("DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (!state.`is`(newState.block)) {
            (level.getBlockEntity(pos) as? AutomaticRewardBoxBlockEntity)?.let { Containers.dropContents(level, pos, it) }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }
}

package site.siredvin.yars.common.block

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

@Suppress("DEPRECATION")
object DebugShops {
    val MANUAL_ID = ResourceLocation("yars", "debug_reward_shop")
    val AUTOMATIC_ID = ResourceLocation("yars", "debug_automatic_reward_box")

    fun manual() = RewardShopBlock(BlockBehaviour.Properties.of().strength(2f), MANUAL_ID)
    fun automatic(type: () -> BlockEntityType<*>) = AutomaticRewardBoxBlock(BlockBehaviour.Properties.of().strength(2f), AUTOMATIC_ID, type)
}

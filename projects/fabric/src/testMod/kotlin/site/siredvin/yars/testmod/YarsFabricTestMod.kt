package site.siredvin.yars.testmod

import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import site.siredvin.testiarium.FabricTestiarium
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.RewardShopBlock

object YarsFabricTestMod : ModInitializer {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onInitialize() {
        listOf("reward_shop", "fletching_rewards", "cartographer_rewards").forEach { path ->
            val shopId = ResourceLocation("yars_test", path)
            Registry.register(BuiltInRegistries.BLOCK, shopId, RewardShopBlock(BlockBehaviour.Properties.of().strength(2.5f), shopId))
        }
        val automaticId = ResourceLocation("yars_test", "automatic_reward_box")
        lateinit var type: BlockEntityType<*>
        val automaticBlock = Registry.register(
            BuiltInRegistries.BLOCK,
            automaticId,
            AutomaticRewardBoxBlock(BlockBehaviour.Properties.of().strength(2.5f), automaticId) { type },
        )
        type = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            automaticId,
            BlockEntityType.Builder.of({ pos, state -> AutomaticRewardBoxBlockEntity(type, pos, state, automaticId) }, automaticBlock).build(null),
        )
        YarsTests.register()
        FabricTestiarium.registerTests()
    }
}

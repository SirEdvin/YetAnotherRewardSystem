package site.siredvin.yars.testmod

import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockBehaviour
import site.siredvin.testiarium.FabricTestiarium
import site.siredvin.yars.common.block.RewardShopBlock

object YarsFabricTestMod : ModInitializer {
    override fun onInitialize() {
        listOf("reward_shop", "fletching_rewards", "cartographer_rewards").forEach { path ->
            val shopId = ResourceLocation("yars_test", path)
            Registry.register(BuiltInRegistries.BLOCK, shopId, RewardShopBlock(BlockBehaviour.Properties.of().strength(2.5f), shopId))
        }
        YarsTests.register()
        FabricTestiarium.registerTests()
    }
}

package site.siredvin.yars

import dev.latvian.mods.kubejs.core.EntityKJS
import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import site.siredvin.broccolium.FabricBroccolium
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.block.AutomaticRewardBoxMenus
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.fabric.FabricModRecipeIngredients
import site.siredvin.yars.fabric.YarsFabricPlatform

object YarsFabric : ModInitializer {
    override fun onInitialize() {
        FabricBroccolium.sayHi()
        YarsCore.configure(YarsFabricPlatform, FabricModRecipeIngredients)
        RewardShopTradeHistory.configure { (it as EntityKJS).`kjs$getPersistentData`() }
        AutomaticRewardBoxMenus.type = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation(YarsCore.MOD_ID, "automatic_reward_box"),
            MenuType(::AutomaticRewardBoxMenu, FeatureFlags.DEFAULT_FLAGS),
        )
    }
}

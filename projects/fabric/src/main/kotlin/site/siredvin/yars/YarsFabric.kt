package site.siredvin.yars

import net.fabricmc.api.ModInitializer
import site.siredvin.broccolium.FabricBroccolium
import site.siredvin.yars.fabric.FabricModRecipeIngredients
import site.siredvin.yars.fabric.YarsFabricPlatform
import site.siredvin.yars.xplat.ModCommonHooks

object YarsFabric : ModInitializer {
    override fun onInitialize() {
        FabricBroccolium.sayHi()
        YarsCore.configure(YarsFabricPlatform, FabricModRecipeIngredients)
        ModCommonHooks.onRegister()
    }
}

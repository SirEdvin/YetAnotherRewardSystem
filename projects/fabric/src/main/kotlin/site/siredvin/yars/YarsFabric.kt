package site.siredvin.yars

import dev.latvian.mods.kubejs.core.EntityKJS
import net.fabricmc.api.ModInitializer
import site.siredvin.broccolium.FabricBroccolium
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.fabric.FabricModRecipeIngredients
import site.siredvin.yars.fabric.YarsFabricPlatform

object YarsFabric : ModInitializer {
    override fun onInitialize() {
        FabricBroccolium.sayHi()
        YarsCore.configure(YarsFabricPlatform, FabricModRecipeIngredients)
        RewardShopTradeHistory.configure { (it as EntityKJS).`kjs$getPersistentData`() }
    }
}

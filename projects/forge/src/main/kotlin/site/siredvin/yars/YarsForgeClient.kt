package site.siredvin.yars

import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import site.siredvin.yars.common.rewardshop.RewardShopMenus
import site.siredvin.yars.forge.RewardShopScreen

@Mod.EventBusSubscriber(modid = YarsCore.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object YarsForgeClient {
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onClientSetup(event: FMLClientSetupEvent) {
        YarsClientCore.onInit()
        event.enqueueWork { MenuScreens.register(RewardShopMenus.type, ::RewardShopScreen) }
    }

    @SubscribeEvent
    fun registerModels(event: RegisterAdditional) {
        YarsClientCore.registerExtraModels { model: ResourceLocation -> event.register(model) }
    }
}

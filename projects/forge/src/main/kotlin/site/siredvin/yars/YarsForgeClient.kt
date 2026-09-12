package site.siredvin.yars

import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import site.siredvin.yars.client.AutomaticRewardBoxRenderer
import site.siredvin.yars.client.AutomaticRewardBoxScreen
import site.siredvin.yars.common.block.AutomaticRewardBoxMenus

@Mod.EventBusSubscriber(modid = YarsCore.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object YarsForgeClient {
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        YarsClientCore.onInit()
        event.enqueueWork { MenuScreens.register(AutomaticRewardBoxMenus.type, ::AutomaticRewardBoxScreen) }
    }

    @SubscribeEvent
    fun registerModels(event: RegisterAdditional) {
        YarsClientCore.registerExtraModels { model: ResourceLocation -> event.register(model) }
    }

    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        AutomaticRewardBoxRenderer.register { type, provider -> event.registerBlockEntityRenderer(type, provider) }
    }
}

package site.siredvin.yars

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.resources.ResourceLocation
import site.siredvin.yars.client.AutomaticRewardBoxRenderer
import site.siredvin.yars.client.AutomaticRewardBoxScreen
import site.siredvin.yars.common.block.AutomaticRewardBoxMenus

object YarsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        YarsClientCore.onInit()
        MenuScreens.register(AutomaticRewardBoxMenus.type, ::AutomaticRewardBoxScreen)
        ModelLoadingPlugin.register {
            // KubeJS registers Fabric blocks in its client initializer, which may run after ours.
            AutomaticRewardBoxRenderer.register { type, provider -> BlockEntityRenderers.register(type, provider) }
            it.addModels(YarsClientCore.EXTRA_MODELS.map { id -> ResourceLocation(YarsCore.MOD_ID, id) })
        }
    }
}

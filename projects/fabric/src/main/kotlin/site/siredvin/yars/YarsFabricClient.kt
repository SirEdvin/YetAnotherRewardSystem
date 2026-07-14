package site.siredvin.yars

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.resources.ResourceLocation

object YarsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        YarsClientCore.onInit()
        ModelLoadingPlugin.register {
            it.addModels(YarsClientCore.EXTRA_MODELS.map { id -> ResourceLocation(YarsCore.MOD_ID, id) })
        }
    }
}

package site.siredvin.yars

import net.minecraft.resources.ResourceLocation
import java.util.function.Consumer

object YarsClientCore {
    val EXTRA_MODELS = emptyArray<String>()

    @Suppress("DEPRECATION")
    fun registerExtraModels(register: Consumer<ResourceLocation>) {
        EXTRA_MODELS.forEach { register.accept(ResourceLocation(YarsCore.MOD_ID, it)) }
    }

    fun onInit() {}
}

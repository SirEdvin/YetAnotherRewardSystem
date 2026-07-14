package site.siredvin.yars.xplat

import net.minecraft.resources.ResourceLocation
import site.siredvin.broccolium.modules.platform.PlatformToolkit
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.setup.Blocks

object ModCommonHooks {
    @Suppress("DEPRECATION")
    fun onRegister() {
        Blocks.doSomething()
        ModPlatform.registerCreativeTab(
            ResourceLocation(YarsCore.MOD_ID, "tab"),
            YarsCore.configureCreativeTab(PlatformToolkit.get().createTabBuilder()).build(),
        )
    }
}

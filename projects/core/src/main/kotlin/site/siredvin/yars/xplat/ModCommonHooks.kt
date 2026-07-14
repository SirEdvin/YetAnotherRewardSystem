package site.siredvin.yars.xplat

import net.minecraft.resources.ResourceLocation
import site.siredvin.broccolium.modules.platform.PlatformToolkit
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.setup.Blocks
import site.siredvin.yars.common.setup.Items

object ModCommonHooks {
    @Suppress("DEPRECATION")
    fun onRegister() {
        Items.doSomething()
        Blocks.doSomething()
        ModPlatform.registerCreativeTab(
            ResourceLocation(YarsCore.MOD_ID, "tab"),
            YarsCore.configureCreativeTab(PlatformToolkit.get().createTabBuilder()).build(),
        )
    }
}

package site.siredvin.yars.testmod

import net.fabricmc.api.ModInitializer
import site.siredvin.testiarium.FabricTestiarium

object YarsFabricTestMod : ModInitializer {
    override fun onInitialize() {
        YarsTests.register()
        FabricTestiarium.registerTests()
    }
}

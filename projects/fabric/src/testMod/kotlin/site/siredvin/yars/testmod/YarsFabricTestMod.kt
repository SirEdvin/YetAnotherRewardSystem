package site.siredvin.yars.testmod

import net.fabricmc.api.ModInitializer
import site.siredvin.testiarium.FabricTestiarium
import site.siredvin.testiarium.Testiarium

object YarsFabricTestMod : ModInitializer {
    override fun onInitialize() {
        Testiarium.register(RewardShopClientTests::class.java)
        FabricTestiarium.registerTests()
    }
}

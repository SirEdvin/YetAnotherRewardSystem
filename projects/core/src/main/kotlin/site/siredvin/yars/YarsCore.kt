package site.siredvin.yars

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import site.siredvin.broccolium.modules.platform.api.InnerBasePlatform
import site.siredvin.yars.xplat.ModPlatform
import site.siredvin.yars.xplat.ModRecipeIngredients

object YarsCore {
    const val MOD_ID = "yars"

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun configure(platform: InnerBasePlatform, ingredients: ModRecipeIngredients) {
        ModPlatform.configure(platform)
        ModRecipeIngredients.configure(ingredients)
    }
}

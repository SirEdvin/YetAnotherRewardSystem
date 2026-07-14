package site.siredvin.yars

import net.minecraft.world.item.CreativeModeTab
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import site.siredvin.broccolium.modules.platform.api.InnerBasePlatform
import site.siredvin.yars.common.setup.Blocks
import site.siredvin.yars.data.ModText
import site.siredvin.yars.xplat.ModPlatform
import site.siredvin.yars.xplat.ModRecipeIngredients

object YarsCore {
    const val MOD_ID = "yars"

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun configureCreativeTab(builder: CreativeModeTab.Builder): CreativeModeTab.Builder = builder.icon { Blocks.REWARD_SHOP.get().asItem().defaultInstance }
        .title(ModText.CREATIVE_TAB.text)
        .displayItems { _, output ->
            ModPlatform.holder.blocks.forEach { output.accept(it.get()) }
        }

    fun configure(platform: InnerBasePlatform, ingredients: ModRecipeIngredients) {
        ModPlatform.configure(platform)
        ModRecipeIngredients.configure(ingredients)
    }
}

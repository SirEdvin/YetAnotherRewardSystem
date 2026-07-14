package site.siredvin.template

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.registries.DeferredRegister
import site.siredvin.broccolium.ForgeBroccolium
import site.siredvin.template.common.configuration.ConfigHolder
import site.siredvin.template.forge.ForgeModPlatform
import site.siredvin.template.forge.ForgeModRecipeIngredients
import site.siredvin.template.xplat.ModCommonHooks

@Mod(ModCore.MOD_ID)
class ForgeMod(modEventBus: IEventBus, modContainer: ModContainer) {

    companion object {
        val blocksRegistry: DeferredRegister<Block> =
            DeferredRegister.create(BuiltInRegistries.BLOCK, ModCore.MOD_ID)
        val itemsRegistry: DeferredRegister<Item> =
            DeferredRegister.create(BuiltInRegistries.ITEM, ModCore.MOD_ID)
        val creativeTabRegistry: DeferredRegister<CreativeModeTab> =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ModCore.MOD_ID)
    }

    init {
        ForgeBroccolium.sayHi()
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHolder.commonSpec, "${ModCore.MOD_ID}.toml")
        ModCore.configure(ForgeModPlatform, ForgeModRecipeIngredients)
        ModCommonHooks.onRegister()
        blocksRegistry.register(modEventBus)
        itemsRegistry.register(modEventBus)
        creativeTabRegistry.register(modEventBus)
    }
}

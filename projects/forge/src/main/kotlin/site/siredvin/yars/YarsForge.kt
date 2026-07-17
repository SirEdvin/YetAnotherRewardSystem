package site.siredvin.yars

import dev.latvian.mods.kubejs.core.EntityKJS
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import site.siredvin.broccolium.ForgeBroccolium
import site.siredvin.yars.common.configuration.ConfigHolder
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.forge.ForgeModRecipeIngredients
import site.siredvin.yars.forge.YarsForgePlatform
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_CONTEXT

@Mod(YarsCore.MOD_ID)
@Mod.EventBusSubscriber(modid = YarsCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@Suppress("DEPRECATION")
object YarsForge {
    val blocksRegistry: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, YarsCore.MOD_ID)
    val itemsRegistry: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, YarsCore.MOD_ID)
    val creativeTabRegistry: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), YarsCore.MOD_ID)

    init {
        ForgeBroccolium.sayHi()
        LOADING_CONTEXT.registerConfig(ModConfig.Type.COMMON, ConfigHolder.commonSpec, "${YarsCore.MOD_ID}.toml")
        YarsCore.configure(YarsForgePlatform, ForgeModRecipeIngredients)
        RewardShopTradeHistory.configure { (it as EntityKJS).`kjs$getPersistentData`() }
        val eventBus = MOD_CONTEXT.getKEventBus()
        eventBus.addListener(this::commonSetup)
        blocksRegistry.register(eventBus)
        itemsRegistry.register(eventBus)
        creativeTabRegistry.register(eventBus)
    }

    @Suppress("UNUSED_PARAMETER")
    fun commonSetup(event: FMLCommonSetupEvent) {
    }
}

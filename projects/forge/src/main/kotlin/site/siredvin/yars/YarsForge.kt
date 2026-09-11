package site.siredvin.yars

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import site.siredvin.broccolium.ForgeBroccolium
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.block.AutomaticRewardBoxMenus
import site.siredvin.yars.common.block.DebugShops
import site.siredvin.yars.common.configuration.ConfigHolder
import site.siredvin.yars.common.rewardshop.RewardShopTrades
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
    val menusRegistry: DeferredRegister<MenuType<*>> = DeferredRegister.create(ForgeRegistries.MENU_TYPES, YarsCore.MOD_ID)
    val blockEntitiesRegistry: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YarsCore.MOD_ID)
    val debugManual = blocksRegistry.register(DebugShops.MANUAL_ID.path, DebugShops::manual)
    val debugAutomatic = blocksRegistry.register(DebugShops.AUTOMATIC_ID.path) { DebugShops.automatic { debugAutomaticType.get() } }
    val debugAutomaticType: net.minecraftforge.registries.RegistryObject<BlockEntityType<AutomaticRewardBoxBlockEntity>> = blockEntitiesRegistry.register(DebugShops.AUTOMATIC_ID.path) {
        BlockEntityType.Builder.of({ pos, state -> AutomaticRewardBoxBlockEntity(debugAutomaticType.get(), pos, state, DebugShops.AUTOMATIC_ID) }, debugAutomatic.get()).build(com.mojang.datafixers.DSL.remainderType())
    }
    val debugManualItem = itemsRegistry.register(DebugShops.MANUAL_ID.path) { BlockItem(debugManual.get(), Item.Properties()) }
    val debugAutomaticItem = itemsRegistry.register(DebugShops.AUTOMATIC_ID.path) { BlockItem(debugAutomatic.get(), Item.Properties()) }
    val automaticRewardBoxMenu = menusRegistry.register("automatic_reward_box") {
        MenuType<MerchantMenu>({ id, inventory -> AutomaticRewardBoxMenu(id, inventory) }, FeatureFlags.DEFAULT_FLAGS)
    }

    init {
        ForgeBroccolium.sayHi()
        LOADING_CONTEXT.registerConfig(ModConfig.Type.COMMON, ConfigHolder.commonSpec, "${YarsCore.MOD_ID}.toml")
        YarsCore.configure(YarsForgePlatform, ForgeModRecipeIngredients)
        val eventBus = MOD_CONTEXT.getKEventBus()
        MinecraftForge.EVENT_BUS.addListener { _: ServerStoppedEvent -> RewardShopTrades.replace(emptyMap(), emptyMap()) }
        eventBus.addListener(this::commonSetup)
        eventBus.addListener(this::creativeTabContents)
        blocksRegistry.register(eventBus)
        itemsRegistry.register(eventBus)
        creativeTabRegistry.register(eventBus)
        menusRegistry.register(eventBus)
        blockEntitiesRegistry.register(eventBus)
    }

    @Suppress("UNUSED_PARAMETER")
    fun commonSetup(event: FMLCommonSetupEvent) {
        @Suppress("UNCHECKED_CAST")
        AutomaticRewardBoxMenus.type = automaticRewardBoxMenu.get() as MenuType<MerchantMenu>
    }

    fun creativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(debugManualItem)
            event.accept(debugAutomaticItem)
        }
    }
}

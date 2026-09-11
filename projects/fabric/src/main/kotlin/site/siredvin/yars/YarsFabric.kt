package site.siredvin.yars

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.BlockEntityType
import site.siredvin.broccolium.FabricBroccolium
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.common.block.AutomaticRewardBoxMenus
import site.siredvin.yars.common.block.DebugShops
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import site.siredvin.yars.fabric.FabricModRecipeIngredients
import site.siredvin.yars.fabric.YarsFabricPlatform
import site.siredvin.yars.fabric.registerAutomaticRewardBoxStorage

object YarsFabric : ModInitializer {
    override fun onInitialize() {
        FabricBroccolium.sayHi()
        YarsCore.configure(YarsFabricPlatform, FabricModRecipeIngredients)
        val manual = Registry.register(BuiltInRegistries.BLOCK, DebugShops.MANUAL_ID, DebugShops.manual())
        lateinit var entityType: BlockEntityType<AutomaticRewardBoxBlockEntity>
        val automatic = Registry.register(BuiltInRegistries.BLOCK, DebugShops.AUTOMATIC_ID, DebugShops.automatic { entityType })
        entityType = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            DebugShops.AUTOMATIC_ID,
            BlockEntityType.Builder.of({ pos, state -> AutomaticRewardBoxBlockEntity(entityType, pos, state, DebugShops.AUTOMATIC_ID) }, automatic).build(com.mojang.datafixers.DSL.remainderType()),
        )
        val manualItem = Registry.register(BuiltInRegistries.ITEM, DebugShops.MANUAL_ID, BlockItem(manual, Item.Properties()))
        val automaticItem = Registry.register(BuiltInRegistries.ITEM, DebugShops.AUTOMATIC_ID, BlockItem(automatic, Item.Properties()))
        registerAutomaticRewardBoxStorage(automatic)
        ServerLifecycleEvents.SERVER_STOPPED.register { RewardShopTrades.replace(emptyMap(), emptyMap()) }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register {
            it.accept(manualItem)
            it.accept(automaticItem)
        }
        AutomaticRewardBoxMenus.type = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation(YarsCore.MOD_ID, "automatic_reward_box"),
            MenuType<MerchantMenu>({ id, inventory -> AutomaticRewardBoxMenu(id, inventory) }, FeatureFlags.DEFAULT_FLAGS),
        )
    }
}

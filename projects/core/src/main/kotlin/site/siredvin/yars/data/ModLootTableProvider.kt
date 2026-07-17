package site.siredvin.yars.data

import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.LootTableSubProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import site.siredvin.broccolium.modules.data.loot.LootTableHelper
import site.siredvin.yars.common.setup.Blocks
import site.siredvin.yars.xplat.ModPlatform
import java.util.function.BiConsumer

object ModLootTableProvider {
    fun getTables(): List<LootTableProvider.SubProviderEntry> = listOf(
        LootTableProvider.SubProviderEntry({ LootTableSubProvider { registerBlocks(it) } }, LootContextParamSets.BLOCK),
    )

    fun registerBlocks(@Suppress("UNUSED_PARAMETER") consumer: BiConsumer<ResourceLocation, LootTable.Builder>) {
        LootTableHelper(ModPlatform.holder).apply {
            computedDrop(Blocks.REWARD_SHOP)
            validate()
        }
    }
}

package site.siredvin.yars.forge

import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.DeferredRegister
import site.siredvin.broccolium.modules.platform.ForgeInnerBasePlatform
import site.siredvin.yars.YarsCore
import site.siredvin.yars.YarsForge

object YarsForgePlatform : ForgeInnerBasePlatform() {
    override val modID: String
        get() = YarsCore.MOD_ID

    override val itemsRegistry: DeferredRegister<Item>
        get() = YarsForge.itemsRegistry

    override val blocksRegistry: DeferredRegister<Block>
        get() = YarsForge.blocksRegistry

    override val creativeTabRegistry: DeferredRegister<CreativeModeTab>
        get() = YarsForge.creativeTabRegistry
}

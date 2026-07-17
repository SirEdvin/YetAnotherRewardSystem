package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.block.BlockBuilder
import net.minecraft.resources.ResourceLocation
import site.siredvin.yars.common.block.RewardShopBlock

class RewardShopBlockBuilder(id: ResourceLocation) : BlockBuilder(id) {
    override fun createObject() = RewardShopBlock(createProperties(), id)
}

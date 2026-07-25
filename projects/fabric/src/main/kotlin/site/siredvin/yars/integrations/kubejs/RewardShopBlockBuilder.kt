package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.block.BlockBuilder
import dev.latvian.mods.kubejs.block.entity.BlockEntityInfo
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.RewardShopBlock
import site.siredvin.yars.fabric.registerAutomaticRewardBoxStorage

class RewardShopBlockBuilder(id: ResourceLocation) : BlockBuilder(id) {
    override fun createObject() = RewardShopBlock(createProperties(), id)
}

class AutomaticRewardBoxBlockBuilder(id: ResourceLocation) : BlockBuilder(id) {
    private val info = object : BlockEntityInfo(this) {
        override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AutomaticRewardBoxBlockEntity(entityType, pos, state, id)
    }

    init {
        blockEntityInfo = info
    }

    override fun createObject() = AutomaticRewardBoxBlock(createProperties(), id) { info.entityType }.also(::registerAutomaticRewardBoxStorage)
}

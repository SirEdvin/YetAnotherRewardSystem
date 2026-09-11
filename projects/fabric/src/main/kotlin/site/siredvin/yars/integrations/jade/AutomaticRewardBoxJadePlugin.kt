package site.siredvin.yars.integrations.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec2
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig

@WailaPlugin("yars")
class AutomaticRewardBoxJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(AutomaticRewardBoxJadeProvider, AutomaticRewardBoxBlockEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(AutomaticRewardBoxJadeProvider, AutomaticRewardBoxBlock::class.java)
    }
}

@Suppress("DEPRECATION")
private object AutomaticRewardBoxJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private val id = ResourceLocation("yars", "automatic_reward_box")
    private const val FIRST_COST = "YarsFirstCost"
    private const val SECOND_COST = "YarsSecondCost"
    private const val RESULT = "YarsResult"

    override fun getUid(): ResourceLocation = id

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
        val trade = (accessor.blockEntity as? AutomaticRewardBoxBlockEntity)?.currentTrade() ?: return
        data.put(FIRST_COST, trade.firstCost.save(CompoundTag()))
        trade.secondCost?.let { data.put(SECOND_COST, it.save(CompoundTag())) }
        data.put(RESULT, trade.result.save(CompoundTag()))
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (!accessor.serverData.contains(FIRST_COST) || !accessor.serverData.contains(RESULT)) return
        val elements = mutableListOf(tooltip.elementHelper.item(ItemStack.of(accessor.serverData.getCompound(FIRST_COST))))
        if (accessor.serverData.contains(SECOND_COST)) {
            elements += tooltip.elementHelper.text(Component.literal(" + ")).translate(Vec2(0f, 6f))
            elements += tooltip.elementHelper.item(ItemStack.of(accessor.serverData.getCompound(SECOND_COST)))
        }
        elements += tooltip.elementHelper.text(Component.literal(" -> ")).translate(Vec2(0f, 6f))
        elements += tooltip.elementHelper.item(ItemStack.of(accessor.serverData.getCompound(RESULT)))
        tooltip.add(elements)
    }
}

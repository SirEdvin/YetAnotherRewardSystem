package site.siredvin.yars.integrations.jade

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
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
    private const val SELECTED_TRADE = "YarsSelectedTrade"

    override fun getUid(): ResourceLocation = id

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
        (accessor.blockEntity as? AutomaticRewardBoxBlockEntity)?.selectedTradeId?.let { data.putString(SELECTED_TRADE, it) }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val selected = accessor.serverData.getString(SELECTED_TRADE).takeIf(String::isNotEmpty)?.let(Component::literal)
            ?: Component.translatable("jade.yars.no_trade")
        tooltip.add(Component.translatable("jade.yars.selected_trade", selected))
    }
}

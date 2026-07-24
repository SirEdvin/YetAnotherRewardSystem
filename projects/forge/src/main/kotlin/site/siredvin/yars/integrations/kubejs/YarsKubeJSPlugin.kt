package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.KubeJSPlugin
import dev.latvian.mods.kubejs.registry.RegistryInfo

class YarsKubeJSPlugin : KubeJSPlugin() {
    override fun init() {
        RegistryInfo.BLOCK.addType("yars:reward_shop", RewardShopBlockBuilder::class.java, ::RewardShopBlockBuilder)
        RegistryInfo.BLOCK.addType("yars:automatic_reward_box", AutomaticRewardBoxBlockBuilder::class.java, ::AutomaticRewardBoxBlockBuilder)
    }

    override fun registerEvents() {
        RewardShopKubeJSEvents.register()
    }

    override fun onServerReload() {
        RewardShopKubeJSEvents.postTrades()
    }
}

package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.KubeJSPlugin
import dev.latvian.mods.kubejs.registry.RegistryInfo

class YarsKubeJSPlugin : KubeJSPlugin() {
    override fun init() {
        RegistryInfo.BLOCK.addType("yars:reward_shop", RewardShopBlockBuilder::class.java, ::RewardShopBlockBuilder)
    }

    override fun registerEvents() {
        RewardShopKubeJSEvents.register()
    }

    override fun onServerReload() {
        RewardShopKubeJSEvents.postTrades()
    }
}

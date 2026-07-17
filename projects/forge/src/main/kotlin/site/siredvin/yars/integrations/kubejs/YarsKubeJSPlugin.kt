package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.KubeJSPlugin

class YarsKubeJSPlugin : KubeJSPlugin() {
    override fun registerEvents() {
        RewardShopKubeJSEvents.register()
    }

    override fun onServerReload() {
        RewardShopKubeJSEvents.postTrades()
    }
}

package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.event.EventGroup
import dev.latvian.mods.kubejs.event.EventHandler
import dev.latvian.mods.kubejs.event.EventJS
import dev.latvian.mods.kubejs.event.EventResult
import dev.latvian.mods.kubejs.script.ScriptType
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration
import site.siredvin.yars.common.rewardshop.RewardShopTradeShopRegistration
import java.util.function.Consumer

class RewardShopTradeEvent : EventJS() {
    private val registration = RewardShopTradeRegistration()

    fun shop(id: String, callback: Consumer<RewardShopTradeShopRegistration>) = registration.shop(id, callback)

    override fun afterPosted(result: EventResult) {
        if (!result.error()) registration.replaceTrades()
    }
}

object RewardShopKubeJSEvents {
    private val group = EventGroup.of("RewardShopEvents")
    private val trades: EventHandler = group.server("trades") { RewardShopTradeEvent::class.java }

    fun register() {
        group.register()
    }

    fun postTrades() {
        trades.post(ScriptType.SERVER, RewardShopTradeEvent())
    }
}

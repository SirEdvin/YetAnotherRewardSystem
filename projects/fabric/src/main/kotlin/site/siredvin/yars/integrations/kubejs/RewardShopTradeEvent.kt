package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.event.EventGroup
import dev.latvian.mods.kubejs.event.EventHandler
import dev.latvian.mods.kubejs.event.EventJS
import dev.latvian.mods.kubejs.script.ScriptType
import net.minecraft.resources.ResourceLocation
import site.siredvin.yars.common.rewardshop.DebugShopTrades
import site.siredvin.yars.common.rewardshop.RewardShopTradeBuilder
import site.siredvin.yars.common.rewardshop.RewardShopTrades

class RewardShopTradeEvent : EventJS() {
    private val registration = DebugShopTrades.registration()

    fun trade(id: String): RewardShopTradeBuilder = registration.trade(id)

    fun attach(boxId: String, tradeId: String) = registration.attach(boxId, tradeId)

    @dev.latvian.mods.rhino.util.HideFromJS
    fun publish() = registration.replaceTrades()
}

object RewardShopKubeJSEvents {
    private val group = EventGroup.of("RewardShopEvents")
    private val trades: EventHandler = group.server("trades") { RewardShopTradeEvent::class.java }

    fun register() {
        group.register()
    }

    fun postTrades() {
        if (RewardShopTrades.find(ResourceLocation("yars", "debug/shared/basic")) == null) DebugShopTrades.registration().replaceTrades()
        val event = RewardShopTradeEvent()
        var failed = false
        val errorsBefore = ScriptType.SERVER.console.errors.size
        // KubeJS can return PASS after logging an exception, and skips afterPosted without listeners.
        val result = trades.post(ScriptType.SERVER, event) { _, _, error ->
            failed = true
            error
        }
        if (!failed && !result.error() && ScriptType.SERVER.console.errors.size == errorsBefore) {
            try {
                event.publish()
            } catch (error: IllegalArgumentException) {
                ScriptType.SERVER.console.error("Invalid reward shop catalog; keeping previous trades", error)
            }
        }
    }
}

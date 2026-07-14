package site.siredvin.yars.integrations.kubejs

import dev.latvian.mods.kubejs.event.EventGroup
import dev.latvian.mods.kubejs.event.EventHandler
import dev.latvian.mods.kubejs.event.EventJS
import dev.latvian.mods.kubejs.event.EventResult
import dev.latvian.mods.kubejs.script.ScriptType
import net.minecraft.world.item.ItemStack
import site.siredvin.yars.common.rewardshop.RewardShopTradeBuilder
import site.siredvin.yars.common.rewardshop.RewardShopTradeRegistration

class RewardShopTradeEvent : EventJS() {
    private val registration = RewardShopTradeRegistration()

    fun trade(id: String, result: ItemStack, firstCost: ItemStack, secondCost: ItemStack? = null): RewardShopTradeBuilder = registration.trade(id, result, firstCost, secondCost)

    fun progression(id: String): RewardShopTradeBuilder = registration.progression(id)

    override fun afterPosted(result: EventResult) {
        registration.replaceTrades()
    }
}

object RewardShopKubeJSEvents {
    private val trades: EventHandler = EventGroup.of("RewardShopEvents").server("trades") { RewardShopTradeEvent::class.java }

    fun postTrades() {
        trades.post(ScriptType.SERVER, RewardShopTradeEvent())
    }
}

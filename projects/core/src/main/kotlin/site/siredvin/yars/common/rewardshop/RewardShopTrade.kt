package site.siredvin.yars.common.rewardshop

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import site.siredvin.yars.common.block.RewardShopTarget

fun interface RewardShopStackResolver {
    fun resolve(purchaseIndex: Int): ItemStack
}

data class RewardShopCost(
    val first: RewardShopStackResolver,
    val second: RewardShopStackResolver? = null,
)

data class RewardShopTradeStage(
    val purchases: Int?,
    val result: RewardShopStackResolver,
    val cost: RewardShopCost,
)

data class ResolvedRewardShopTrade(
    val id: ResourceLocation,
    val result: ItemStack,
    val firstCost: ItemStack,
    val secondCost: ItemStack?,
)

class RewardShopTrade(
    val id: ResourceLocation,
    val stages: List<RewardShopTradeStage>,
) {
    init {
        require(stages.isNotEmpty()) { "Reward shop trade $id needs at least one stage" }
        require(stages.dropLast(1).none { it.purchases == null }) { "Only the final stage of $id may be unlimited" }
        require(stages.all { it.purchases == null || it.purchases > 0 }) { "Trade stages must have positive purchase counts" }
        var purchaseIndex = 0
        stages.forEach { stage ->
            validateStage(stage, purchaseIndex)
            purchaseIndex = Math.addExact(purchaseIndex, stage.purchases ?: 0)
        }
    }

    fun resolve(completedPurchases: Int): ResolvedRewardShopTrade? {
        val purchaseIndex = completedPurchases.coerceAtLeast(0)
        var stageIndex = purchaseIndex
        for (stage in stages) {
            val purchases = stage.purchases
            if (purchases == null || stageIndex < purchases) {
                val result = stage.result.resolve(purchaseIndex).copy()
                val firstCost = stage.cost.first.resolve(purchaseIndex).copy()
                val secondCost = stage.cost.second?.resolve(purchaseIndex)?.copy()
                validateStack(result, "result")
                validateStack(firstCost, "first cost")
                secondCost?.let { validateStack(it, "second cost") }
                return ResolvedRewardShopTrade(id, result, firstCost, secondCost)
            }
            stageIndex -= purchases
        }
        return null
    }

    private fun validateStage(stage: RewardShopTradeStage, purchaseIndex: Int) {
        validateStack(stage.result.resolve(purchaseIndex), "result")
        validateStack(stage.cost.first.resolve(purchaseIndex), "first cost")
        stage.cost.second?.resolve(purchaseIndex)?.let { validateStack(it, "second cost") }
    }

    private fun validateStack(stack: ItemStack, name: String) {
        require(!stack.isEmpty && stack.count in 1..stack.maxStackSize) {
            "Reward shop trade $id has an invalid $name stack"
        }
    }
}

object RewardShopTrades {
    fun interface Listener {
        fun onRewardShopTradesReplaced()
    }

    private data class Snapshot(
        val trades: Map<ResourceLocation, RewardShopTrade>,
        val attachments: Map<ResourceLocation, List<ResourceLocation>>,
    )

    @Volatile
    private var snapshot = Snapshot(emptyMap(), emptyMap())
    private val listeners = java.util.Collections.newSetFromMap(java.util.WeakHashMap<Listener, Boolean>())

    @Synchronized
    fun replace(newTrades: Map<ResourceLocation, RewardShopTrade>, newAttachments: Map<ResourceLocation, List<ResourceLocation>>) {
        snapshot = Snapshot(newTrades.toMap(), newAttachments.mapValues { it.value.toList() })
        synchronized(listeners) { listeners.toList() }.forEach(Listener::onRewardShopTradesReplaced)
    }

    fun addListener(listener: Listener) = synchronized(listeners) { listeners += listener }
    fun removeListener(listener: Listener) = synchronized(listeners) { listeners -= listener }

    fun all(boxId: ResourceLocation): List<RewardShopTrade> {
        val current = snapshot
        return current.attachments[boxId]?.mapNotNull(current.trades::get) ?: emptyList()
    }

    fun find(id: ResourceLocation): RewardShopTrade? = snapshot.trades[id]

    fun find(boxId: ResourceLocation, id: ResourceLocation): RewardShopTrade? {
        val current = snapshot
        return current.trades[id]?.takeIf { id in (current.attachments[boxId] ?: emptyList()) }
    }
}

class RewardShopTradeBuilder internal constructor(
    private val id: ResourceLocation,
) {
    private val stages = mutableListOf<RewardShopTradeStage>()

    @JvmOverloads
    fun simple(purchases: Number, firstCost: ItemStack, result: ItemStack, secondCost: ItemStack? = null): RewardShopTradeBuilder {
        validateFixedStack(firstCost, "first cost")
        validateFixedStack(result, "result")
        secondCost?.let { validateFixedStack(it, "second cost") }
        return stage(purchases.toStageLimit(), { result.copy() }, { firstCost.copy() }, secondCost?.let { stack -> { stack.copy() } })
    }

    @JvmOverloads
    fun dynamic(
        purchases: Number,
        firstCost: RewardShopStackResolver,
        result: RewardShopStackResolver,
        secondCost: RewardShopStackResolver? = null,
    ): RewardShopTradeBuilder = stage(
        purchases.toStageLimit(),
        result,
        firstCost,
        secondCost,
    )

    fun build(): RewardShopTrade = RewardShopTrade(id, stages.toList())

    private fun stage(
        purchases: Int?,
        result: RewardShopStackResolver,
        firstCost: RewardShopStackResolver,
        secondCost: RewardShopStackResolver?,
    ): RewardShopTradeBuilder {
        require(stages.none { it.purchases == null }) { "Unlimited stages must be final" }
        stages += RewardShopTradeStage(purchases, result, RewardShopCost(firstCost, secondCost))
        return this
    }

    private fun Number.toStageLimit(): Int? {
        val value = toLong()
        require(toDouble().isFinite() && toDouble() == value.toDouble() && value in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Trade stage limits must be whole numbers in the integer range"
        }
        return when (value.toInt()) {
            -1 -> null
            in 1..Int.MAX_VALUE -> value.toInt()
            else -> throw IllegalArgumentException("Trade stages must have a positive limit or -1 for unlimited")
        }
    }

    private fun validateFixedStack(stack: ItemStack, name: String) {
        require(!stack.isEmpty && stack.count in 1..stack.maxStackSize) { "Reward shop trade $id has an invalid $name stack" }
    }
}

class RewardShopTradeRegistration {
    private val builders = mutableListOf<RewardShopTradeBuilder>()
    private val attachments = mutableListOf<Pair<ResourceLocation, ResourceLocation>>()

    fun trade(id: String): RewardShopTradeBuilder = RewardShopTradeBuilder(parse(id, "trade")).also { builders += it }

    fun attach(boxId: String, tradeId: String) {
        attachments += parse(boxId, "reward box") to parse(tradeId, "trade")
    }

    @Suppress("DEPRECATION")
    fun replaceTrades(
        isRewardShop: (ResourceLocation) -> Boolean = {
            BuiltInRegistries.BLOCK.containsKey(it) && BuiltInRegistries.BLOCK.get(it) is RewardShopTarget
        },
    ) {
        val built = linkedMapOf<ResourceLocation, RewardShopTrade>()
        builders.forEach { builder ->
            val trade = builder.build()
            require(built.putIfAbsent(trade.id, trade) == null) { "Duplicate reward shop trade: ${trade.id}" }
        }
        val attached = linkedMapOf<ResourceLocation, MutableList<ResourceLocation>>()
        attachments.forEach { (boxId, tradeId) ->
            require(isRewardShop(boxId)) { "Reward shop target is missing or is not a reward shop block: $boxId" }
            require(tradeId in built) { "Unknown reward shop trade: $tradeId" }
            val boxTrades = attached.getOrPut(boxId) { mutableListOf() }
            require(tradeId !in boxTrades) { "Duplicate reward shop attachment: $boxId / $tradeId" }
            boxTrades += tradeId
        }
        RewardShopTrades.replace(built, attached)
    }

    private fun parse(id: String, type: String): ResourceLocation = ResourceLocation.tryParse(id)?.takeIf { ':' in id } ?: throw IllegalArgumentException("Invalid $type ID: $id")
}

package site.siredvin.yars.common.rewardshop

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import site.siredvin.yars.common.block.RewardShopTarget
import java.util.function.Consumer

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
    val id: String,
    val result: ItemStack,
    val firstCost: ItemStack,
    val secondCost: ItemStack?,
)

class RewardShopTrade(
    val id: String,
    val stages: List<RewardShopTradeStage>,
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+"))) { "Reward shop trade IDs must be lowercase paths: $id" }
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

    @Volatile
    private var trades: Map<ResourceLocation, Map<String, RewardShopTrade>> = emptyMap()
    private val listeners = java.util.Collections.newSetFromMap(java.util.WeakHashMap<Listener, Boolean>())

    @Synchronized
    fun replace(newTrades: Map<ResourceLocation, List<RewardShopTrade>>) {
        trades = newTrades.mapValues { (_, shopTrades) -> shopTrades.associateBy { it.id } }
        synchronized(listeners) { listeners.toList() }.forEach(Listener::onRewardShopTradesReplaced)
    }

    fun addListener(listener: Listener) = synchronized(listeners) { listeners += listener }
    fun removeListener(listener: Listener) = synchronized(listeners) { listeners -= listener }

    fun all(shopId: ResourceLocation): Collection<RewardShopTrade> = trades[shopId]?.values ?: emptyList()

    fun find(shopId: ResourceLocation, id: String): RewardShopTrade? = trades[shopId]?.get(id)
}

class RewardShopTradeBuilder internal constructor(
    private val id: String,
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
    private val shops = mutableListOf<Pair<ResourceLocation, MutableList<RewardShopTradeBuilder>>>()

    fun shop(id: String, callback: Consumer<RewardShopTradeShopRegistration>) {
        val shopId = ResourceLocation.tryParse(id) ?: throw IllegalArgumentException("Invalid reward shop ID: $id")
        val builders = mutableListOf<RewardShopTradeBuilder>()
        shops += shopId to builders
        callback.accept(RewardShopTradeShopRegistration(builders))
    }

    @Suppress("DEPRECATION")
    fun replaceTrades(
        isRewardShop: (ResourceLocation) -> Boolean = {
            BuiltInRegistries.BLOCK.containsKey(it) && BuiltInRegistries.BLOCK.get(it) is RewardShopTarget
        },
    ) {
        val built = linkedMapOf<ResourceLocation, MutableList<RewardShopTrade>>()
        shops.forEach { (shopId, builders) ->
            require(isRewardShop(shopId)) { "Reward shop target is missing or is not a reward shop block: $shopId" }
            val trades = built.getOrPut(shopId) { mutableListOf() }
            builders.forEach { builder ->
                val trade = builder.build()
                require(trades.none { it.id == trade.id }) { "Duplicate reward shop trade: $shopId / ${trade.id}" }
                trades += trade
            }
        }
        RewardShopTrades.replace(built)
    }
}

class RewardShopTradeShopRegistration internal constructor(
    private val builders: MutableList<RewardShopTradeBuilder>,
) {
    fun trade(id: String): RewardShopTradeBuilder = RewardShopTradeBuilder(id).also { builders += it }
}

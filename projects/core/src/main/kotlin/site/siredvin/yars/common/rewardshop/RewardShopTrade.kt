package site.siredvin.yars.common.rewardshop

import net.minecraft.world.item.ItemStack

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
    private var trades: Map<String, RewardShopTrade> = emptyMap()

    @Synchronized
    fun replace(newTrades: Collection<RewardShopTrade>) {
        val duplicate = newTrades.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        require(duplicate == null) { "Duplicate reward shop trade ID: $duplicate" }
        val byId = newTrades.associateBy { it.id }
        trades = byId
    }

    fun all(): Collection<RewardShopTrade> = trades.values

    fun find(id: String): RewardShopTrade? = trades[id]
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
    private val builders = mutableListOf<RewardShopTradeBuilder>()

    fun trade(id: String): RewardShopTradeBuilder = RewardShopTradeBuilder(id).also { builders += it }

    fun replaceTrades() {
        RewardShopTrades.replace(builders.map { it.build() })
    }
}

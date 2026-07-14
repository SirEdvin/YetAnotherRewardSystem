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
    }

    fun resolve(completedPurchases: Int): ResolvedRewardShopTrade? {
        var index = completedPurchases.coerceAtLeast(0)
        for (stage in stages) {
            val purchases = stage.purchases
            if (purchases == null || index < purchases) {
                val result = stage.result.resolve(index).copy()
                val firstCost = stage.cost.first.resolve(index).copy()
                val secondCost = stage.cost.second?.resolve(index)?.copy()
                validateStack(result, "result")
                validateStack(firstCost, "first cost")
                secondCost?.let { validateStack(it, "second cost") }
                return ResolvedRewardShopTrade(id, result, firstCost, secondCost)
            }
            index -= purchases
        }
        return null
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
        val byId = newTrades.associateBy { it.id }
        require(byId.size == newTrades.size) { "Reward shop trade IDs must be unique" }
        trades = byId
    }

    fun all(): Collection<RewardShopTrade> = trades.values
}

class RewardShopTradeBuilder internal constructor(
    private val id: String,
) {
    private val stages = mutableListOf<RewardShopTradeStage>()

    @JvmOverloads
    fun simple(purchases: Number, result: ItemStack, firstCost: ItemStack, secondCost: ItemStack? = null): RewardShopTradeBuilder = stage(purchases.toInt().toStageLimit(), { result.copy() }, { firstCost.copy() }, secondCost?.let { stack -> { stack.copy() } })

    @JvmOverloads
    fun dynamic(
        purchases: Number,
        result: RewardShopStackResolver,
        firstCost: RewardShopStackResolver,
        secondCost: RewardShopStackResolver? = null,
    ): RewardShopTradeBuilder = stage(
        purchases.toInt().toStageLimit(),
        { index -> result.resolve(index + 1) },
        { index -> firstCost.resolve(index + 1) },
        secondCost?.let { resolver -> { index -> resolver.resolve(index + 1) } },
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

    private fun Int.toStageLimit(): Int? = when {
        this == -1 -> null
        this > 0 -> this
        else -> throw IllegalArgumentException("Trade stages must have a positive limit or -1 for unlimited")
    }
}

class RewardShopTradeRegistration {
    private val builders = mutableListOf<RewardShopTradeBuilder>()

    fun trade(id: String): RewardShopTradeBuilder = RewardShopTradeBuilder(id).also { builders += it }

    fun replaceTrades() {
        RewardShopTrades.replace(builders.map { it.build() })
    }
}

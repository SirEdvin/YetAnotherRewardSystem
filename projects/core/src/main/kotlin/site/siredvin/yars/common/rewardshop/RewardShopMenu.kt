package site.siredvin.yars.common.rewardshop

import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object RewardShopMenus {
    lateinit var type: MenuType<RewardShopMenu>
        private set

    fun configure(type: MenuType<RewardShopMenu>) {
        this.type = type
    }
}

class RewardShopMenu(
    containerId: Int,
    private val inventory: Inventory,
) : AbstractContainerMenu(RewardShopMenus.type, containerId) {
    private val offer = SimpleContainer(3)
    private var selectedTrade = 0
    private var availableTradeCount = 0

    val selected = DataSlot.standalone()
    val available = DataSlot.standalone()

    init {
        addSlot(readOnlySlot(0, 136, 37))
        addSlot(readOnlySlot(1, 162, 37))
        addSlot(readOnlySlot(2, 220, 37))
        addDataSlot(selected)
        addDataSlot(available)

        for (row in 0 until 3) {
            for (column in 0 until 9) addSlot(Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18))
        }
        for (column in 0 until 9) addSlot(Slot(inventory, column, 8 + column * 18, 142))

        if (!inventory.player.level().isClientSide) refreshOffer()
    }

    override fun stillValid(player: Player): Boolean = true

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player.level().isClientSide) return false
        when (id) {
            0 -> purchase(player)
            1 -> select(selectedTrade - 1)
            2 -> select(selectedTrade + 1)
            else -> return false
        }
        return true
    }

    private fun purchase(player: Player) {
        val trade = availableTrades().getOrNull(selectedTrade) ?: return refreshOffer()
        val resolved = trade.resolve(RewardShopTradeHistory.completed(player, trade.id)) ?: return refreshOffer()
        val costs = listOfNotNull(resolved.firstCost, resolved.secondCost)
        if (!costs.all { cost -> hasCost(cost, costs) }) return

        costs.forEach(::removeCost)
        if (!inventory.add(resolved.result.copy())) player.drop(resolved.result.copy(), false)
        RewardShopTradeHistory.increment(player, trade.id)
        refreshOffer()
    }

    private fun hasCost(cost: ItemStack, allCosts: List<ItemStack>): Boolean = inventory.items.asSequence()
        .filter { ItemStack.isSameItemSameTags(it, cost) }
        .sumOf(ItemStack::getCount) >= allCosts.filter { ItemStack.isSameItemSameTags(it, cost) }.sumOf(ItemStack::getCount)

    private fun removeCost(cost: ItemStack) {
        var remaining = cost.count
        for (stack in inventory.items) {
            if (!ItemStack.isSameItemSameTags(stack, cost)) continue
            val removed = minOf(remaining, stack.count)
            stack.shrink(removed)
            remaining -= removed
            if (remaining == 0) return
        }
    }

    private fun select(index: Int) {
        selectedTrade = index.coerceIn(0, (availableTrades().size - 1).coerceAtLeast(0))
        refreshOffer()
    }

    private fun refreshOffer() {
        val trades = availableTrades()
        selectedTrade = selectedTrade.coerceIn(0, (trades.size - 1).coerceAtLeast(0))
        val resolved = trades.getOrNull(selectedTrade)?.resolve(RewardShopTradeHistory.completed(inventory.player, trades[selectedTrade].id))
        offer.setItem(0, resolved?.firstCost?.copy() ?: ItemStack.EMPTY)
        offer.setItem(1, resolved?.secondCost?.copy() ?: ItemStack.EMPTY)
        offer.setItem(2, resolved?.result?.copy() ?: ItemStack.EMPTY)
        availableTradeCount = trades.size
        selected.set(selectedTrade)
        available.set(availableTradeCount)
        broadcastChanges()
    }

    private fun availableTrades(): List<RewardShopTrade> = RewardShopTrades.all()
        .filter { it.resolve(RewardShopTradeHistory.completed(inventory.player, it.id)) != null }

    private fun readOnlySlot(index: Int, x: Int, y: Int): Slot = object : Slot(offer, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false

        override fun mayPickup(player: Player): Boolean = false
    }
}

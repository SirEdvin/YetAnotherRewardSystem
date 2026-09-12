package site.siredvin.yars.common.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import site.siredvin.broccolium.modules.base.api.IOwnedBlockEntity
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.rewardshop.ResolvedRewardShopTrade
import site.siredvin.yars.common.rewardshop.RewardShopTradeHistory
import site.siredvin.yars.common.rewardshop.RewardShopTrades
import java.util.UUID

class AutomaticRewardBoxBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    val shopId: ResourceLocation,
) : BaseContainerBlockEntity(type, pos, state),
    WorldlyContainer,
    RewardShopTrades.Listener,
    RewardShopTradeHistory.Listener,
    IOwnedBlockEntity {
    companion object {
        const val PAYMENT_SLOTS = 2
        const val SIZE = 6
        private const val ITEMS_KEY = "Items"
        private const val SELECTED_TRADE_KEY = "SelectedTrade"
        private const val OWNER_KEY = "Owner"
        private const val DISPLAY_ENABLED_KEY = "ShowTradeDisplay"
        private const val DISPLAY_UPDATE_KEY = "TradeDisplay"

        private fun sameItem(left: ItemStack, right: ItemStack): Boolean = ItemStack.isSameItemSameTags(left, right)
    }

    private var items = NonNullList.withSize(SIZE, ItemStack.EMPTY)
    private var processing = false
    private var observedHistory: RewardShopTradeHistory? = null
    private var refreshingDisplay = false
    var showTradeDisplay = true
        private set
    private var displayStacks: List<ItemStack> = emptyList()
    val displayedItems: List<ItemStack> get() = displayStacks.map(ItemStack::copy)
    override var ownerPlayerUUID: UUID? = null
    override var player: Player?
        get() = ownerPlayerUUID?.let { level?.getPlayerByUUID(it) }
        set(value) {
            ownerPlayerUUID = value?.uuid
        }
    var selectedTradeId: ResourceLocation? = null
        private set

    init {
        // ponytail: weak listeners provide reload callbacks without a tick loop or world scan.
        RewardShopTrades.addListener(this)
    }

    override fun getContainerSize(): Int = SIZE
    override fun isEmpty(): Boolean = items.all(ItemStack::isEmpty)
    override fun getItem(slot: Int): ItemStack = items[slot]
    override fun removeItem(slot: Int, amount: Int): ItemStack = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount).also { if (!it.isEmpty) setChanged() }
    override fun removeItemNoUpdate(slot: Int): ItemStack = net.minecraft.world.ContainerHelper.takeItem(items, slot)
    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxStackSize) stack.count = maxStackSize
        setChanged()
    }

    fun setItemForTransaction(slot: Int, stack: ItemStack) {
        items[slot] = stack
    }
    override fun stillValid(player: Player): Boolean = canOpen(player)
    override fun clearContent() {
        items.clear()
        setChanged()
    }

    override fun setChanged() {
        super.setChanged()
        if (!processing) processTrades()
    }

    override fun getDefaultName(): Component = blockState.block.name
    override fun createMenu(id: Int, inventory: Inventory): AbstractContainerMenu = AutomaticRewardBoxMenu(id, inventory, AutomaticRewardBoxMerchant(inventory.player, this), this)
    override fun canOpen(player: Player): Boolean = ownerPlayerUUID == player.uuid &&
        !isRemoved &&
        player.distanceToSqr(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5) <= 64.0
    override fun getSlotsForFace(side: Direction): IntArray = IntArray(SIZE) { it }
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = slot < PAYMENT_SLOTS && acceptsPayment(stack)
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, direction: Direction?): Boolean = canPlaceItem(slot, stack)
    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, direction: Direction): Boolean = slot >= PAYMENT_SLOTS

    override fun load(tag: CompoundTag) {
        // Both loaders deliver vanilla block-entity updates through load; these tags are not disk saves.
        if (tag.contains(DISPLAY_UPDATE_KEY)) {
            val display = tag.getCompound(DISPLAY_UPDATE_KEY)
            showTradeDisplay = display.getBoolean(DISPLAY_ENABLED_KEY)
            displayStacks = if (showTradeDisplay) {
                listOf("FirstCost", "SecondCost", "Result").map { ItemStack.of(display.getCompound(it)) }.filterNot(ItemStack::isEmpty)
            } else {
                emptyList()
            }
            return
        }
        super.load(tag)
        showTradeDisplay = !tag.contains(DISPLAY_ENABLED_KEY) || tag.getBoolean(DISPLAY_ENABLED_KEY)
        displayStacks = emptyList()
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY)
        net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompound(ITEMS_KEY), items)
        selectedTradeId = ResourceLocation.tryParse(tag.getString(SELECTED_TRADE_KEY))
        ownerPlayerUUID = if (tag.hasUUID(OWNER_KEY)) tag.getUUID(OWNER_KEY) else null
        if (level?.isClientSide == false) refreshDisplay()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putBoolean(DISPLAY_ENABLED_KEY, showTradeDisplay)
        tag.put(ITEMS_KEY, CompoundTag().also { net.minecraft.world.ContainerHelper.saveAllItems(it, items) })
        selectedTradeId?.let { tag.putString(SELECTED_TRADE_KEY, it.toString()) }
        ownerPlayerUUID?.let { tag.putUUID(OWNER_KEY, it) }
    }

    override fun setLevel(level: Level) {
        observedHistory?.removeListener(this)
        super.setLevel(level)
        observeHistory()
        processTrades()
    }

    override fun clearRemoved() {
        super.clearRemoved()
        RewardShopTrades.addListener(this)
        observeHistory()
        refreshDisplay()
    }

    private fun observeHistory() {
        observedHistory = history()
        observedHistory?.addListener(this)
    }

    override fun setRemoved() {
        observedHistory?.removeListener(this)
        observedHistory = null
        RewardShopTrades.removeListener(this)
        super.setRemoved()
    }

    override fun onRewardShopTradesReplaced() = processTrades()

    override fun onRewardShopTradeCompleted(playerId: UUID, tradeId: ResourceLocation) {
        if (!isRemoved && !processing && playerId == ownerPlayerUUID && tradeId == selectedTradeId) refreshDisplay()
    }

    fun setTradeDisplayEnabled(player: Player, enabled: Boolean): Boolean {
        if (level?.isClientSide != false || !canOpen(player)) return false
        setTradeDisplayEnabled(enabled)
        return true
    }

    internal fun setTradeDisplayEnabled(enabled: Boolean) {
        if (showTradeDisplay == enabled) return
        showTradeDisplay = enabled
        // A cosmetic setting must not trigger the setChanged override's transaction loop.
        super.setChanged()
        refreshDisplay(forceUpdate = true)
    }

    internal fun refreshDisplay(history: RewardShopTradeHistory? = history(), forceUpdate: Boolean = false): Boolean {
        if (level?.isClientSide == true || isRemoved || refreshingDisplay) return false
        refreshingDisplay = true
        try {
            val trade = if (showTradeDisplay && history != null) currentTrade(history) else null
            val next = trade?.let { listOfNotNull(it.firstCost, it.secondCost, it.result).map(ItemStack::copy) } ?: emptyList()
            if (!forceUpdate && next.size == displayStacks.size && next.indices.all { ItemStack.matches(next[it], displayStacks[it]) }) return false
            displayStacks = next
            level?.sendBlockUpdated(worldPosition, blockState, blockState, 2)
            return true
        } finally {
            refreshingDisplay = false
        }
    }

    override fun getUpdateTag(): CompoundTag {
        val display = CompoundTag()
        display.putBoolean(DISPLAY_ENABLED_KEY, showTradeDisplay)
        if (displayStacks.isNotEmpty()) {
            display.put("FirstCost", displayStacks.first().save(CompoundTag()))
            if (displayStacks.size == 3) display.put("SecondCost", displayStacks[1].save(CompoundTag()))
            display.put("Result", displayStacks.last().save(CompoundTag()))
        }
        return CompoundTag().apply { put(DISPLAY_UPDATE_KEY, display) }
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    fun selectTrade(player: Player, tradeId: ResourceLocation): Boolean {
        if (!canOpen(player)) return false
        val history = history() ?: return false
        return selectTrade(player.uuid, tradeId, history)
    }

    internal fun selectTrade(playerId: UUID, tradeId: ResourceLocation, history: RewardShopTradeHistory): Boolean {
        if (ownerPlayerUUID != playerId) return false
        val trade = RewardShopTrades.find(shopId, tradeId) ?: return false
        if (runCatching { trade.resolve(history.completed(playerId, tradeId)) }.getOrNull() == null) return false
        selectedTradeId = tradeId
        setChanged()
        refreshDisplay(history)
        return true
    }

    fun currentTrade(): ResolvedRewardShopTrade? {
        val history = history() ?: return null
        return currentTrade(history)
    }

    internal fun currentTrade(history: RewardShopTradeHistory): ResolvedRewardShopTrade? {
        val ownerId = ownerPlayerUUID ?: return null
        val id = selectedTradeId ?: return null
        return runCatching { RewardShopTrades.find(shopId, id)?.resolve(history.completed(ownerId, id)) }.getOrNull()
    }

    private fun acceptsPayment(stack: ItemStack): Boolean = !stack.isEmpty &&
        currentTrade()?.let { sameItem(stack, it.firstCost) || it.secondCost?.let { cost -> sameItem(stack, cost) } == true } == true

    fun extractOutput(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        if (slot !in PAYMENT_SLOTS until SIZE || amount <= 0) return ItemStack.EMPTY
        val stored = items[slot]
        if (stored.isEmpty) return ItemStack.EMPTY
        val extracted = stored.copyWithCount(minOf(amount, stored.count))
        if (!simulate) {
            stored.shrink(extracted.count)
            if (stored.isEmpty) items[slot] = ItemStack.EMPTY
            setChanged()
        }
        return extracted
    }

    internal fun processTrades(serverSide: Boolean = level?.isClientSide == false) {
        val history = history() ?: return
        processTrades(history, serverSide)
    }

    internal fun processTrades(history: RewardShopTradeHistory, serverSide: Boolean = true) {
        if (processing || !serverSide) return
        processing = true
        try {
            while (performTrade(history)) Unit
        } finally {
            processing = false
            refreshDisplay(history)
        }
    }

    private fun performTrade(history: RewardShopTradeHistory): Boolean {
        val ownerId = ownerPlayerUUID ?: return false
        val resolved = currentTrade(history) ?: return false
        if (!hasCosts(resolved) || !canFitOutput(resolved.result)) return false
        consume(resolved.firstCost)
        resolved.secondCost?.let(::consume)
        insertOutput(resolved.result)
        history.increment(ownerId, resolved.id)
        setChanged()
        return true
    }

    private fun history(): RewardShopTradeHistory? = (level as? ServerLevel)?.let(RewardShopTradeHistory::get)

    private fun hasCosts(trade: ResolvedRewardShopTrade): Boolean {
        val costs = listOfNotNull(trade.firstCost, trade.secondCost)
        return costs.all { cost ->
            val required = costs.filter { sameItem(it, cost) }.sumOf(ItemStack::getCount)
            items.take(PAYMENT_SLOTS).filter { sameItem(it, cost) }.sumOf(ItemStack::getCount) >= required
        }
    }

    private fun consume(cost: ItemStack) {
        var remaining = cost.count
        for (slot in 0 until PAYMENT_SLOTS) {
            val stored = items[slot]
            if (!sameItem(stored, cost)) continue
            val consumed = minOf(remaining, stored.count)
            stored.shrink(consumed)
            remaining -= consumed
            if (stored.isEmpty) items[slot] = ItemStack.EMPTY
            if (remaining == 0) return
        }
    }

    private fun canFitOutput(result: ItemStack): Boolean {
        var remaining = result.count
        for (slot in PAYMENT_SLOTS until SIZE) {
            val stored = items[slot]
            if (stored.isEmpty) {
                remaining -= minOf(remaining, result.maxStackSize)
            } else if (sameItem(stored, result)) {
                remaining -= minOf(remaining, stored.maxStackSize - stored.count)
            }
            if (remaining <= 0) return true
        }
        return false
    }

    private fun insertOutput(result: ItemStack) {
        var remaining = result.count
        for (slot in PAYMENT_SLOTS until SIZE) {
            val stored = items[slot]
            if (!stored.isEmpty && !sameItem(stored, result)) continue
            val moved = minOf(remaining, result.maxStackSize - stored.count)
            if (moved <= 0) continue
            if (stored.isEmpty) items[slot] = result.copyWithCount(moved) else stored.grow(moved)
            remaining -= moved
            if (remaining == 0) return
        }
        YarsCore.LOGGER.error("Automatic reward box output changed after capacity check at $worldPosition")
    }
}

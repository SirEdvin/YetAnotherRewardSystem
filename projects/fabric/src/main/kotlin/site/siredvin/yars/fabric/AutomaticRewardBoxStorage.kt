package site.siredvin.yars.fabric

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

private val storages = Collections.synchronizedMap(WeakHashMap<AutomaticRewardBoxBlockEntity, Storage<ItemVariant>>())
private val provider = BlockApiLookup.BlockApiProvider<Storage<ItemVariant>, Direction?> { _, _, _, entity, _ ->
    (entity as? AutomaticRewardBoxBlockEntity)?.let(::storageFor)
}

fun registerAutomaticRewardBoxStorage(block: AutomaticRewardBoxBlock) {
    ItemStorage.SIDED.registerForBlocks(provider, block)
}

private fun storageFor(box: AutomaticRewardBoxBlockEntity): Storage<ItemVariant> = synchronized(storages) {
    storages.getOrPut(box) {
        val reference = WeakReference(box)
        val dirty = DirtyParticipant(reference)
        CombinedStorage(List(AutomaticRewardBoxBlockEntity.SIZE) { BoxSlot(reference, dirty, it) })
    }
}

private class DirtyParticipant(
    private val box: WeakReference<AutomaticRewardBoxBlockEntity>,
) : SnapshotParticipant<Unit>() {
    override fun createSnapshot() = Unit
    override fun readSnapshot(snapshot: Unit) = Unit
    override fun onFinalCommit() {
        box.get()?.setChanged()
    }
}

private class BoxSlot(
    private val box: WeakReference<AutomaticRewardBoxBlockEntity>,
    private val dirty: DirtyParticipant,
    private val slot: Int,
) : SingleStackStorage() {
    private fun box() = checkNotNull(box.get()) { "Automatic reward box was removed" }

    override fun getStack(): ItemStack = box().getItem(slot)
    override fun setStack(stack: ItemStack) = box().setItemForTransaction(slot, stack)
    override fun canInsert(variant: ItemVariant): Boolean = slot < AutomaticRewardBoxBlockEntity.PAYMENT_SLOTS && box().canPlaceItem(slot, variant.toStack())
    override fun canExtract(variant: ItemVariant): Boolean = slot >= AutomaticRewardBoxBlockEntity.PAYMENT_SLOTS
    override fun getCapacity(variant: ItemVariant): Int = minOf(box().maxStackSize, variant.item.maxStackSize)

    override fun updateSnapshots(transaction: TransactionContext) {
        dirty.updateSnapshots(transaction)
        super.updateSnapshots(transaction)
    }
}

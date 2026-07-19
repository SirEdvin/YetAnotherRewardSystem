package site.siredvin.yars.common.block

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import site.siredvin.yars.mixins.SlotAccessor

object AutomaticRewardBoxMenus {
    lateinit var type: MenuType<AutomaticRewardBoxMenu>
}

class AutomaticRewardBoxMenu : MerchantMenu {
    constructor(id: Int, inventory: Inventory) : super(id, inventory) {
        addStorage(SimpleContainer(AutomaticRewardBoxBlockEntity.SIZE))
    }

    constructor(id: Int, inventory: Inventory, merchant: AutomaticRewardBoxMerchant, storage: AutomaticRewardBoxBlockEntity) : super(id, inventory, merchant) {
        addStorage(storage)
    }

    private fun addStorage(storage: Container) {
        slots.take(3).forEach {
            (it as SlotAccessor).`yars$setX`(-100)
            (it as SlotAccessor).`yars$setY`(-100)
        }
        repeat(AutomaticRewardBoxBlockEntity.SIZE) { index ->
            addSlot(
                object : Slot(storage, index, 136 + index % 3 * 18, 19 + index / 3 * 18) {
                    override fun mayPlace(stack: ItemStack): Boolean = index < AutomaticRewardBoxBlockEntity.PAYMENT_SLOTS && container.canPlaceItem(index, stack)
                },
            )
        }
    }

    override fun getType(): MenuType<*> = AutomaticRewardBoxMenus.type
    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY
}

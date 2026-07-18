package site.siredvin.yars.forge

import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.items.IItemHandler
import site.siredvin.yars.YarsCore
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity

@Mod.EventBusSubscriber(modid = YarsCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object AutomaticRewardBoxCapability {
    @JvmStatic
    @SubscribeEvent
    @Suppress("DEPRECATION")
    fun attach(event: AttachCapabilitiesEvent<BlockEntity>) {
        val box = event.`object` as? AutomaticRewardBoxBlockEntity ?: return
        val optional = LazyOptional.of { Handler(box) }
        event.addCapability(
            ResourceLocation(YarsCore.MOD_ID, "automatic_reward_box"),
            object : ICapabilityProvider {
                override fun <T : Any?> getCapability(capability: Capability<T>, side: Direction?): LazyOptional<T> = if (capability === ForgeCapabilities.ITEM_HANDLER) optional.cast() else LazyOptional.empty()
            },
        )
        event.addListener(optional::invalidate)
    }

    private class Handler(private val box: AutomaticRewardBoxBlockEntity) : IItemHandler {
        override fun getSlots(): Int = box.containerSize
        override fun getStackInSlot(slot: Int): ItemStack = box.getItem(slot)
        override fun getSlotLimit(slot: Int): Int = box.maxStackSize
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = box.canPlaceItem(slot, stack)
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = if (slot < AutomaticRewardBoxBlockEntity.PAYMENT_SLOTS) box.insertPayment(stack, simulate) else stack

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = box.extractOutput(slot, amount, simulate)
    }
}

package site.siredvin.yars.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity;

@Mixin(BaseContainerBlockEntity.class)
abstract class AutomaticRewardBoxItemHandlerMixin {
    @Inject(
            method = "createUnSidedHandler()Lnet/minecraftforge/items/IItemHandler;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void yars$createAutomaticRewardBoxHandler(CallbackInfoReturnable<IItemHandler> callback) {
        if ((Object) this instanceof AutomaticRewardBoxBlockEntity box) {
            callback.setReturnValue(new Handler(box));
        }
    }

    private record Handler(AutomaticRewardBoxBlockEntity box) implements IItemHandler {
        @Override
        public int getSlots() {
            return AutomaticRewardBoxBlockEntity.SIZE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot >= 0 && slot < getSlots() ? box.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return box.getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0
                    && slot < AutomaticRewardBoxBlockEntity.PAYMENT_SLOTS
                    && box.canPlaceItem(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
            ItemStack stored = box.getItem(slot);
            if (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, stack)) return stack;
            int moved = Math.min(stack.getCount(), Math.min(box.getMaxStackSize(), stack.getMaxStackSize()) - stored.getCount());
            if (moved <= 0) return stack;
            if (!simulate) {
                ItemStack replacement = stored.isEmpty() ? stack.copyWithCount(moved) : stored.copy();
                if (!stored.isEmpty()) replacement.grow(moved);
                box.setItem(slot, replacement);
            }
            return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return box.extractOutput(slot, amount, simulate);
        }
    }
}

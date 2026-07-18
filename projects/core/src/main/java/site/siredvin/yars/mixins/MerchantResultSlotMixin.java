package site.siredvin.yars.mixins;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.siredvin.yars.common.rewardshop.RewardShopMerchant;
import site.siredvin.yars.common.block.AutomaticRewardBoxMerchant;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Merchant merchant;

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void yars$validateRewardShopOffer(int amount, CallbackInfoReturnable<ItemStack> callback) {
        if (merchant instanceof AutomaticRewardBoxMerchant
                || merchant instanceof RewardShopMerchant rewardShop && !rewardShop.approveTrade(slots.getActiveOffer())) {
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }
}

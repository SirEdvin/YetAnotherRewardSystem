package site.siredvin.yars.mixins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.siredvin.yars.common.rewardshop.RewardShopMerchant;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void yars$validateQuickMovedRewardShopOffer(
            Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        if (index == 2
                && trader instanceof RewardShopMerchant rewardShop
                && !rewardShop.approveTrade(tradeContainer.getActiveOffer())) {
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
    private void yars$skipEntityTradeSound(CallbackInfo callback) {
        if (trader instanceof RewardShopMerchant) {
            callback.cancel();
        }
    }
}

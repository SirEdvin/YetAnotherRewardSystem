package site.siredvin.yars.testmod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import site.siredvin.testiarium.Testiarium;

@Mod("yars_testmod")
public final class YarsTestMod {
    public YarsTestMod() {
        YarsTestiarium.registerHooks();
        Testiarium.register(RewardShopClientTests.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> YarsClientTestHooks::register);
    }
}

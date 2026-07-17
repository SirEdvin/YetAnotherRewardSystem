package site.siredvin.yars.testmod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("yars_testmod")
public final class YarsTestMod {
    public YarsTestMod() {
        YarsTestiarium.registerHooks();
        YarsTests.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> YarsClientTestHooks::register);
    }
}

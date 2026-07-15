package site.siredvin.yars.testmod.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldOpenFlows.class)
class WorldOpenFlowsMixin {
    /** Prevent unattended client tests from stopping at the backup prompt. */
    @Overwrite
    private void askForBackup(Screen screen, String level, boolean customised, Runnable action) {
        action.run();
    }
}

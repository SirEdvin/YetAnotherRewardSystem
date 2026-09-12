package site.siredvin.yars.mixins.client;

import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity;

@Mixin(value = AutomaticRewardBoxBlockEntity.class, remap = false)
abstract class AutomaticRewardBoxRenderBoundsMixin implements IForgeBlockEntity {
    @Override
    public AABB getRenderBoundingBox() {
        // Forge additionally culls global block-entity renderers against this box.
        var pos = ((AutomaticRewardBoxBlockEntity) (Object) this).getBlockPos();
        return new AABB(pos).inflate(0.5, 1.0, 0.5);
    }
}

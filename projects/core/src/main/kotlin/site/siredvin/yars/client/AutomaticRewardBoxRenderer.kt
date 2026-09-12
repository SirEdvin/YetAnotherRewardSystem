package site.siredvin.yars.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.entity.BlockEntityType
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity

class AutomaticRewardBoxRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<AutomaticRewardBoxBlockEntity> {
    private val items = context.itemRenderer
    private val font = context.font
    private val dispatcher = context.blockEntityRenderDispatcher

    companion object {
        @Suppress("DEPRECATION")
        fun register(register: (BlockEntityType<AutomaticRewardBoxBlockEntity>, BlockEntityRendererProvider<AutomaticRewardBoxBlockEntity>) -> Unit) {
            BuiltInRegistries.BLOCK.filterIsInstance<AutomaticRewardBoxBlock>().map { it.entityType() }.distinct().forEach {
                // These types belong to AutomaticRewardBoxBlock, whose factory always creates this block entity.
                @Suppress("UNCHECKED_CAST")
                register(it as BlockEntityType<AutomaticRewardBoxBlockEntity>, BlockEntityRendererProvider(::AutomaticRewardBoxRenderer))
            }
        }
    }

    // The row extends above its block; distance culling still applies when the block itself leaves the frustum.
    override fun shouldRenderOffScreen(blockEntity: AutomaticRewardBoxBlockEntity): Boolean = true

    override fun render(box: AutomaticRewardBoxBlockEntity, partialTick: Float, pose: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        if (!box.showTradeDisplay) return
        val stacks = box.displayedItems
        if (stacks.size !in 2..3) return
        val displayLight = box.level?.let { LevelRenderer.getLightColor(it, box.blockPos.above()) } ?: light
        pose.pushPose()
        pose.translate(0.5, 1.45, 0.5)
        pose.mulPose(dispatcher.camera.rotation())
        stacks.forEachIndexed { index, stack ->
            val x = ((stacks.size - 1) / 2.0 - index) * 0.55
            pose.pushPose()
            pose.translate(x, 0.0, 0.0)
            pose.pushPose()
            pose.scale(0.32f, 0.32f, 0.32f)
            items.renderStatic(stack, ItemDisplayContext.FIXED, displayLight, overlay, pose, buffer, box.level, index)
            pose.popPose()
            text(stack.count.toString(), 0.0, -0.22, pose, buffer, displayLight)
            if (index < stacks.lastIndex) text(if (index == stacks.lastIndex - 1) "→" else "+", -0.275, 0.0, pose, buffer, displayLight)
            pose.popPose()
        }
        pose.popPose()
    }

    private fun text(value: String, x: Double, y: Double, pose: PoseStack, buffer: MultiBufferSource, light: Int) {
        pose.pushPose()
        pose.translate(x, y, 0.02)
        pose.scale(-0.018f, -0.018f, 0.018f)
        font.drawInBatch(value, -font.width(value) / 2f, -font.lineHeight / 2f, 0xFFFFFF, true, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x60000000, light)
        pose.popPose()
    }
}

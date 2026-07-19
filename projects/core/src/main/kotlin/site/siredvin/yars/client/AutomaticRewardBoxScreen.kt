package site.siredvin.yars.client

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.MerchantScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MerchantMenu

class AutomaticRewardBoxScreen(
    menu: MerchantMenu,
    inventory: Inventory,
    title: Component,
) : MerchantScreen(menu, inventory, title) {
    companion object {
        @Suppress("DEPRECATION")
        private val CONTAINER_TEXTURE = ResourceLocation("textures/gui/container/generic_54.png")
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.renderBg(graphics, partialTick, mouseX, mouseY)
        graphics.fill(leftPos + 107, topPos + 17, leftPos + 269, topPos + 75, 0xFFC6C6C6.toInt())
        graphics.blit(CONTAINER_TEXTURE, leftPos + 124, topPos + 34, 0, 7f, 17f, 108, 18, 256, 256)
    }
}

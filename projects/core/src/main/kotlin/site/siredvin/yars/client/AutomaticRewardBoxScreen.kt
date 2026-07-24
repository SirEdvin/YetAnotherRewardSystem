package site.siredvin.yars.client

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.MerchantScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MerchantMenu
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu
import site.siredvin.yars.mixins.client.MerchantScreenAccessor

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

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        val selected = (menu as AutomaticRewardBoxMenu).selectedTradeIndex
        val row = selected - (this as MerchantScreenAccessor).`yars$getScrollOffset`()
        if (row !in 0 until 7) return
        val x = leftPos + 5
        val y = topPos + 18 + row * 20
        val color = 0xFFFFD700.toInt()
        graphics.fill(x, y, x + 88, y + 1, color)
        graphics.fill(x, y + 19, x + 88, y + 20, color)
        graphics.fill(x, y + 1, x + 1, y + 19, color)
        graphics.fill(x + 87, y + 1, x + 88, y + 19, color)
    }
}

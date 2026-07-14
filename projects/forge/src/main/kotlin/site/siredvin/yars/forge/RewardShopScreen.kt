package site.siredvin.yars.forge

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import site.siredvin.yars.common.rewardshop.RewardShopMenu

class RewardShopScreen(menu: RewardShopMenu, inventory: Inventory, title: Component) : AbstractContainerScreen<RewardShopMenu>(menu, inventory, title) {
    init {
        imageWidth = 276
        imageHeight = 166
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight)
        button(graphics, leftPos + 7, topPos + 35, 18, 20, "<")
        button(graphics, leftPos + 59, topPos + 35, 18, 20, ">")
        button(graphics, leftPos + 211, topPos + 59, 49, 18, "Buy")
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, title, 8, 6, 0x404040, false)
        graphics.drawString(font, "${menu.selected.get() + 1}/${menu.available.get()}", 30, 41, 0x404040, false)
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val id = when {
                hit(mouseX, mouseY, 7, 35, 18, 20) -> 1
                hit(mouseX, mouseY, 59, 35, 18, 20) -> 2
                hit(mouseX, mouseY, 211, 59, 49, 18) || hit(mouseX, mouseY, 220, 37, 16, 16) -> 0
                else -> -1
            }
            if (id >= 0) {
                minecraft!!.gameMode?.handleInventoryButtonClick(menu.containerId, id) ?: return false
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun hit(mouseX: Double, mouseY: Double, x: Int, y: Int, width: Int, height: Int): Boolean = mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height

    private fun button(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, text: String) {
        graphics.fill(x, y, x + width, y + height, 0xFF808080.toInt())
        graphics.drawCenteredString(font, text, x + width / 2, y + (height - 8) / 2, 0xFFFFFF)
    }

    private companion object {
        val TEXTURE = ResourceLocation.tryParse("minecraft:textures/gui/container/villager.png")!!
    }
}

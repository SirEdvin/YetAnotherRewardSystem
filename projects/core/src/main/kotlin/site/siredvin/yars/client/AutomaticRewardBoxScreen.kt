package site.siredvin.yars.client

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Inventory
import site.siredvin.yars.common.block.AutomaticRewardBoxMenu

class AutomaticRewardBoxScreen(
    menu: AutomaticRewardBoxMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<AutomaticRewardBoxMenu>(menu, inventory, title) {
    companion object {
        @Suppress("DEPRECATION")
        private val TEXTURE = ResourceLocation("textures/gui/container/villager2.png")
        private const val VISIBLE_OFFERS = 7
    }

    private var selected = 0
    private var scroll = 0

    init {
        imageWidth = 276
        inventoryLabelX = 107
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0f, 0f, imageWidth, imageHeight, 512, 256)
        graphics.fill(leftPos + 107, topPos + 17, leftPos + 269, topPos + 75, 0xFFC6C6C6.toInt())
        repeat(9) { index ->
            val x = leftPos + 135 + index % 3 * 18
            val y = topPos + 18 + index / 3 * 18
            graphics.fill(x, y, x + 18, y + 18, 0xFF373737.toInt())
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B.toInt())
        }
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, Component.translatable("merchant.trades"), 38, 6, 0x404040, false)
        graphics.drawString(font, title, 188 - font.width(title) / 2, 6, 0x404040, false)
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        super.render(graphics, mouseX, mouseY, partialTick)
        menu.offers.drop(scroll).take(VISIBLE_OFFERS).forEachIndexed { row, offer ->
            val x = leftPos + 5
            val y = topPos + 18 + row * 20
            if (selected == row + scroll) graphics.fill(x, y, x + 88, y + 20, 0x55FFFFFF)
            graphics.renderFakeItem(offer.costA, x + 5, y + 2)
            graphics.renderItemDecorations(font, offer.costA, x + 5, y + 2)
            offer.costB.takeUnless { it.isEmpty }?.let {
                graphics.renderFakeItem(it, x + 35, y + 2)
                graphics.renderItemDecorations(font, it, x + 35, y + 2)
            }
            graphics.drawString(font, ">", x + 56, y + 6, 0x606060, false)
            graphics.renderFakeItem(offer.result, x + 68, y + 2)
            graphics.renderItemDecorations(font, offer.result, x + 68, y + 2)
        }
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val row = ((mouseY - topPos - 18) / 20).toInt()
        if (mouseX >= leftPos + 5 && mouseX < leftPos + 93 && row in 0 until VISIBLE_OFFERS) {
            val index = row + scroll
            if (index < menu.offers.size) {
                selected = index
                menu.setSelectionHint(index)
                minecraft?.connection?.send(ServerboundSelectTradePacket(index))
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        scroll = Mth.clamp(scroll - delta.toInt(), 0, (menu.offers.size - VISIBLE_OFFERS).coerceAtLeast(0))
        return true
    }
}

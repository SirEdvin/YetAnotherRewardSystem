package site.siredvin.yars.common.setup

import net.minecraft.world.item.Item
import site.siredvin.broccolium.modules.base.item.DescriptiveItem
import site.siredvin.yars.xplat.ModPlatform

object Items {
    val YARS_ITEM = ModPlatform.registerItem("yars_item") {
        DescriptiveItem(Item.Properties())
    }

    fun doSomething() {}
}

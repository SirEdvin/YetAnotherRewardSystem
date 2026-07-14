package site.siredvin.yars.data

import net.minecraft.data.models.ItemModelGenerators
import net.minecraft.data.models.model.ModelTemplates
import site.siredvin.yars.common.setup.Items

object ModItemModelProvider {
    fun addModels(generators: ItemModelGenerators) {
        generators.generateFlatItem(Items.YARS_ITEM.get(), ModelTemplates.FLAT_ITEM)
    }
}

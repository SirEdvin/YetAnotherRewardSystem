package site.siredvin.yars.data

import net.minecraft.data.PackOutput
import site.siredvin.yars.common.setup.Items
import java.util.function.Consumer

class ModEnLanguageProvider(output: PackOutput) : ModLanguageProvider(output, "en_us") {
    companion object {
        private val hooks: MutableList<Consumer<ModEnLanguageProvider>> = mutableListOf()

        fun addHook(hook: Consumer<ModEnLanguageProvider>) {
            hooks.add(hook)
        }
    }

    override fun addTranslations() {
        add(Items.YARS_ITEM.get(), "YARS Item", "A configurable reward-system item")
        add(ModText.CREATIVE_TAB, "Yet Another Reward System")
        hooks.forEach { it.accept(this) }
    }
}

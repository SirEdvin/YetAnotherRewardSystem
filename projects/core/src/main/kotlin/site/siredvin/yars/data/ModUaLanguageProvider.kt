package site.siredvin.yars.data

import net.minecraft.data.PackOutput
import site.siredvin.yars.common.setup.Blocks
import java.util.function.Consumer

class ModUaLanguageProvider(output: PackOutput) : ModLanguageProvider(output, "uk_ua") {
    companion object {
        private val hooks: MutableList<Consumer<ModUaLanguageProvider>> = mutableListOf()

        fun addHook(hook: Consumer<ModUaLanguageProvider>) {
            hooks.add(hook)
        }
    }

    override fun addTranslations() {
        add(Blocks.REWARD_SHOP.get(), "Крамниця нагород")
        add(ModText.CREATIVE_TAB, "Yet Another Reward System")
        hooks.forEach { it.accept(this) }
    }
}

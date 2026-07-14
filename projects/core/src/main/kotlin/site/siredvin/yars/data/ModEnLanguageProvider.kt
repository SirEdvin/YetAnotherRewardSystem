package site.siredvin.yars.data

import net.minecraft.data.PackOutput
import site.siredvin.yars.common.setup.Blocks
import java.util.function.Consumer

class ModEnLanguageProvider(output: PackOutput) : ModLanguageProvider(output, "en_us") {
    companion object {
        private val hooks: MutableList<Consumer<ModEnLanguageProvider>> = mutableListOf()

        fun addHook(hook: Consumer<ModEnLanguageProvider>) {
            hooks.add(hook)
        }
    }

    override fun addTranslations() {
        add(Blocks.REWARD_SHOP.get(), "Reward Shop")
        add(ModText.CREATIVE_TAB, "Yet Another Reward System")
        hooks.forEach { it.accept(this) }
    }
}

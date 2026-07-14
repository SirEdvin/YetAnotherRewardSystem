package site.siredvin.yars.common.configuration

import net.minecraftforge.common.ForgeConfigSpec

object ModConfig {
    val enableSomething: Boolean
        get() = ConfigHolder.commonConfig.enableSomething.get()

    class CommonConfig internal constructor(builder: ForgeConfigSpec.Builder) {
        var enableSomething: ForgeConfigSpec.BooleanValue

        init {
            builder.push("base")
            enableSomething = builder.comment("Something enabled")
                .define("enableSomething", true)
            builder.pop()
        }
    }
}

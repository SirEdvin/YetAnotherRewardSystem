package site.siredvin.template.common.configuration

import net.neoforged.neoforge.common.ModConfigSpec

object ModConfig {

    val enableSomething: Boolean
        get() = ConfigHolder.commonConfig.enableSomething.get()

    class CommonConfig internal constructor(builder: ModConfigSpec.Builder) {

        // Generic plugins
        var enableSomething: ModConfigSpec.BooleanValue

        init {
            builder.push("base")
            enableSomething = builder.comment("Something enabled")
                .define("enableSomething", true)
            builder.pop()
        }
    }
}

pluginManagement {
    repositories {
        maven("https://mvn.siredvin.site/minecraft") {
            name = "SirEdvin's Maven proxy"
        }
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}

val minecraftVersion: String by settings
rootProject.name = "YetAnotherRewardSystem $minecraftVersion"

include(":core")
include(":forge")
include(":fabric")


for (project in rootProject.children) {
    project.projectDir = file("projects/${project.name}")
}

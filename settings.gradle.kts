pluginManagement {
    repositories {
        maven("https://repo.repsy.io/siredvin/default") {
            name = "Repsy"
            content {
                includeGroupByRegex("site\\.siredvin.*")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            name = "Forge"
            content {
                includeGroupByRegex("net\\.minecraftforge.*")
            }
        }
        maven("https://maven.parchmentmc.org") {
            name = "ParchmentMC"
            content {
                includeGroupByRegex("org\\.parchmentmc.*")
            }
        }
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge"
            content {
                includeGroupByRegex("org\\.spongepowered.*")
            }
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroupByRegex("(net\\.fabricmc.*|fabric-loom)")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
            if (requested.id.id.startsWith("site.siredvin.")) {
                useModule("site.siredvin:modding-buildenv:${requested.version}")
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

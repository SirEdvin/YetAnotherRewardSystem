import site.siredvin.peripheralium.gradle.mavenDependencies

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("site.siredvin.publishing")
    id("site.siredvin.mod-publishing")
    id("site.siredvin.forge")
}

baseShaking {
    projectPart.set("forge")
    integrationRepositories.set(true)
    shake()
}

forgeShaking {
    commonProjectName.set("core")
    useMixins.set(true)
    useAT.set(false)
    useJarJar.set(false)
    extraVersionMappings.set(
        mapOf(
            "broccolium" to "broccolium",
            "kubejs" to "kubejs",
        ),
    )
    shake()
}

repositories {
    maven {
        name = "Latvian Mods"
        url = uri("https://maven.latvian.dev/releases")
        content { includeGroup("dev.latvian.mods") }
    }
    // location of the maven that hosts JEI files since January 2023
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.forge.raw)
    libs.bundles.forge.base.get().map { implementation(fg.deobf(it)) }
    implementation(fg.deobf(libs.kubejs.forge))

    libs.bundles.externalMods.forge.runtime.get().map { runtimeOnly(fg.deobf(it)) }
}

// modPublishing {
//    output.set(tasks.jar)
//    requiredDependencies.set(
//        listOf(
//            "cc-tweaked",
//            "kotlin-for-forge",
//            "peripheralium"
//        ),
//    )
//    shake()
// }

publishingShaking {
    shake()
    project.publishing {
        publications {
            named<MavenPublication>("maven") {
                fg.component(this)
                mavenDependencies {
                    exclude(dependencies.create("site.siredvin:"))
                    exclude(libs.jei.forge.get())
                }
            }
        }
    }
}

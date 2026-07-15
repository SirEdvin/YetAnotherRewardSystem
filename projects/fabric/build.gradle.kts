import site.siredvin.peripheralium.gradle.mavenDependencies

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("site.siredvin.fabric")
    id("site.siredvin.publishing")
    id("site.siredvin.mod-publishing")
}

val modVersion: String by extra
val minecraftVersion: String by extra
val modBaseName: String by extra
val testiariumFabric = (dependencies.create("site.siredvin:testiarium-fabric-1.20.1:${libs.versions.testiarium.get()}") as ExternalModuleDependency).apply {
    isTransitive = false
}

baseShaking {
    projectPart.set("fabric")
    integrationRepositories.set(true)
    shake()
}

fabricShaking {
    commonProjectName.set("core")
    createRefmap.set(true)
    accessWidener.set(project(":core").file("src/main/resources/yars.accesswidener"))
    extraVersionMappings.set(
        mapOf(
            "forgeconfigapiport" to "forgeconfigapirt",
            "broccolium" to "broccolium",
            "kubejs" to "kubejs",
        ),
    )
    shake()
}

val testMod = sourceSets.create("testMod") {
    resources.srcDir(project(":core").file("src/testMod/resources"))
    compileClasspath += sourceSets.main.get().compileClasspath
    compileClasspath += sourceSets.main.get().output
    compileClasspath += project(":core").sourceSets.main.get().output
    compileClasspath += project(":core").sourceSets["testMod"].output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
    runtimeClasspath += sourceSets.main.get().output
    runtimeClasspath += project(":core").sourceSets.main.get().output
    runtimeClasspath += project(":core").sourceSets["testMod"].output
}

net.fabricmc.loom.configuration.RemapConfigurations.setupForSourceSet(project, testMod)

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
    maven {
        name = "ModMenu maven"
        url = uri("https://maven.terraformersmc.com/releases")
        content {
            includeGroup("com.terraformersmc")
        }
    }
}

dependencies {
    implementation(libs.bundles.kotlin)

    modImplementation(libs.bundles.fabric.core)
    modImplementation(libs.bundles.fabric.base) {
        isTransitive = false
    }
    modImplementation(libs.fabric.config)
    modImplementation(libs.kubejs.fabric)

    modRuntimeOnly(libs.bundles.externalMods.fabric.runtime) {
        isTransitive = false
    }
    add("modTestModImplementation", testiariumFabric)
}

loom {
    mods {
        register("yars_testmod") {
            sourceSet(testMod)
            sourceSet(project(":core").sourceSets["testMod"])
        }
    }
    runs {
        create("clientGameTest") {
            client()
            source(testMod)
            property("testiarium.client", "true")
            property("testiarium.tags", "client")
            property("testiarium.structures", layout.buildDirectory.dir("resources/testMod/gameteststructures").get().asFile.absolutePath)
            property("testiarium.gametest-report", layout.buildDirectory.file("test-results/client-gametest.xml").get().asFile.absolutePath)
            property("testiarium.screenshots", layout.buildDirectory.get().asFile.absolutePath)
            vmArg("-ea")
            runDir("run/client-gametest")
        }
    }
}

// modPublishing {
//    output.set(tasks.remapJar)
//    requiredDependencies.set(
//        listOf(
//            "cc-tweaked",
//            "fabric-language-kotlin",
//            "peripheralium",
//        ),
//    )
//    requiredDependenciesCurseforge.add("forge-config-api-port-fabric")
//    requiredDependenciesModrinth.add("forge-config-api-port")
//    shake()
// }

publishingShaking {
    shake()
    project.publishing {
        publications {
            named<MavenPublication>("maven") {
                mavenDependencies {
                    exclude(project.dependencies.create("site.siredvin:"))
                }
            }
        }
    }
}

import site.siredvin.peripheralium.gradle.mavenDependencies

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("site.siredvin.publishing")
    id("site.siredvin.mod-publishing")
    id("site.siredvin.forge")
}

val testiariumVersion = libs.versions.testiarium.get()
val testiariumCore = configurations.detachedConfiguration(
    dependencies.create(fg.deobf("site.siredvin:testiarium-core-1.20.1:$testiariumVersion@jar")),
)

baseShaking {
    projectPart.set("forge")
    integrationRepositories.set(false)
    shake()
}

forgeShaking {
    commonProjectName.set("core")
    useMixins.set(true)
    useAT.set(true)
    useJarJar.set(false)
    extraVersionMappings.set(
        mapOf(
            "broccolium" to "broccolium",
            "kubejs" to "kubejs",
        ),
    )
    shake()
}

val testMod = sourceSets.create("testMod") {
    resources.srcDir(project(":core").file("src/testMod/resources"))
    resources.srcDir(layout.buildDirectory.dir("generated/testiarium-core"))
    compileClasspath += sourceSets.main.get().compileClasspath
    compileClasspath += sourceSets.main.get().output
    compileClasspath += project(":core").sourceSets.main.get().output
    compileClasspath += project(":core").sourceSets["testMod"].output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
    runtimeClasspath += sourceSets.main.get().output
    runtimeClasspath += project(":core").sourceSets.main.get().output
    runtimeClasspath += project(":core").sourceSets["testMod"].output
    compileClasspath += testiariumCore
    runtimeClasspath += testiariumCore
}

repositories {
    maven {
        name = "SirEdvin's Maven proxy"
        url = uri("https://mvn.siredvin.site/minecraft")
    }
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.forge.raw)
    libs.bundles.forge.base.get().map { implementation(fg.deobf(it)) }
    implementation(fg.deobf(libs.kubejs.forge))
    implementation(fg.deobf(libs.architectury.forge))
    implementation(fg.deobf(libs.rhino.forge))

    libs.bundles.externalMods.forge.runtime.get().map { runtimeOnly(fg.deobf(it)) }
}

val unpackTestiariumCore = tasks.register<Sync>("unpackTestiariumCore") {
    from(testiariumCore.map(::zipTree))
    into(layout.buildDirectory.dir("generated/testiarium-core"))
}
tasks.named(testMod.processResourcesTaskName) {
    dependsOn(unpackTestiariumCore)
}

minecraft {
    runs {
        create("clientGameTest") {
            parent(runs.getByName("client"))
            workingDirectory(file("run/client-gametest"))
            property("forge.enabledGameTestNamespaces", "yars_testmod")
            property("testiarium.client", "true")
            property("testiarium.tags", "client")
            property("testiarium.structures", layout.buildDirectory.dir("resources/testMod/gameteststructures").get().asFile.absolutePath)
            property("testiarium.gametest-report", layout.buildDirectory.file("test-results/client-gametest.xml").get().asFile.absolutePath)
            property("testiarium.screenshots", layout.buildDirectory.get().asFile.absolutePath)
            jvmArgs("-ea")
            mods {
                create("yars") {
                    source(sourceSets.main.get())
                    source(project(":core").sourceSets.main.get())
                }
                create("yars_testmod") {
                    source(testMod)
                    source(project(":core").sourceSets["testMod"])
                }
            }
        }
    }
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

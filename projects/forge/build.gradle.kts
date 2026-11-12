import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import site.siredvin.peripheralium.gradle.mavenDependencies
import java.util.zip.ZipFile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("net.neoforged.moddev.legacyforge")
    id("site.siredvin.publishing")
    id("site.siredvin.mod-publishing")
}

val modBaseName: String by extra
val commonProject = project(":core")

baseShaking {
    projectPart.set("forge")
    integrationRepositories.set(true)
    shake()
}

val testMod = sourceSets.create("testMod") {
    resources.srcDir(commonProject.file("src/testMod/resources"))
    compileClasspath += sourceSets.main.get().compileClasspath
    compileClasspath += sourceSets.main.get().output
    compileClasspath += commonProject.sourceSets["testMod"].output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
    runtimeClasspath += sourceSets.main.get().output
    runtimeClasspath += commonProject.sourceSets["testMod"].output
}

legacyForge {
    enable { forgeVersion = "1.20.1-${libs.versions.forge.asProvider().get()}" }
    parchment {
        minecraftVersion = libs.versions.parchmentMc.get()
        mappingsVersion = libs.versions.parchment.get()
    }
    validateAccessTransformers.set(true)
    setAccessTransformers(sourceSets.main.map { it.resources.matching { include("META-INF/accesstransformer.cfg") } })
    mods {
        create("yars") { sourceSet(sourceSets.main.get()) }
        create("yars_testmod") {
            sourceSet(testMod)
            sourceSet(commonProject.sourceSets["testMod"])
        }
    }
    runs {
        create("client") {
            client()
            gameDirectory = file("run")
        }
        create("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }
        create("data") {
            data()
            gameDirectory = file("run")
            programArguments.addAll(
                "--mod", modBaseName, "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", commonProject.file("src/main/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath,
            )
        }
        create("clientGameTest") {
            client()
            sourceSet.set(testMod)
            gameDirectory = file("run/client-gametest")
            systemProperty("forge.enabledGameTestNamespaces", "yars_testmod")
            systemProperty("testiarium.client", "true")
            systemProperty("testiarium.tags", "client")
            systemProperty("testiarium.structures", layout.buildDirectory.dir("resources/testMod/gameteststructures").get().asFile.absolutePath)
            systemProperty("testiarium.gametest-report", layout.buildDirectory.file("test-results/client-gametest.xml").get().asFile.absolutePath)
            systemProperty("testiarium.screenshots", layout.buildDirectory.get().asFile.absolutePath)
            jvmArgument("-ea")
            loadedMods.add(mods.named("yars"))
            loadedMods.add(mods.named("yars_testmod"))
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modBaseName.refmap.json")
    config("$modBaseName.mixins.json")
}

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge/") { content { includeGroup("thedarkcolour") } }
    maven("https://maven.latvian.dev/releases") {
        name = "Latvian"
        content {
            includeGroupByRegex("dev\\.latvian.*")
        }
    }
    maven("https://maven.blamejared.com/") { content { includeGroup("mezz.jei") } }
    maven("https://repo.repsy.io/siredvin/default") {
        name = "Repsy"
        content {
            includeGroupByRegex("site\\.siredvin.*")
        }
    }
}

val obfuscation = extensions.getByType<ObfuscationExtension>()
val modImplementation = configurations.getByName("modImplementation")
val modRuntimeOnly = configurations.getByName("modRuntimeOnly")
val modTestImplementation = obfuscation.createRemappingConfiguration(configurations.getByName(testMod.implementationConfigurationName))

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.forge.raw)
    compileOnly(commonProject)
    add(modImplementation.name, libs.bundles.forge.base)
    add(modImplementation.name, libs.kubejs.forge)
    add(modImplementation.name, libs.architectury.forge)
    add(modImplementation.name, libs.rhino.forge)
    add(modImplementation.name, libs.jade.forge)
    add(modRuntimeOnly.name, libs.bundles.externalMods.forge.runtime)
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    add(modTestImplementation.name, libs.testiarium.forge)
    add(modTestImplementation.name, libs.testiarium.core)
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") { source(commonProject.sourceSets.main.get().allSource) }
tasks.named<JavaCompile>("compileJava") { source(commonProject.sourceSets.main.get().allSource) }

tasks.processResources {
    from(commonProject.sourceSets.main.get().resources)
    val versions = mapOf(
        "version" to project.version,
        "file" to mapOf("jarVersion" to project.version),
        "forgeVersion" to libs.versions.forge.asProvider().get(),
        "broccoliumVersion" to libs.versions.broccolium.asProvider().get(),
        "kubejsVersion" to libs.versions.kubejs.get(),
    )
    inputs.properties(versions)
    inputs.file(buildFile)
    filesMatching("META-INF/mods.toml") { expand(versions) }
    exclude(".cache")
}
tasks.jar { manifest.attributes["MixinConfigs"] = "$modBaseName.mixins.json" }
extra["releaseJar"] = "reobfJar"

val verifyReleaseMixins = tasks.register("verifyReleaseMixins") {
    dependsOn("reobfJar")
    val releaseJar = tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }
    inputs.file(releaseJar)
    doLast {
        ZipFile(releaseJar.get().asFile).use { jar ->
            fun text(path: String): String {
                val entry = checkNotNull(jar.getEntry(path)) { "Missing release resource: $path" }
                return jar.getInputStream(entry).use { String(it.readBytes(), Charsets.ISO_8859_1) }
            }
            check(text("yars.mixins.json").contains("\"refmap\": \"yars.refmap.json\""))
            val refmap = text("yars.refmap.json")
            for (mixin in listOf("MerchantMenuMixin", "MerchantResultSlotMixin", "SlotAccessor", "client/MerchantScreenAccessor")) {
                check(refmap.contains("site/siredvin/yars/mixins/$mixin")) { "Missing release mappings: $mixin" }
            }
            val merchant = text("site/siredvin/yars/mixins/MerchantMenuMixin.class")
            check(merchant.contains("f_40027_") && merchant.contains("f_40028_")) {
                "MerchantMenu shadow fields were not reobfuscated for production Forge"
            }
        }
    }
}
tasks.named("check") { dependsOn(verifyReleaseMixins) }
rootProject.tasks.named("githubRelease") { dependsOn(verifyReleaseMixins) }

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

configurations.named("reobfRuntimeElements") {
    extendsFrom(configurations.implementation.get(), modImplementation)
}

publishingShaking {
    shake()
    project.publishing {
        publications {
            named<MavenPublication>("maven") {
                mavenDependencies {
                    exclude(dependencies.create("site.siredvin:"))
                    exclude(libs.jei.forge.get())
                }
            }
        }
    }
}

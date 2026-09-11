import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import site.siredvin.peripheralium.gradle.mavenDependencies
import java.util.zip.ZipFile

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

// MixinGradle's temporary refmap and shadow mappings must survive incremental/cache reuse.
tasks.named<JavaCompile>("compileJava") {
    outputs.dir(layout.buildDirectory.dir("tmp/compileJava"))
}

// Register the generated shadow mappings before they exist on a clean checkout.
tasks.withType<RenameJarInPlace>().matching { it.name == "reobfJar" }.configureEach {
    extraMappings.from(files(layout.buildDirectory.file("tmp/compileJava/compileJava-mappings.tsrg")).builtBy(tasks.named("compileJava")))
}

val verifyReleaseMixins = tasks.register("verifyReleaseMixins") {
    dependsOn("reobfJar")
    val releaseJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
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
    implementation(fg.deobf(libs.jade.forge))

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

// ponytail: Client GameTests need Minecraft's window, not Forge's flaky early splash.
tasks.matching { it.name == "runClientGameTest" }.configureEach {
    doFirst {
        file("run/client-gametest/config").mkdirs()
        file("run/client-gametest/config/fml.toml").writeText("earlyWindowControl = false\n")
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

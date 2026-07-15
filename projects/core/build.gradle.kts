@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("site.siredvin.vanilla")
    id("site.siredvin.publishing")
}

val modVersion: String by extra
val minecraftVersion: String by extra
val modBaseName: String by extra

baseShaking {
    projectPart.set("common")
    integrationRepositories.set(true)
    shake()
}

vanillaShaking {
    accessWideners.add("src/main/resources/yars-common.accesswidener")
    accessWideners.add("src/main/resources/yars.accesswidener")
    shake()
}

val testMod = sourceSets.create("testMod") {
    compileClasspath += sourceSets.main.get().compileClasspath
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.common)
    api(libs.bundles.apicommon)
    testImplementation(libs.junit.jupiter)
    add(testMod.implementationConfigurationName, libs.testiarium.core)
    add(testMod.compileOnlyConfigurationName, libs.mixin)
}

tasks.test {
    useJUnitPlatform()
}

publishingShaking {
    shake()
}

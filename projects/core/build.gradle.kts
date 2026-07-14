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

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.common)
    api(libs.bundles.apicommon)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

publishingShaking {
    shake()
}

plugins {
    java
    alias(libs.plugins.loom) apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.0" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.142" apply false
    id("site.siredvin.root") version "0.9.4"
    id("site.siredvin.release") version "0.9.4"
    id("com.dorongold.task-tree") version "4.0.0"
}

subprojectShaking {
    withKotlin.set(true)
    kotlinVersion.set("2.0.0")
}

val setupSubproject = subprojectShaking::setupSubproject

subprojects {
    setupSubproject(this)
}

githubShaking {
    modBranch.set("1.20")
    projectRepo.set("YetAnotherRewardSystem")
    shake()
}

githubRelease {
    tagName.set(providers.gradleProperty("modVersion"))
    releaseName.set(providers.gradleProperty("modVersion"))
    targetCommitish.set(providers.gradleProperty("releaseCommit").orElse("1.20"))
    draft.set(false)
}

repositories {
    maven("https://repo.repsy.io/siredvin/default") {
        name = "Repsy"
        content {
            includeGroupByRegex("site\\.siredvin.*")
        }
    }
}

plugins {
    java
    id("site.siredvin.root") version "0.8.26"
    id("site.siredvin.release") version "0.8.26"
    id("com.dorongold.task-tree") version "4.0.0"
}

subprojectShaking {
    withKotlin.set(true)
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

repositories {
    maven("https://mvn.siredvin.site/minecraft") {
        name = "SirEdvin's Maven proxy"
    }
}

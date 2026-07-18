# Yet Another Reward System

## Overview

Yet Another Reward System is a Minecraft 1.20.1 mod that provides KubeJS extension points for reward systems. It supports both Fabric and Forge from a shared core module.

## Tech Stack

- Runtime: Minecraft 1.20.1 / JVM
- Language: Kotlin 2.0 and Java
- Loaders: Fabric Loader 0.17 and Forge 47
- Scripting: KubeJS
- Testing: JUnit 5 and Testiarium GameTests
- Build system: Gradle Wrapper

## Key Commands

Log handling is important. Always use an explicit timeout and silently save complete output.

- Build: `mkdir -p build; LOG="build/gradle-$(date +%Y%m%d-%H%M%S).log"; timeout 120s ./gradlew :core:build :fabric:build :forge:build >"$LOG" 2>&1`
- Unit tests: `mkdir -p build; LOG="build/unit-tests-$(date +%Y%m%d-%H%M%S).log"; timeout 120s ./gradlew :core:test >"$LOG" 2>&1`
- Client GameTests: `mkdir -p build; LOG="build/client-gametests-$(date +%Y%m%d-%H%M%S).log"; timeout 180s xvfb-run -a ./gradlew :fabric:runClientGameTest :forge:runClientGameTest >"$LOG" 2>&1`
- Forge server: `mkdir -p build; LOG="build/forge-server-$(date +%Y%m%d-%H%M%S).log"; timeout 90s ./gradlew :forge:runServer >"$LOG" 2>&1`
- Fabric server: `mkdir -p build; LOG="build/fabric-server-$(date +%Y%m%d-%H%M%S).log"; timeout 90s ./gradlew :fabric:runServer >"$LOG" 2>&1`

Increase timeouts only when required. Stop development clients and servers after collecting results. Report the command, exit code, duration, log path, and relevant errors; inspect only the relevant failure window.

## Project Structure

```text
projects/
  core/    # Shared implementation, resources, unit tests, and shared test mod
  fabric/  # Fabric integration and test mod
  forge/   # Forge integration and test mod
gradle/    # Version catalog and Gradle configuration
openspec/  # Change specifications and implementation plans
```

Each module uses `src/main/` for production code. Tests live in `src/test/`, and GameTest support code and resources live in `src/testMod/`.

## Conventions

- Keep loader-independent behavior in `projects/core/`.
- Keep loader API usage in the corresponding `fabric` or `forge` module.
- Follow the official Kotlin code style configured in `gradle.properties`.
- Reuse existing project patterns before adding helpers, abstractions, or dependencies.
- Comments explain why, not what.

## DO NOT MODIFY

- Never edit files under `src/generated`; regenerate them with the relevant datagen task.
- Never commit `build/`, development run directories, logs, EULA files, or local KubeJS smoke-test scripts.
- Do not modify unrelated user changes in a dirty worktree.

## Testing Approach

- Run `:core:test` for shared unit tests.
- Run both Fabric and Forge client GameTests under `xvfb-run` for client or graphics-dependent behavior.
- For KubeJS smoke tests, place scripts in `projects/<loader>/run/server/kubejs/server_scripts/`, run both loader servers, and verify both logs report zero script errors and the target behavior.
- Run the timed multi-loader build before marking code changes complete.
- Fix failing tests rather than skipping them.

## Code Style

- Prefer the smallest correct change.
- Do not add speculative abstractions or dependencies.
- Keep shared and loader-specific responsibilities separated.
- Preserve validation, error handling, and dedicated-server safety.
- If requirements are unclear, ask instead of assuming.

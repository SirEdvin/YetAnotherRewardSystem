# Development Commands

## Gradle

Always use an explicit timeout and silently save complete output. Log handling is important.

```sh
mkdir -p build
LOG="build/gradle-$(date +%Y%m%d-%H%M%S).log"
timeout 120s ./gradlew :core:build :fabric:build :forge:build >"$LOG" 2>&1
```

- Increase the timeout only when required.
- Stop `runClient` and `runServer` after collecting results.
- Regenerate `src/generated` with the relevant datagen task; never edit it manually.
- Report the command, exit code, duration, log path, and relevant errors; inspect only the relevant failure window.

## Tests

Run unit tests with the same timeout and silent log handling:

```sh
LOG="build/unit-tests-$(date +%Y%m%d-%H%M%S).log"
timeout 120s ./gradlew :core:test >"$LOG" 2>&1
```

Run loader client GameTests under a virtual display:

```sh
LOG="build/client-gametests-$(date +%Y%m%d-%H%M%S).log"
timeout 180s xvfb-run -a ./gradlew :fabric:runClientGameTest :forge:runClientGameTest >"$LOG" 2>&1
```

## KubeJS

1. Put the smoke-test script in `projects/<loader>/run/server/kubejs/server_scripts/`.
2. Run Forge: `LOG="build/forge-server-$(date +%Y%m%d-%H%M%S).log"; timeout 90s ./gradlew :forge:runServer >"$LOG" 2>&1`.
3. Run Fabric: `LOG="build/fabric-server-$(date +%Y%m%d-%H%M%S).log"; timeout 90s ./gradlew :fabric:runServer >"$LOG" 2>&1`.
4. Check both logs for zero script errors and verify the target behavior.
5. Do not commit run directories, logs, EULA files, or smoke-test scripts.

# Development Notes

## Gradle

Always run Gradle through an explicit shell timeout to prevent a client or server development run from hanging the session.

```sh
timeout 120s ./gradlew :core:build :fabric:build :forge:build
```

Use a longer timeout only when a task requires it, and terminate long-running `runClient` or `runServer` tasks explicitly after collecting the required logs.

## KubeJS Smoke Tests

Run KubeJS tests against the loader's development server and inspect the server log for script errors.

1. Place the test script in the active run directory: `projects/<loader>/run/server/kubejs/server_scripts/` for Forge or the corresponding Fabric server run directory.
2. Start the server with an explicit timeout, for example `timeout 90s ./gradlew :forge:runServer`.
3. Confirm the log reports the script loaded with zero errors and verify the target event or behavior.
4. Do not commit generated run directories, logs, EULA files, or local KubeJS scripts.

# Development Notes

## Gradle

Always run Gradle through an explicit shell timeout to prevent a client or server development run from hanging the session.

```sh
timeout 120s ./gradlew :core:build :fabric:build :forge:build
```

Use a longer timeout only when a task requires it, and terminate long-running `runClient` or `runServer` tasks explicitly after collecting the required logs.

Never manually create or edit files under `src/generated`; regenerate them with the appropriate Gradle datagen task.

## KubeJS Smoke Tests

Run KubeJS tests against the loader's development server and inspect the server log for script errors.

1. Place the test script in the active run directory: `projects/<loader>/run/server/kubejs/server_scripts/` for Forge or the corresponding Fabric server run directory.
2. Start the server with an explicit timeout, for example `timeout 90s ./gradlew :forge:runServer`.
3. Confirm the log reports the script loaded with zero errors and verify the target event or behavior.
4. Do not commit generated run directories, logs, EULA files, or local KubeJS scripts.

## Java process output and token budget

When work in this repository requires Java, Minecraft, a mod loader, Gradle, or an installer process:

- Treat complete stdout/stderr as a file-backed artifact, not chat context. Redirect it to a timestamped file under a disposable `build/`, `run/`, or `work/logs/` directory and preserve the real exit code.
- Return only a compact summary to the agent: command, exit code, duration, full-log path, readiness/startup time, unique ERROR/FATAL/exception signatures, relevant test or content counts, and clean-shutdown evidence.
- Read a narrow window around a concrete failure from the saved log only when diagnosis requires it. Do not paste an entire `latest.log`, Gradle transcript, download list, or crash report into the conversation.
- Keep stdin attached for long-running servers so they can receive `stop`. Prefer a wrapper that writes every line to disk while emitting only rare readiness/failure markers, and prefer completion/readiness notifications over frequent polling.
- Apply the same rule to Forge/NeoForge/Fabric and Packwiz installers: retain full download and patch progress on disk, but report only success/failure, item counts, and the log path.
- Terminal-side truncation is not filtering: a truncated 50 KiB Java log can still consume roughly 15k model tokens while omitting the line that matters.
- Batch modpack migrations and related fixes before launching Minecraft, then perform consolidated runtime validation instead of restarting after every edit.

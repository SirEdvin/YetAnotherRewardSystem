# Verification

## Implementation coverage

- `AutomaticRewardBoxDisplayTest`: seven unit tests cover legacy/default visibility, both persisted boolean values, exact counts and metadata, defensive copies, minimal update tags, inventory-safe decoding, stage advancement/exhaustion, invalid/detached/restored definitions, transaction independence, and removable history listeners.
- `AutomaticShopDisplayClientTests.tradeDisplayLifecycle`: actual client/server block entities and menu packets cover initial tracking, an offline owner, selection/progress updates from automatic and interactive purchases, no cross-box transaction trigger, hidden automation, invalidation/restoration, removal/revival, authorization failures, stale menus, reopening, keyboard Enter activation, mouse activation, GUI scales 1/2/3, and renderer registration after resource reload.
- The client runs included a real KubeJS `display_smoke_shop` alongside the bundled automatic shop. The normal fixture remains usable without local scripts by falling back to the test-mod automatic shop.
- The native button supplies vanilla focus and narration behavior; the client fixture verifies focus, visible authoritative labels, and Enter activation. No custom accessibility implementation replaces vanilla behavior.

## Successful checks

All commands ran from the repository root with complete output redirected to the named logs.

| Check | Command | Exit | Duration | Log |
| --- | --- | --- | --- | --- |
| Unit tests | `timeout 120s ./gradlew --no-daemon :core:test` | 0 | 77s | `build/unit-tests-20260912-125126.log` |
| Both client suites | `timeout 300s xvfb-run -a ./gradlew --no-daemon :fabric:runClientGameTest :forge:runClientGameTest` | 0 | 248s | `build/client-gametests-20260912-130711.log` |
| Formatting | `timeout 120s ./gradlew --no-daemon :core:spotlessApply :fabric:spotlessApply :forge:spotlessApply` | 0 | 11s | `build/format-20260912-133342.log` |
| Final multi-loader build | `timeout 120s ./gradlew --no-daemon :core:build :fabric:build :forge:build` | 0 | 34s | `build/gradle-20260912-133448.log` |

The final unit XML reports contain 24 tests, zero failures/errors/skips. Each loader's `build/test-results/client-gametest.xml` contains six client tests with zero failures/skips, including the display lifecycle fixture. Formatting after the client run changed whitespace/import ordering and removed unused imports; the final build passed against that formatted source.

## Dedicated-server and real remote-client checks

Temporary run configuration: `/tmp/yars-display-smoke.init.gradle`.
Runner: `/tmp/yars-display-remote-check.py`.
Machine-readable results, including unsuccessful setup attempts: `build/remote-display-results.jsonl`.

For each loader and phase, the runner launched:

- `timeout --kill-after=10s 180s ./gradlew --no-daemon -I /tmp/yars-display-smoke.init.gradle :<loader>:runServer`
- After actual server readiness: `timeout --kill-after=10s 120s xvfb-run -a ./gradlew --no-daemon -I /tmp/yars-display-smoke.init.gradle :<loader>:runClient`

The extended server deadline accommodates client startup and a complete remote handshake. Connections used a local, offline-mode smoke server; the original Forge server properties were backed up and restored after testing. Separate smoke worlds avoided modifying the existing world.

| Loader / phase | Server exit | Client exit | Combined duration | Server / client logs under `build/` |
| --- | --- | --- | --- | --- |
| Fabric / initial | 0 | 0 | 55.8s | `remote-fabric-initial-server-20260912-132416.log`, `remote-fabric-initial-client-20260912-132416.log` |
| Fabric / restart | 0 | 0 | 133.3s | `remote-fabric-restored-server-20260912-132535.log`, `remote-fabric-restored-client-20260912-132535.log` |
| Forge / initial | 0 | 0 | 92.3s | `remote-forge-initial-server-20260912-132844.log`, `remote-forge-initial-client-20260912-132844.log` |
| Forge / restart | 0 | 0 | 127.9s | `remote-forge-restored-server-20260912-133030.log`, `remote-forge-restored-client-20260912-133030.log` |

Successful initial runs recorded `YARS_REMOTE_SNAPSHOT_PASS` and `YARS_REMOTE_TOGGLE_PASS` from the connected client. Restart runs recorded server state `enabled=false count=0`, then `YARS_REMOTE_RESTART_PASS` and `YARS_REMOTE_TOGGLE_PASS` from a newly connected client. Startup/server/client script summaries reported zero errors and warnings in these successful runs. Both dedicated servers stopped normally without client-class loading failures.

## Visual evidence

Screenshots are outside tracked source directories:

- `/tmp/yars-trade-display/fabric/screenshots/`
- `/tmp/yars-trade-display/forge/screenshots/`

Reviewed both loaders for the two/three-item rows, quantity 1 and multi-digit quantities, tagged result stacks, payment/result ordering, placement above adjacent shops, opposite camera view, above-block culling, and GUI scales 1/2/3. The fixture also captures `toggle-off-ui.png` and `resource-reload-adjacent-shops.png` and asserts hidden snapshots and retained renderer registration. The contact sheet at `/tmp/yars-trade-display/contact-sheet.jpg` is a convenience preview; full-resolution images are authoritative.

## Issues found and resolved

- Initial compilation rejected use of a deprecated registry API under warnings-as-errors. A narrowly scoped suppression documents use of the pinned 1.20.1 registry API.
- The old client runner entered a world before initial model/resource loading finished. Both loader test hooks now start on a client tick after the loading overlay closes; rendering stability also requires no overlay.
- KubeJS Fabric block registration can follow this mod's client initializer. Renderer type discovery now runs in the existing Fabric model-loading callback, before renderer creation on initial load and resource reload. Forge uses `EntityRenderersEvent.RegisterRenderers`.
- Visual inspection caught back-facing quantity text and underlit item models. Text uses the native billboard orientation, and light is sampled above the shop rather than inside its block. Forge has a client-only bounding-box extension for the above-block row.
- The first combined client run exceeded 180s (`build/client-gametests-20260912-130243.log`, exit 124). The subsequent 300s run completed normally; the timeout was not treated as a pass.
- Remote smoke setup needed loader-specific run-directory configuration, deferred Forge task configuration, and supported KubeJS accessors instead of filtered Java `System` access. Failed setup runs are preserved in the JSONL and timestamped logs and are not counted as acceptance evidence.
- The first final build failed only on formatting (`build/gradle-20260912-133304.log`, exit 1, 14s). Spotless fixed the violations; the full build then passed.

## Cleanup and scope

The original Forge server properties were restored byte-for-byte. Task-created active startup/server/client scripts were moved to `/tmp/yars-trade-display/smoke/`, retaining reproducibility without leaving automatic login/logout hooks active. No project development client/server process remained at final cleanup; unrelated projects' processes were left alone. Generated source directories, runtime worlds, EULA files, logs, screenshots, and local smoke scripts are not part of the source diff.

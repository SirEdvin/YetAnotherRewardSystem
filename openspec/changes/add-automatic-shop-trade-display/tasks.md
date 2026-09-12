## 1. Authoritative display state

- [x] 1.1 Create a feature branch for this change and inspect pinned vanilla/KubeJS update and registration hooks; verify the branch differs from `1.20` and record the concrete cross-loader lifecycle hooks before editing implementation. Branch: `feature/automatic-shop-trade-display`. Pinned hooks: block entity `getUpdateTag` / `getUpdatePacket` with `load` delivery on Fabric and Forge's default handlers; menu `clickMenuButton`; Forge `EntityRenderersEvent.RegisterRenderers`; Fabric's existing model-loading callback after KubeJS registry creation and before renderer creation. KubeJS `BlockEntityBuilder.createObject` assigns `BlockEntityInfo.entityType` during block-entity registry creation. See `verification.md` for the lifecycle issue found and corrected in real clients.
- [x] 1.2 Add the default-enabled persisted setting and copied transient display stacks to `AutomaticRewardBoxBlockEntity`; verify unit coverage for legacy missing-key default, explicit false/true round trips, one/two costs, metadata/count preservation, and empty/unresolvable selections.
- [x] 1.3 Implement minimal initial/update display serialization with client decoding separate from disk inventory loading; verify snapshot round trips, stale-stack clearing, absence of inventory/owner/history fields, and unchanged inventory after applying a display update.
- [x] 1.4 Refresh snapshots after selection, load, existing processing, and definition reload; verify stage advancement, exhaustion, removal/detachment, restoration, and suppression of unchanged updates without adding transaction triggers.
- [x] 1.5 Add scoped owner/trade history notifications and loaded-box lifecycle subscription; verify external automatic and interactive purchases refresh matching displays, unrelated owners/trades do not, removed boxes unsubscribe, reload restores subscriptions, and callbacks do not execute trades in other boxes.

## 2. Owner UI control

- [x] 2.1 Add menu data synchronization and validated native container-button handling in `AutomaticRewardBoxMenu`; verify valid owner toggles, rejected unknown buttons/non-owner/stale-menu/out-of-range requests, and unchanged selection, inventory, and purchase counts on toggling.
- [x] 2.2 Add the localized on/off button to `AutomaticRewardBoxScreen` using the existing language source/datagen convention, without editing generated resources; verify authoritative state after reopening, keyboard focus/narration, and no overlap with slots or offer scrolling at supported GUI scales.

## 3. Shared rendering and loader registration

- [x] 3.1 Implement the client-only automatic shop renderer with camera-facing item models, numeric counts including 1, optional second payment, and payment/result separators; verify a real client shows the correct two/three components, metadata-bearing items, readable multi-digit quantities, and no display when disabled/invalid.
- [x] 3.2 Register the renderer for deduplicated automatic-shop types via Fabric and Forge client lifecycles, exposing only minimal common type access; verify bundled, test-mod, and KubeJS-created types are covered on both loaders and remain covered after resource reload.

## 4. End-to-end verification

- [x] 4.1 Extend shared Testiarium client GameTests for initial tracking without a menu, offline-owner observation, synchronized toggling, shared progress, reload invalidation/restoration, and continued automation while hidden; verify assertions against actual client block-entity/menu state on both loaders rather than only server-side snapshots.
- [x] 4.2 Run `timeout 120s ./gradlew --no-daemon :core:test` and `timeout 180s xvfb-run -a ./gradlew --no-daemon :fabric:runClientGameTest :forge:runClientGameTest`, silently saving complete timestamped logs under `build/`; verify passing results and report command, exit code, duration, log paths, and relevant failure windows if any. The initial client deadline expired; an explicitly increased 300s deadline completed successfully. See `verification.md`.
- [x] 4.3 Perform and document real visual checks on both loaders: one/two payments, quantity 1 and multi-digit counts, metadata models, above-block placement, camera movement/culling, adjacent shops, GUI scaling, and resource reload; verify readable rendering and capture evidence outside tracked source directories.
- [x] 4.4 Smoke-test KubeJS-created shops and dedicated-server startup on Fabric and Forge using existing run-directory conventions and timed `:fabric:runServer` / `:forge:runServer` commands with `--no-daemon`; verify zero script errors, no client-class loading failures, correct display state on a connected observer, and saved disabled state after chunk reload/world restart. Save complete logs and stop all development processes after collecting evidence.
- [x] 4.5 Run `timeout 120s ./gradlew --no-daemon :core:build :fabric:build :forge:build` with complete output silently saved to a timestamped `build/` log; verify success, report command/exit/duration/log, and review the final diff for generated files, local scripts, logs, or unrelated changes before marking implementation complete.

Evidence, command results, resolved failures, and cleanup are recorded in `verification.md`.

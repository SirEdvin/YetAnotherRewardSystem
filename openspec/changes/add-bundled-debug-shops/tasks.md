## 1. Native debug shop registration

- [x] 1.1 Add shared native IDs/factories for `yars:debug_reward_shop` and `yars:debug_automatic_reward_box`, reusing existing blocks and the automatic block entity; verify no KubeJS or test-mod types enter these definitions and both loader compilation tasks pass.
- [x] 1.2 Register blocks, block items, the automatic block-entity type, and Functional Blocks Creative entries through Fabric/Forge entrypoints; verify actual registry lookups, Creative listing, `/give`, and player placement on both loaders without custom startup scripts.
- [x] 1.3 Trace pinned Broccolium owner assignment and connect native Fabric storage registration while reusing Forge capability handling; verify player-placed automatic shops have the correct owner, reject other players, and expose working real loader inventory adapters.

## 2. Native example catalog and lifecycle

- [x] 2.1 Implement the exact catalog table in design.md using shared native trade registration, copied vanilla stacks, shared attachments, and separate manual/automatic limited IDs; verify unit tests for each entry's initial offer, finite exhaustion, stage boundaries, NBT token source/exchange, attachment order, and shop-specific isolation.
- [x] 2.2 Implement bounded dynamic resolvers and verify resolved inputs/results against the documented formula over representative indices and integer-boundary values, with all counts within stack limits.
- [ ] 2.3 Inspect pinned KubeJS and loader server-start ordering, initialize the native baseline before script publication, and seed both loader event registrations before handlers; verify empty handlers, scripted coexistence, repeated reloads, and a second integrated world retain native offers without stale scripted entries or duplicates.
- [x] 2.4 Preserve all-or-nothing catalog replacement and history across invalid or conflicting scripted registration; verify unit tests and loader smoke tests show the error and retain the previous valid catalog and purchase counts.

## 3. Pixel-written assets and names

- [x] 3.1 Add `tools/generate_debug_shop_textures.py` using Python 3 stdlib pixel operations and deterministic PNG encoding; generate the planned wood/brass manual and metal/teal automatic assets, verify PNG dimensions and byte-identical regeneration, and visually inspect nearest-neighbor previews outside the repository.
- [ ] 3.2 Add production blockstates, cube block models, block-parent item models, and English names for both shops; verify every model/texture reference resolves, no survival recipe or world-generation entry was introduced, and both loader jars package these resources without depending on test or generated KubeJS assets.

## 4. Gameplay integration and manual guide

- [x] 4.1 Extend shared and native-item client tests to cover manual purchase and shift-click, shared progression in both directions, independent limited IDs, finite exhaustion, staged/dynamic offers, and NBT payment acceptance/rejection; verify through the relevant JUnit and both loader Testiarium suites rather than substitute test-only shops.
- [x] 4.2 Extend native automatic-shop integration coverage for selection, real hopper insertion/output extraction, insufficient and same-item combined costs, full-output blocking/resume, selection changes with retained inputs, owner rejection, and inventory drops on removal; verify exact item conservation and expected history on Fabric and Forge.
- [x] 4.3 Add or exercise world save/reopen coverage for automatic owner, selection, inventory, and player history, plus a second-player independence check; verify the reloaded native shop resumes the correct trade without resetting progress.
- [x] 4.4 Document acquisition commands, the full catalog table, token acquisition, Creative acquisition versus Survival transaction testing, fresh-world reset, and the spec's complete manual checklist; verify a script-free walkthrough can obtain every required payment and reproduce the advertised results without reading code.

## 5. End-to-end verification

- [ ] 5.1 Run `timeout 120s ./gradlew --no-daemon :core:test` and `timeout 180s xvfb-run -a ./gradlew --no-daemon :fabric:runClientGameTest :forge:runClientGameTest`, saving complete output silently to timestamped build logs; verify passing suites and report command, exit code, duration, log path, and relevant failures if any.
- [x] 5.2 Run `timeout 90s ./gradlew --no-daemon :fabric:runServer` and the corresponding `:forge:runServer` first without custom scripts, then with temporary coexistence/reload smoke fixtures; verify baseline offers, successful normal reloads, intentional collision rejection with retained catalog, and zero unexpected script errors, restoring local scripts and stopping servers afterward.
- [ ] 5.3 Inspect both shops in-world and in inventory on both clients, exercise resource reload, and check optional Jade offer display where installed; verify readable names, distinguishable pixel motifs, and no missing-model/texture warnings, keeping screenshots outside the repo and stopping clients afterward.
- [ ] 5.4 Run `timeout 120s ./gradlew --no-daemon :core:build :fabric:build :forge:build` with complete output silently saved to a timestamped build log, inspect packaged assets, and run `git diff --check`; verify build success and report command, exit code, duration, log path, and any remaining acceptance blockers without marking unexecuted checks complete.

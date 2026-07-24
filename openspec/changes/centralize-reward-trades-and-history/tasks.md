## 1. Baseline and Trade Registry

- [x] 1.1 Integrate the `feat/automatic-reward-box` baseline into the implementation branch, preserving its internal inventory, selection UI, loader item bridges, and change-driven execution behavior.
- [x] 1.2 Change trade and resolved-trade identities from shop-local strings to namespaced resource locations, then update unit tests for ID validation and stage resolution.
- [x] 1.3 Replace the shop-to-built-trades registry with one atomic snapshot of global definitions and ordered box attachments, including lookup and attachment revalidation APIs.
- [x] 1.4 Replace nested KubeJS shop registration with global `trade` and separate `attach` calls, validating duplicate definitions, duplicate attachments, unknown trades, and invalid box targets before replacement.
- [x] 1.5 Add unit tests proving reusable definitions, attachment ordering, shared attachments across box types, atomic failed reloads, and stale detached-trade rejection.

## 2. Server-Wide Player History

- [x] 2.1 Implement versioned server-wide SavedData keyed by canonical player UUID and namespaced trade ID, with validated reads, saturating increments, and dirty marking through overworld data storage.
- [x] 2.2 Add focused persistence tests for UUID isolation, shared trade counts, malformed values, saturation, save/load behavior, and ignoring legacy NBT shapes.
- [x] 2.3 Refactor interactive reward-shop merchants to read and increment history by server level, interacting-player UUID, and global trade ID while revalidating box attachment before completion.
- [x] 2.4 Remove player persistent-data configuration and loader-specific KubeJS entity-NBT access that is no longer used.

## 3. Automatic Box Ownership and Progress

- [x] 3.1 Make the automatic box use Broccolium's ownership-aware placement contract and implement `IOwnedBlockEntity` with a persisted owner UUID and optional online-player resolution.
- [x] 3.2 Enforce owner-only automatic-box UI opening and selection changes while leaving existing hopper, Fabric storage, and Forge capability access unchanged.
- [x] 3.3 Remove block-local completion counts and resolve every selected automatic transaction from the owner's latest server-wide count, including when the owner is offline.
- [x] 3.4 Revalidate the selected namespaced trade and its attachment on selection, processing, and registry reload while retaining invalidated selection IDs and inventory.
- [x] 3.5 Extend automatic-box unit tests for ownership persistence, non-owner denial, ownerless inactivity, offline execution, cross-box shared stages and limits, and atomic inventory/history updates.

## 4. Scripts, Documentation, and Game Coverage

- [x] 4.1 Update testmod KubeJS registrations and documentation to define namespaced trades once and attach them separately to interactive and automatic boxes.
- [x] 4.2 Extend shared client GameTests to cover owner-only UI access, trade selection, interactive-to-automatic shared progress, and automatic execution for an offline owner.
- [x] 4.3 Verify Fabric transfer storage and Forge item capability still accept selected payments, reject unrelated inputs, expose only outputs for extraction, and behave identically for simulation and commit.

## 5. Verification

- [x] 5.1 Run `:core:test` and fix all shared trade, history, merchant, and automatic-box failures.
- [x] 5.2 Run both Fabric and Forge client GameTests under `xvfb-run` with an explicit timeout and fix loader-specific behavior.
- [x] 5.3 Run Fabric and Forge server smoke tests with the new KubeJS declaration/attachment API and verify zero script errors.
- [x] 5.4 Run the timed `:core:build :fabric:build :forge:build` command and confirm the multi-loader build succeeds.

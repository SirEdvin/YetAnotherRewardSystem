## Purpose

Provide ready-to-place, mod-bundled manual and automatic debug shops with deterministic example trades and pixel-authored assets, allowing repeatable manual testing without user-authored KubeJS scripts.

## ADDED Requirements

### Requirement: Native bundled shops and Creative acquisition
The mod SHALL ship `yars:debug_reward_shop` and `yars:debug_automatic_reward_box` as usable blocks and block items on Fabric and Forge. They MUST exist without startup/server scripts or the test mod, appear in Creative inventory, and be obtainable via `/give`. They SHALL have no survival recipe or world-generation source. Acquisition SHALL NOT impose a Creative-only interaction restriction on placed shops.

#### Scenario: Ordinary installation
- **WHEN** a player launches either loader with the mod's required dependencies and no custom scripts or test mod
- **THEN** both named items are available in Creative inventory and `/give`, can be placed, and open their respective populated interfaces

#### Scenario: Survival playtesting
- **WHEN** a player places the shops in Creative and switches to Survival
- **THEN** the manual shop accepts normal paid purchases and the automatic shop retains its normal owner access and item-automation behavior
- **AND** neither shop has a survival crafting recipe

### Requirement: Predictable example trade coverage
The shops SHALL expose a deterministic, documented catalog using vanilla items. The combined catalog MUST include unlimited single-cost trades, two distinct costs, two costs using the same item, finite exhaustion, finite-to-unlimited staged progression, bounded purchase-index-dependent costs and results, and an NBT-sensitive payment with a way to obtain the matching stack. Limits and stages SHALL be small enough to exercise manually. All resolved stacks MUST remain within valid item stack limits.

#### Scenario: Normal and exceptional payments
- **WHEN** a tester supplies a single-cost or two-cost example with complete matching payment
- **THEN** the advertised output is produced and exactly the advertised payment is consumed
- **AND** missing, insufficient, or NBT-mismatched payment produces no transaction

#### Scenario: Observable progression
- **WHEN** a tester follows the documented finite, staged, and dynamic example sequences from fresh player history
- **THEN** the finite trade exhausts at its documented limit, the staged trade reaches its documented unlimited final stage, and dynamic payment and output match the documented purchase-index sequence

#### Scenario: Same-item costs
- **WHEN** the automatic shop is supplied with fewer items than the combined quantity of its same-item two-cost example
- **THEN** it produces no output and consumes nothing
- **AND** supplying the full combined quantity permits the trade

### Requirement: Shared and independent history examples
At least one limited or staged trade SHALL use the same trade ID in both shops to demonstrate shared per-player progression. Each shop SHALL also have a shop-specific trade with a distinct ID to demonstrate independent progression. Automatic purchases SHALL use the owner's history, not a new block-local counter; existing ownership and multiplayer isolation MUST remain enforced.

#### Scenario: Cross-shop progression
- **WHEN** a player completes a shared trade in the manual shop and then inspects that trade in their automatic shop
- **THEN** the automatic shop resolves the next offer using that player's updated history
- **AND** an automatic completion likewise advances the same trade when the manual shop is reopened

#### Scenario: Independent fixtures and players
- **WHEN** one player advances the manual-only example
- **THEN** the automatic-only example remains at its own current purchase count
- **AND** another player's shared trade history remains unchanged

### Requirement: Reliable built-in catalog lifecycle
Bundled trades SHALL be present at server startup and after successful reload, including reloads without trade event handlers. Successful scripted registration SHALL coexist with bundled trades. A conflicting bundled trade ID or invalid registration SHALL reject the candidate catalog rather than silently overriding a built-in trade or partially publishing changes. Existing history SHALL NOT reset on reload or restart.

#### Scenario: Empty and scripted reloads
- **WHEN** the server reloads with no trade handlers, or with valid unrelated scripted trades
- **THEN** both debug shops retain their built-in offers, and valid scripted trades remain available at their attached targets
- **AND** repeated reloads do not duplicate offers

#### Scenario: Failed reload
- **WHEN** a script attempts to define an existing bundled trade ID or submits an invalid registration
- **THEN** the error is reported and the previous valid catalog remains usable without losing history

#### Scenario: Persistent playtest
- **WHEN** a world is saved and reopened after purchases and automatic-shop selection
- **THEN** player progression and the automatic shop's owner, selection, and stored inventory are retained under existing persistence rules

### Requirement: Pixel-authored packaged presentation
Both shops SHALL ship complete blockstate, block-model, item-model, and English naming resources. Their distinct 16×16 PNG textures MUST be authored through explicit pixel writing, not generative-image tools, and SHALL be reproducible from a checked-in local tool. They SHALL be visually distinguishable by motif as well as color in-world and in inventory, with no dependency on KubeJS-generated assets or test resources.

#### Scenario: Resource loading
- **WHEN** either packaged loader build is used and resources are reloaded
- **THEN** both placed blocks and inventory items render with their own textures and readable names without missing-model or missing-texture warnings

#### Scenario: Reproducible pixel assets
- **WHEN** the documented texture tool is run with its supported local runtime
- **THEN** it reproduces the shipped PNG files byte-for-byte without a network call or image-generation model

### Requirement: Complete manual testing guide
The mod SHALL document acquisition commands, every example's trade ID, inputs, outputs, limits and stages, shared versus independent history, matching NBT fixture acquisition, and a fresh-world reset procedure. It MUST provide a checklist for manual purchases, shift-click, automatic selection and hopper insertion/extraction, insufficient inputs, full-output blocking/resume, ownership, persistence, reload, and optional Jade display.

#### Scenario: Script-free manual test session
- **WHEN** a tester follows the guide in a fresh Creative world with no custom scripts
- **THEN** they can obtain both shops and all example payments, test each documented trade shape, and identify the expected results without reading implementation code
- **AND** the guide distinguishes optional integration checks and commands requiring operator permissions

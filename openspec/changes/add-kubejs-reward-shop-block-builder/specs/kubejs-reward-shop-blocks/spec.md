## ADDED Requirements

### Requirement: Startup-script reward-shop block creation
The system SHALL register a KubeJS block type named `yars:reward_shop` that creates a functional reward-shop block in `startup_scripts`. The created block's resource location SHALL be its shop ID, and the builder SHALL retain the standard KubeJS block customization surface for pack-defined presentation and properties.

#### Scenario: Create a custom reward shop
- **WHEN** a startup script creates `kubejs:daily_rewards` with block type `yars:reward_shop`
- **THEN** the game registers `kubejs:daily_rewards` as a placeable block that opens the reward-shop merchant interface

#### Scenario: Customize a shop block
- **WHEN** a startup script applies supported KubeJS block-builder properties and assets to a reward-shop block
- **THEN** the registered block uses those pack-defined properties and assets without requiring YARS-provided block resources

### Requirement: No mod-provided blocks
YARS SHALL NOT register or ship a built-in reward-shop block or other gameplay block. YARS SHALL NOT retain block-specific assets, loot, recipes, translations, generated data, or creative-tab content for the removed `yars:reward_shop` block.

#### Scenario: Start without custom block scripts
- **WHEN** YARS starts in an instance whose startup scripts create no reward-shop blocks
- **THEN** YARS contributes no blocks to the block registry

#### Scenario: Removed built-in identifier
- **WHEN** an updated instance queries the block registry for `yars:reward_shop`
- **THEN** no YARS-provided block is registered under that identifier

### Requirement: Shop-scoped server-script trades
The system SHALL extend `RewardShopEvents.trades` with a nested `event.shop(shopId, callback)` registration API that associates every reward-shop trade created in the callback with that reward-shop block ID. Trade IDs SHALL remain stable plain strings, SHALL be unique within their shop, MAY be reused by different shops, and SHALL retain fixed, limited, staged, and dynamic stack behavior. The previous unscoped `event.trade(...)` API SHALL NOT remain available.

#### Scenario: Populate a created shop
- **WHEN** a server script registers a diamond trade for `kubejs:daily_rewards`
- **THEN** interacting with `kubejs:daily_rewards` presents that trade when it is available to the player

#### Scenario: Keep shop inventories independent
- **WHEN** two server-script shop registrations contain different trades
- **THEN** each reward-shop block presents only the trades registered for its own block ID

#### Scenario: Reuse a trade ID in separate shops
- **WHEN** two different shops each register a trade named `daily_diamond`
- **THEN** both registrations succeed and each trade resolves independently

#### Scenario: Merge repeated shop declarations
- **WHEN** multiple callbacks or script files declare the same shop ID with different trade IDs
- **THEN** the system merges those trades in event execution order

#### Scenario: Reject duplicate trade within one shop
- **WHEN** one shop registers `daily_diamond` more than once in the same registration cycle
- **THEN** the complete registration fails and reports both the shop ID and trade ID

#### Scenario: Reject legacy global registration
- **WHEN** a server script attempts to use the previous unscoped trade-registration form
- **THEN** no global or default-shop trade is registered

### Requirement: Shop registration validation
The system SHALL validate shop identifiers supplied by server scripts. An absent block ID or an ID that does not identify a registered reward-shop block SHALL fail the complete registration with a clear script error naming the target. A reward-shop block with no current trade registration SHALL remain usable and open an empty merchant interface.

#### Scenario: Target a non-shop block
- **WHEN** a server script registers trades against an ID that is absent or is not a reward-shop block
- **THEN** the complete registration fails with a script error containing the target block ID

#### Scenario: Open an unpopulated shop
- **WHEN** a player interacts with a registered reward-shop block that has no server-script trades
- **THEN** the merchant interface opens with no offers

### Requirement: Reload-safe shop replacement
The system SHALL replace the complete shop-to-trades registry atomically after a successful server-script reload. A failed registration SHALL leave the previous valid registry active. An open merchant SHALL revalidate its shop and trade against the current registry before completing a transaction.

#### Scenario: Replace trades on reload
- **WHEN** a server-script reload removes one trade and adds another for an existing shop
- **THEN** newly opened shop menus expose only the newly registered trade set

#### Scenario: Preserve trades after failed reload
- **WHEN** a server-script reload fails because any shop or trade declaration is invalid
- **THEN** the complete previously valid shop-to-trades registry remains active

#### Scenario: Reject stale offer after reload
- **WHEN** a player attempts to complete an offer that was removed from its shop by a server-script reload
- **THEN** the system grants no result and records no completed purchase

### Requirement: Shop-scoped player history
The system SHALL persist completed purchases separately by player, shop ID, and trade ID at `yars.reward_shop.v2.shops[<shop-id>].trades[<trade-id>]`. Shop IDs and trade IDs SHALL be accessed as full direct compound keys without splitting punctuation into path components. Missing, malformed, negative, and legacy unscoped values SHALL NOT grant progress in a custom shop.

#### Scenario: Same trade ID has independent limits
- **WHEN** a player completes `daily_diamond` in one shop and another shop uses the same trade ID
- **THEN** the second shop resolves its trade from that player's count for the second shop only

#### Scenario: Shop progress survives reconnect
- **WHEN** a player completes a trade in a custom shop and later reconnects
- **THEN** that shop resolves the next offer from the persisted count for its shop ID and trade ID

#### Scenario: Legacy global count is not assigned automatically
- **WHEN** a player has an old unscoped count for a trade ID and opens a custom shop using that trade ID
- **THEN** the custom shop treats its shop-scoped count as zero unless new-format history exists

### Requirement: Custom shop merchant title
The system SHALL use the interacted reward-shop block's display name as the title of its vanilla merchant interface.

#### Scenario: Display a customized shop name
- **WHEN** a startup script gives a reward-shop block a custom display name and a player interacts with it
- **THEN** the merchant interface title shows that custom block display name

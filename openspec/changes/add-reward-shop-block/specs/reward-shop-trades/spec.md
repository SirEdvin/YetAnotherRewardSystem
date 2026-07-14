## ADDED Requirements

### Requirement: Reward-shop block access
The system SHALL provide a reward-shop block that opens a merchant interface using the vanilla wandering trader presentation when a player interacts with it. A newly installed system with no KubeJS trade registrations SHALL expose no trade offers.

#### Scenario: Empty default shop
- **WHEN** a player opens a reward-shop block before any trade is registered
- **THEN** the wandering trader-style merchant interface shows no offers

#### Scenario: Configured shop opens
- **WHEN** a player opens a reward-shop block after at least one available trade is registered
- **THEN** the merchant interface presents that player's available reward-shop offers

### Requirement: KubeJS trade registration
The system SHALL provide a KubeJS server-side registration API for reward-shop trades. Each trade SHALL have a stable unique ID, an offered item stack, and one or two item-stack payment costs for each purchase stage.

#### Scenario: Register a fixed trade
- **WHEN** a KubeJS script registers a trade with a stable ID, one diamond result, and one emerald cost
- **THEN** a player can receive a trade offer for one diamond costing one emerald

#### Scenario: Reject duplicate IDs
- **WHEN** KubeJS registration defines more than one reward-shop trade with the same ID
- **THEN** the system rejects the duplicate registration and reports the conflicting ID to the pack author

### Requirement: Per-player purchase limits
The system SHALL track completed transactions separately for every player and trade ID. A trade without a configured limit SHALL remain available after any number of completed transactions. A trade with a configured limit SHALL no longer be offered to a player once that player has completed the limit.

#### Scenario: Capped trade is exhausted for one player
- **WHEN** a player completes a trade limited to four purchases four times
- **THEN** that trade is no longer offered to that player

#### Scenario: Capped trade remains available to another player
- **WHEN** one player exhausts a trade limited to four purchases
- **THEN** a second player who has not completed the trade can still receive the trade offer

### Requirement: Purchase-index-dependent trade stages
The system SHALL resolve a trade's offered stack and payment costs from that player's completed-purchase count before each transaction. KubeJS definitions SHALL support fixed stacks, staged changes, and a pack-author-supplied purchase-index-dependent stack resolution so that prices or results can change after each purchase.

#### Scenario: Doubling price progression
- **WHEN** a player views a three-purchase diamond trade configured with emerald costs of one, two, and four for purchase indexes zero, one, and two
- **THEN** the player sees the matching cost for their next uncompleted purchase

#### Scenario: Item-type price progression
- **WHEN** a player completes a diamond trade whose first stage costs one emerald and whose next stage costs sixty-four iron ingots
- **THEN** the next offer requires sixty-four iron ingots

### Requirement: Transaction integrity
The system SHALL validate that a player can pay the currently resolved cost before granting the currently resolved result. On a successful transaction, it SHALL atomically record exactly one completed transaction for that player and trade ID before refreshing the player's offers. An exhausted or stale offer SHALL not grant an item or increment a count.

#### Scenario: Successful purchase records progress
- **WHEN** a player pays the currently displayed cost for an available trade
- **THEN** the player receives the currently resolved result and their completed count for that trade increases by one

#### Scenario: Stale exhausted offer is rejected
- **WHEN** a player submits an offer after their current completed count has reached that trade's limit
- **THEN** the system grants no result and does not increase the completed count

### Requirement: Player-NBT trade history
The system SHALL persist each completed reward-shop trade count in player NBT under a YARS-owned, documented namespace keyed by stable trade ID. Missing, malformed, or negative stored counts SHALL be treated as zero when resolving trades.

#### Scenario: Progress persists across reconnect
- **WHEN** a player completes a reward-shop trade and later reconnects
- **THEN** the next offer resolves using the player's persisted completed-trade count

#### Scenario: UI reads trade history
- **WHEN** an integration reads the documented YARS player-NBT trade-history namespace for a player who completed a trade
- **THEN** it can obtain that trade's completed count by its stable trade ID

### Requirement: Texture provenance
The system SHALL ship the reward-shop block texture only with a recorded source, author, license, and any required attribution that permits redistribution with the mod.

#### Scenario: Asset provenance is documented
- **WHEN** the reward-shop texture is included in a release
- **THEN** the repository documents its source repository path and license information

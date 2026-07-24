## ADDED Requirements

### Requirement: KubeJS automatic reward-box creation
The system SHALL register a KubeJS startup-script block type named `yars:automatic_reward_box`. A block created with this type SHALL use its block resource location as its shop ID, SHALL retain standard KubeJS block presentation customization, and SHALL be a valid target for `RewardShopEvents.trades` shop registration. YARS SHALL NOT provide a built-in automatic reward box or its gameplay assets.

#### Scenario: Create an automatic reward box
- **WHEN** a startup script creates `kubejs:automated_rewards` with type `yars:automatic_reward_box`
- **THEN** the game registers a placeable automatic reward box whose shop ID is `kubejs:automated_rewards`

#### Scenario: Register trades for the box
- **WHEN** a server script registers trades for the created automatic reward-box ID
- **THEN** those trades are available for selection at that box

#### Scenario: Start without a box script
- **WHEN** no startup script creates an automatic reward box
- **THEN** YARS registers no automatic reward-box gameplay block

### Requirement: Player-selected automatic trade
The system SHALL open a server-authoritative trade-selection interface when a player uses an automatic reward box. The interface SHALL use the block display name, SHALL show the box's currently resolvable shop trades, and SHALL let the player select exactly one trade by stable trade ID without accepting payment or granting a result through the interface.

#### Scenario: Select a trade
- **WHEN** a player selects an available offer in the box interface
- **THEN** the box persists that offer's trade ID as its active automatic trade and grants no result

#### Scenario: Replace the selection
- **WHEN** a player selects another available offer
- **THEN** the new trade replaces the prior selection without resetting either trade's stored completion count

#### Scenario: Reject a stale selection
- **WHEN** a client submits an offer that is absent or no longer resolves for the box
- **THEN** the server leaves the active selection unchanged and grants no item

#### Scenario: Open an unpopulated box
- **WHEN** a player opens an automatic reward box with no registered trades
- **THEN** the selection interface opens with no offers

### Requirement: Persistent box inventory and progress
Each placed automatic reward box SHALL persist two payment slots, its output inventory, its selected trade ID, and non-negative completed purchase counts keyed by full trade ID. The box SHALL preserve this state across chunk unloads and world restarts, SHALL let players recover stored items, and SHALL drop stored items when broken.

#### Scenario: Reload a populated box
- **WHEN** a box containing payments, outputs, a selection, and trade progress is saved and loaded
- **THEN** all valid state is restored without changing item quantities or trade progress

#### Scenario: Keep progress per trade
- **WHEN** a box progresses one staged trade, selects and progresses another, and then reselects the first
- **THEN** each trade resolves from its own block-local completed count

#### Scenario: Recover contents after selection change
- **WHEN** stored payment items do not match a newly selected trade
- **THEN** the items remain stored and available to the player without being consumed

#### Scenario: Break a populated box
- **WHEN** a player breaks an automatic reward box containing payment or output items
- **THEN** the box drops those contents without duplication

### Requirement: Automatic atomic trade execution
On the logical server, the box SHALL recheck its selected trade after an item is inserted, a selection changes, valid state loads, or registered trades are replaced. It SHALL repeatedly perform the selected trade while exact resolved costs are available and the complete result fits in output storage. Each performance SHALL atomically insert the result, consume the costs, increment that trade's block-local completion count, and resolve the next stage.

#### Scenario: Perform a one-cost trade on insertion
- **WHEN** insertion makes the selected trade's first cost available and the result fits
- **THEN** the box consumes that cost, stores the result, and increments the selected trade count once

#### Scenario: Require both costs
- **WHEN** a selected two-cost trade has only one complete cost available
- **THEN** the box consumes nothing and produces nothing

#### Scenario: Drain multiple available trades
- **WHEN** the payment slots contain enough items for multiple performances and all results fit
- **THEN** the box performs trades until a cost, output capacity, or trade availability prevents the next performance

#### Scenario: Preserve items when output is full
- **WHEN** the complete next result cannot fit in output storage
- **THEN** the box consumes no cost and does not increment progress for that performance

#### Scenario: Advance a staged trade
- **WHEN** a performance reaches the purchase limit of the selected trade's current stage
- **THEN** the next performance uses the costs and result resolved from the following stage

#### Scenario: Exhaust a limited trade
- **WHEN** the selected trade has no stage available at its completed count
- **THEN** the box stops processing and preserves all remaining items

#### Scenario: Handle invalid dynamic resolution
- **WHEN** resolving the selected dynamic trade throws or returns invalid stacks
- **THEN** the box stops processing that trade without consuming, producing, or incrementing anything

### Requirement: Standard item automation
The automatic reward box SHALL expose standard item automation on Forge and Fabric. Automation SHALL insert only into payment slots, SHALL accept only items matching a currently resolved cost of the selected trade, SHALL extract only from output slots, and SHALL enforce the same simulation and commit result on both loaders.

#### Scenario: Insert an accepted payment
- **WHEN** automation inserts an item matching either resolved cost of the selected trade
- **THEN** the box accepts as much as payment-slot capacity permits and checks for executable trades

#### Scenario: Reject unrelated insertion
- **WHEN** automation inserts an item that matches neither resolved cost or no valid trade is selected
- **THEN** the box accepts none of that item

#### Scenario: Extract produced items
- **WHEN** automation requests an item from output storage
- **THEN** the box permits extraction without exposing payment slots for extraction

#### Scenario: Simulate an operation
- **WHEN** a loader item API simulates insertion or extraction
- **THEN** the box reports the same transferable quantity as commit would without mutating inventory or progress

### Requirement: Reload-safe trade selection
Successful server-script trade replacement SHALL cause automatic reward boxes to use the new definitions. A removed, exhausted, or invalid selected trade SHALL perform no transaction and SHALL retain its selection ID, inventory, and progress so that a later valid definition with the same ID can resume.

#### Scenario: Update selected trade costs on reload
- **WHEN** a reload changes the selected trade's resolved costs
- **THEN** subsequent insertion and execution use only the new costs

#### Scenario: Remove the selected trade
- **WHEN** a reload removes the selected trade while payments are stored
- **THEN** the box consumes nothing and retains its selection, inventory, and progress

#### Scenario: Restore the selected trade
- **WHEN** a later reload restores a valid trade with the retained ID
- **THEN** the box resolves it from the retained block-local completion count and checks whether it can execute

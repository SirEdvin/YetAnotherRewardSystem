## ADDED Requirements

### Requirement: Server-wide player trade history
The system SHALL persist completed reward-trade counts in one server-wide SavedData record keyed by player UUID and namespaced trade ID. The same record SHALL be used from every dimension and SHALL be readable and writable without loading the identified player entity.

#### Scenario: Read an offline player's history
- **WHEN** a loaded automatic reward box resolves a trade for an owner who is offline
- **THEN** the system obtains that owner's completed count from level SavedData without requiring a player entity

#### Scenario: Share history across dimensions
- **WHEN** the same player's trade is accessed from boxes in different dimensions
- **THEN** both boxes resolve the trade from the same server-wide completed count

### Requirement: Trade identity defines shared progress
The system SHALL track one count for each player UUID and namespaced trade ID, independent of box ID, box position, dimension, and interactive or automatic box type. Every successful transaction of that trade for that player SHALL advance the same count.

#### Scenario: Share progress between box types
- **WHEN** a player completes a trade in an interactive box and an owned automatic box selects the same attached trade
- **THEN** the automatic box resolves the next transaction from the count advanced by the interactive purchase

#### Scenario: Share a limit between boxes
- **WHEN** transactions across multiple boxes exhaust a trade for one player
- **THEN** that trade is unavailable to that player in every box where it is attached

### Requirement: Validated and durable counts
The system SHALL treat missing, malformed, and negative persisted counts as zero, SHALL prevent integer overflow, and SHALL mark level SavedData dirty after a successful increment. Legacy player-NBT and automatic-box-local counts SHALL be ignored.

#### Scenario: Read malformed history
- **WHEN** a stored count is absent, not an integer, or negative
- **THEN** the trade resolves as if the player has completed it zero times

#### Scenario: Saturate the maximum count
- **WHEN** a successful transaction increments a count already at the supported integer maximum
- **THEN** the stored count remains at that maximum instead of overflowing

#### Scenario: Ignore unreleased legacy state
- **WHEN** old player NBT or automatic-box-local completion data exists
- **THEN** it does not contribute to the server-wide count

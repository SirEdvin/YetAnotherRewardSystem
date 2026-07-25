## MODIFIED Requirements

### Requirement: Shop-scoped server-script trades
The system SHALL extend `RewardShopEvents.trades` with global `event.trade(tradeId)` declarations and separate `event.attach(boxId, tradeId)` calls. Trade IDs SHALL be namespaced resource locations, SHALL be globally unique within one successful registration cycle, and SHALL retain fixed, limited, staged, and dynamic stack behavior. Each valid trade MAY be attached to any number of interactive or automatic reward-box block IDs, and each box SHALL present its attached trades in attachment declaration order. The previous nested `event.shop(...).trade(...)` form SHALL NOT remain available.

#### Scenario: Define and attach a reusable trade
- **WHEN** a server script defines `pack:daily_diamond` once and attaches it to an interactive box and an automatic box
- **THEN** both boxes expose the same trade definition under the same persisted identity

#### Scenario: Attach different trades to boxes
- **WHEN** two boxes receive different attachment declarations
- **THEN** each box presents only its own attached trades

#### Scenario: Reject a duplicate global trade
- **WHEN** registration defines the same namespaced trade ID more than once
- **THEN** the complete registration fails and reports the duplicate trade ID

#### Scenario: Reject a duplicate attachment
- **WHEN** registration attaches the same trade ID to the same box ID more than once
- **THEN** the complete registration fails and reports both IDs

#### Scenario: Reject legacy nested registration
- **WHEN** a server script attempts to define a trade through the previous `event.shop(...).trade(...)` form
- **THEN** no trade or attachment is registered through that form

### Requirement: Shop registration validation
The system SHALL validate every global trade declaration and box attachment before replacing the active registry. An invalid trade ID, invalid box ID, unknown attached trade ID, absent block, or block that is not an interactive or automatic reward box SHALL fail the complete registration with a clear script error naming the invalid target. A reward box with no attachments SHALL remain usable and expose no offers.

#### Scenario: Attach an unknown trade
- **WHEN** a server script attaches a trade ID that has no global definition in that registration cycle
- **THEN** the complete registration fails with an error containing the trade ID

#### Scenario: Target a non-box block
- **WHEN** a server script attaches a trade to an absent block or a block that is not a reward box
- **THEN** the complete registration fails with an error containing the box ID

#### Scenario: Open an unattached box
- **WHEN** a player interacts with a registered reward box that has no trade attachments
- **THEN** its trade or selection interface opens with no offers

### Requirement: Reload-safe shop replacement
The system SHALL replace the complete global definitions-and-attachments registry atomically after a successful server-script reload. A failed registration SHALL leave the previous valid snapshot active. An open merchant and an automatic box SHALL revalidate both trade definition and attachment before selecting or completing a transaction.

#### Scenario: Replace definitions and attachments on reload
- **WHEN** a successful reload changes a trade definition or moves its attachment between boxes
- **THEN** subsequent resolutions use only the new definition and attachment snapshot

#### Scenario: Preserve registry after failed reload
- **WHEN** any global definition or attachment fails validation during reload
- **THEN** the complete previously valid snapshot remains active

#### Scenario: Reject a stale detached offer
- **WHEN** a player attempts to complete or select an offer after its trade was detached from that box
- **THEN** the system grants no result, changes no selection, and records no completed purchase

## REMOVED Requirements

### Requirement: Shop-scoped player history
**Reason**: Reusable trades share one player count across all attached interactive and automatic boxes, so shop ID is no longer part of progression identity.

**Migration**: No migration is provided because the old format has not been released. New transactions use server-wide player UUID and namespaced trade ID history.

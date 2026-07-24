## ADDED Requirements

### Requirement: Persistent automatic-box ownership
Each automatic reward box SHALL bind to the UUID of the player who places it and SHALL persist that UUID in its block entity. Only the owner SHALL be allowed to open its UI or change its selected trade. Item automation SHALL retain its existing insertion and extraction access regardless of whether the owner is online.

#### Scenario: Bind the placing player
- **WHEN** a player places an automatic reward box
- **THEN** the block entity persists that player's UUID as its owner

#### Scenario: Owner opens the box
- **WHEN** the owner interacts with their automatic reward box within normal use range
- **THEN** the trade-selection and inventory UI opens

#### Scenario: Reject another player
- **WHEN** a player whose UUID differs from the stored owner interacts with the box
- **THEN** the UI does not open and the selected trade remains unchanged

#### Scenario: Preserve automation while owner is offline
- **WHEN** the owner is offline and item automation accesses the loaded box
- **THEN** accepted payments can be inserted and produced outputs can be extracted under the normal slot rules

#### Scenario: Leave a non-player-placed box inactive
- **WHEN** an automatic reward box has no valid owner UUID
- **THEN** no player can open it and it performs no trade

## MODIFIED Requirements

### Requirement: Player-selected automatic trade
The system SHALL open a server-authoritative trade-selection interface only for the automatic reward box's owner. The interface SHALL use the block display name, SHALL show the box's attached trades resolved from the owner's shared level history, and SHALL let the owner select exactly one trade by namespaced ID without accepting payment or granting a result through the interface.

#### Scenario: Owner selects a trade
- **WHEN** the owner selects an available attached offer in the box interface
- **THEN** the box persists that offer's namespaced trade ID as its active automatic trade and grants no result

#### Scenario: Replace the selection
- **WHEN** the owner selects another available attached trade
- **THEN** the new trade replaces the prior selection without changing shared completion history

#### Scenario: Reject a stale selection
- **WHEN** a client submits a trade that is absent, detached, or no longer resolves for the owner
- **THEN** the server leaves the active selection unchanged and grants no item

#### Scenario: Open an unattached box
- **WHEN** the owner opens an automatic reward box with no attached trades
- **THEN** the selection interface opens with no offers

### Requirement: Persistent box inventory and progress
Each placed automatic reward box SHALL persist its payment and output inventory, selected namespaced trade ID, and owner UUID across chunk unloads and world restarts. It SHALL NOT persist local completed-purchase counts. It SHALL let its owner recover stored items and SHALL drop stored items when broken.

#### Scenario: Reload a populated box
- **WHEN** an owned box containing payments, outputs, and a selection is saved and loaded
- **THEN** its inventory, selection, and owner UUID are restored without changing item quantities or shared level history

#### Scenario: Keep progress outside the box
- **WHEN** a selected box is broken and another box owned by the same player selects the same trade
- **THEN** the new box resolves from that player's existing server-wide count

#### Scenario: Recover contents after selection change
- **WHEN** stored payment items do not match a newly selected trade
- **THEN** the items remain stored and available to the owner without being consumed

#### Scenario: Break a populated box
- **WHEN** an automatic reward box containing payment or output items is broken
- **THEN** the box drops those contents without duplication

### Requirement: Automatic atomic trade execution
On the logical server, an owned automatic reward box SHALL recheck its selected attached trade after its existing processing triggers. It SHALL resolve that trade from the owner's latest server-wide completed count and repeatedly perform it while exact costs are available and the complete result fits in output storage. Each performance SHALL atomically insert the result, consume the costs, and increment the owner's shared trade count exactly once before resolving the next stage.

#### Scenario: Perform for an offline owner
- **WHEN** item insertion makes the selected trade executable while the owner is offline
- **THEN** the box consumes the costs, stores the result, and increments that owner's shared count once

#### Scenario: Re-read shared progress
- **WHEN** another box or interactive purchase advances the owner's selected trade
- **THEN** the automatic box resolves its next execution from the updated shared count

#### Scenario: Require complete transaction capacity
- **WHEN** a required cost is missing or the complete result cannot fit
- **THEN** the box consumes nothing, produces nothing, and does not increment history

#### Scenario: Stop an exhausted shared trade
- **WHEN** the owner's shared count exhausts the selected trade
- **THEN** the box preserves its inventory and performs no further transaction

#### Scenario: Handle invalid dynamic resolution
- **WHEN** resolving the selected dynamic trade throws or returns invalid stacks
- **THEN** the box stops processing without consuming, producing, or incrementing anything

### Requirement: Reload-safe trade selection
Successful server-script replacement SHALL cause automatic reward boxes to use the new global definitions and attachments. A removed, detached, exhausted, or invalid selected trade SHALL perform no transaction and SHALL retain its selection ID and inventory so that a later valid definition and attachment with the same ID can resume from the owner's shared history.

#### Scenario: Update selected trade on reload
- **WHEN** a reload changes the selected trade's resolved costs while retaining its attachment
- **THEN** subsequent insertion and execution use only the new costs

#### Scenario: Detach the selected trade
- **WHEN** a reload detaches the selected trade while payments are stored
- **THEN** the box consumes nothing and retains its selection and inventory

#### Scenario: Restore the selected trade
- **WHEN** a later reload restores a valid definition and attachment for the retained ID
- **THEN** the box resolves it from the owner's current shared count and checks whether it can execute

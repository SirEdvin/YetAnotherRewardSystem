## Purpose

Make an automatic shop's selected trade and exact quantities visible above the block, while allowing its owner to disable that display independently of trading.

## ADDED Requirements

### Requirement: Selected trade world display
An enabled automatic shop SHALL display the currently resolvable selected trade above the block for nearby observing players, without requiring its UI to be open. The display SHALL show each payment item followed by the result, with exact numeric quantities associated with each item, and SHALL distinguish payments from the result. Displayed item appearance SHALL preserve the resolved stack's item metadata. The display SHALL represent the next trade, not stored inventory or the last completed trade.

#### Scenario: One payment item
- **WHEN** a shop selects a trade requiring five emeralds for two diamonds
- **THEN** the display shows the emerald item with quantity 5 and the diamond item with quantity 2, distinguishing cost from result

#### Scenario: Two payment items
- **WHEN** the selected trade requires two payment stacks
- **THEN** both payment stacks and the result appear with their own exact quantities, including quantities of 1
- **AND** identical payment item types remain separate trade components rather than being silently merged

#### Scenario: Temporarily blocked execution
- **WHEN** the selected trade resolves but payments are missing or output storage is full
- **THEN** the display remains visible and represents the selected trade rather than inventory contents

#### Scenario: No resolvable selection
- **WHEN** the shop has no selected trade or owner, or its selected trade is removed, detached, exhausted, or fails resolution
- **THEN** no trade items or quantities are displayed
- **AND** existing inventory and retained selection behavior remain unchanged

### Requirement: Persistent owner-controlled visibility
Each automatic shop SHALL have an independent display-enabled setting that defaults to true for new shops and saves missing the setting. The owner's automatic shop UI SHALL provide a localized, keyboard-operable button identifying the current on/off state. The server SHALL accept display changes only through a valid open menu for that shop's owner within the existing interaction range. The setting SHALL persist across menu reopening, chunk unloading, and world restart.

#### Scenario: Default visibility
- **WHEN** a new shop or a shop saved before this feature loads with a resolvable selected trade
- **THEN** its display is enabled without requiring owner action

#### Scenario: Toggle and restore
- **WHEN** the owner disables the display using the UI button
- **THEN** the button reports off and the display disappears for observing players
- **AND** reopening the menu or reloading the world preserves the disabled state
- **WHEN** the owner enables it again
- **THEN** the button reports on and the current resolvable trade reappears

#### Scenario: Reject unauthorized changes
- **WHEN** a non-owner, an out-of-range player, or a player without a valid menu attempts a display change
- **THEN** the shop's display setting remains unchanged

#### Scenario: Independent presentation setting
- **WHEN** an owner toggles one shop's display
- **THEN** other shops' settings are unchanged
- **AND** no items are consumed or produced, no purchase history is advanced, and the selected trade is unchanged by that action
- **AND** automatic trading continues normally while the display is disabled

### Requirement: Authoritative and fresh display state
The display SHALL use server-resolved trade items and quantities based on the owner's shared purchase history. Observers SHALL receive current state on initial chunk tracking and subsequent relevant changes without reopening a menu or reconnecting. Clients SHALL NOT need server scripts or private inventory/history data to render the display.

#### Scenario: Selection and local progress
- **WHEN** the owner changes the selection or the shop completes purchases that change or exhaust its selected trade
- **THEN** observing clients update to the next resolved trade or hide the exhausted display

#### Scenario: Shared progress changes elsewhere
- **WHEN** another automatic shop or an interactive purchase advances the same owner's selected trade
- **THEN** the loaded shop's display updates to the new quantities or exhaustion state without requiring inventory interaction

#### Scenario: Reload changes and restores a trade
- **WHEN** successful script reload changes, removes, or detaches a selected trade
- **THEN** its display updates or clears to match the new authoritative state
- **WHEN** a later successful reload restores a valid attached trade with the retained ID
- **THEN** an enabled display reappears using current shared history

#### Scenario: New observer and offline owner
- **WHEN** a player begins observing a loaded shop while its owner is offline
- **THEN** the observer receives the shop's current display state without opening the owner-only UI

### Requirement: Cross-loader presentation safety
The feature SHALL work for bundled and KubeJS-created automatic shops on Fabric and Forge for Minecraft 1.20.1. Rendering SHALL be client-only, SHALL create no collectible item entities, and SHALL NOT alter inventories, trade execution, ownership, or history. Dedicated servers SHALL remain usable without client rendering classes.

#### Scenario: Automatic shop variants
- **WHEN** bundled and KubeJS-created automatic shops with valid selections are observed on either supported loader
- **THEN** each supports the same display and visibility control

#### Scenario: Dedicated server and visual-only items
- **WHEN** a dedicated server loads shops and players observe their displays from connected clients
- **THEN** the server runs without client-class loading failures and no display item can be collected or accessed by item automation

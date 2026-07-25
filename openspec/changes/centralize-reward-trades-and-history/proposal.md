## Why

Reward trade progress currently lives on online player entities and is scoped by shop block, so an automatic reward box cannot resolve or advance its owner's trades while that player is offline. Trade definitions are also nested under one shop, which prevents interactive and automatic boxes from sharing one stable trade and its progression.

## What Changes

- Store completed trade counts in server-wide level SavedData keyed by player UUID and globally unique trade ID, allowing offline-owner transactions in loaded automatic boxes.
- Define each reward trade once under a namespaced ID and attach it explicitly to any number of interactive or automatic box block IDs.
- Resolve one shared per-player count for a trade everywhere that trade is attached.
- Bind each automatic reward box to the player who places it by persisting the owner's UUID in its block entity.
- Restrict automatic-box UI access and trade selection to the owner while retaining its existing automation-accessible internal inventory and change-driven transaction loop.
- **BREAKING** Replace shop-nested trade declarations and shop-scoped plain trade IDs with global namespaced trade declarations plus explicit box-to-trade attachments.
- **BREAKING** Replace player persistent-NBT and automatic-box-local progress with server-wide UUID-keyed level data; unreleased legacy data is ignored rather than migrated.

## Capabilities

### New Capabilities
- `player-reward-trade-history`: Server-wide, dimension-independent reward trade progress keyed by player UUID and namespaced trade ID.

### Modified Capabilities
- `kubejs-reward-shop-blocks`: Replace box-owned trade definitions with globally reusable trade definitions and explicit attachments shared by interactive and automatic boxes.
- `automatic-reward-boxes`: Bind boxes to their placer, enforce owner-only UI access, and execute selected trades against the owner's shared history even while offline.

## Impact

- Affects shared trade registration, lookup, merchant resolution, transaction completion, automatic-box persistence and menus, KubeJS documentation, and Fabric/Forge tests.
- Introduces a shared Minecraft `SavedData` model accessed through server-wide overworld data storage.
- Uses Broccolium's existing `IOwnedBlockEntity` placement contract; no Broccolium library change is required unless implementation reveals a missing persistence primitive.
- Requires the existing `feat/automatic-reward-box` implementation as the automatic-box baseline.

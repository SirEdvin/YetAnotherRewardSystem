# Bundled debug shops

YARS ships a manual reward shop and an automatic reward box on Fabric and Forge. They are native mod blocks, not KubeJS-generated content. Install the normal mod dependencies, but no startup/server scripts or test mod are needed.

## Obtain and place

Find **Debug Reward Shop** and **Debug Automatic Reward Box** in Creative's Functional Blocks tab, or use these operator/cheats-enabled commands:

```mcfunction
/give @s yars:debug_reward_shop
/give @s yars:debug_automatic_reward_box
```

Place them yourself: the automatic box belongs to the player who places it. `/setblock` does not assign an owner. There are no survival crafting recipes or world-generation sources. Creative-only means acquisition, not interaction: switch to Survival after placing them to test real payment, inventory space, and shift-click behavior.

## Example trades

All IDs below have the `yars:` namespace. Each shop contains the shared entries plus its own limited apple trade. Inputs and outputs are vanilla items except for the paper token's NBT.

| Trade ID path | Payment | Result | Limit/progression |
| --- | --- | --- | --- |
| `debug/shared/basic` | 1 cobblestone | 1 emerald | Unlimited |
| `debug/shared/two_costs` | 1 emerald + 2 sticks | 4 torches | Unlimited |
| `debug/shared/limited` | 1 emerald | 1 diamond | 3 purchases |
| `debug/shared/staged` | 1 emerald, then 2, then 3 | 1 iron ingot, then gold ingot, then diamond | 2 iron purchases, 2 gold purchases, unlimited diamonds |
| `debug/shared/dynamic` | `1 + n % 4` emeralds | `1 + n % 3` redstone | Unlimited; `n` is completed purchases before buying |
| `debug/shared/same_item_costs` | 2 cobblestone + 3 cobblestone | 1 copper ingot | Unlimited; both costs must be paid |
| `debug/shared/token_source` | 1 paper | 1 Debug Reward Token | Unlimited |
| `debug/shared/token_exchange` | 1 Debug Reward Token | 1 amethyst shard | Unlimited; ordinary paper does not match |
| `debug/manual/limited` | 1 dirt | 1 apple | 2 purchases, manual shop only |
| `debug/automatic/limited` | 1 dirt | 1 apple | 2 purchases, automatic shop only |

The dynamic trade starts at 1 emerald → 1 redstone, then 2 → 2, then 3 → 3, then 4 → 1, then 1 → 2. Counts stay within normal stack sizes indefinitely.

Make tokens through `token_source`; do not rename ordinary paper and expect it to work. The matching stack has string NBT `yars_debug_token: "reward"` and the name `Debug Reward Token`. Use the source trade's actual output for the exchange.

Shared trade IDs share progress for the same player across both shops and every placed copy. Automatic purchases count for the box's owner, including when that owner is offline. Another player's progress is independent. The manual-only and automatic-only apple trades have identical recipes but independent IDs and purchase counts.

Reloading scripts, replacing a shop, or switching trades does not reset history. To repeat finite/staged tests from zero, create a fresh disposable world. Do not delete world data to reset a test.

## Script-free manual checklist

Start in a fresh Creative flat world. Obtain both blocks, cobblestone, emeralds, sticks, paper, dirt, hoppers, and chests from Creative. Put the shops side by side, then switch to Survival. Creative acquisition is convenient; Survival avoids masking inventory/payment problems.

- [ ] Open the manual shop. Confirm its name, textures, and offers. Pay for the cobblestone trade normally, then shift-click a result. Exactly the offered cost should be consumed.
- [ ] Try insufficient payment and the wrong item. No result or purchase progress should be granted. For the two-cost trade, supply just emeralds before adding sticks.
- [ ] Buy the shared limited diamond offer three times. It should disappear. Open your automatic box: the same shared offer should already be exhausted.
- [ ] Buy staged iron once manually, then select that same iron offer in your automatic box and supply one emerald. Reopen the manual shop: the next stage should be gold for two emeralds. Buy the gold stage twice, then verify the unlimited diamond stage.
- [ ] Repeat the dynamic trade and compare with the formula above. Its cost and output change with this player's purchase count, not with block placement.
- [ ] Exhaust the manual apple trade. The automatic apple trade should still allow its own two purchases.
- [ ] Produce a token from paper and exchange it for amethyst. Ordinary paper must not satisfy the token exchange.
- [ ] Open the automatic box, select a trade, and put matching items in its payment storage. The merchant list selects a trade; automatic outputs appear in the box's storage, not the ordinary merchant result slot. Recover outputs with normal clicks and shift-click.
- [ ] Select the same-item cobblestone trade. Supply only four cobblestone: no output. Supply a total of five: one copper ingot. Manual purchases require the two advertised payment slots.
- [ ] Feed a selected automatic trade with a hopper above; extract outputs with a hopper below into a chest. Confirm the full required payments are consumed and output extraction does not pull payment items.
- [ ] Fill all four automatic output slots using normal trades, then leave another valid payment waiting. It must not be consumed until enough space exists for the complete result. Extract output and confirm processing resumes.
- [ ] Leave an incomplete payment, switch to a trade with a different payment, and confirm old inputs remain recoverable rather than disappearing. New automation insertion must follow the selected trade's costs.
- [ ] Save and quit with an automatic selection, stored items, and some completed purchases. Reopen: ownership, selection, inventory, and history should remain. Resume trading.
- [ ] Have another player try opening your automatic box: access should be denied. Have that player place their own box and confirm their shared-trade progress is independent.
- [ ] Break a box containing items in a clear area and recover its inventory drops. No inputs or outputs should disappear or duplicate.
- [ ] Use operator `/reload`; reopen shops and confirm offers and progress remain, without duplicate entries. No user trade handler is required.
- [ ] Use the client's resource reload (F3+T). Check placed blocks, held items, inventory models, and English names for missing textures/models.
- [ ] Optional, when Jade is installed: look at the automatic box with no selection and with single/two-cost trades selected. Confirm the overlay's item icons, separators, and displayed costs/results.

## KubeJS coexistence

Custom scripts can continue registering trades and attaching them to either kind of shop. Each reload starts with the native examples, then adds script definitions. Built-in trade IDs cannot be overwritten: duplicate IDs, invalid registrations, or trade-handler failures retain the previous valid catalog and report the error in KubeJS logs. Removing a valid custom script removes its offers on the next successful reload, not the built-in examples. A failed reload does not erase purchase history.

## Rebuild the pixel textures

From the repository root:

```sh
python3 tools/generate_debug_shop_textures.py
python3 tools/generate_debug_shop_textures.py --preview /tmp/yars-debug-textures
```

The generator uses only Python 3's standard library and explicitly writes pixels. The six opaque 16×16 PNGs are committed under `projects/core/src/main/resources/assets/yars/textures/block/`; no image-generation service, runtime generator, or Python installation is required to play. Manual shops use brass coin motifs; automatic boxes use teal hopper/arrow motifs. Preview images are nearest-neighbor enlargements, not replacement game textures.

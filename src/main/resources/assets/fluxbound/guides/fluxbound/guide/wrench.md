---
navigation:
  title: Fluxbound Wrench
  position: 3
item_ids:
  - fluxbound:wrench
---

# Fluxbound Wrench

The Fluxbound Wrench is a reusable, durability-free tool for managing machines such as the <ItemLink id="fluxbound:arrival_anchor" />.

## Crafting

<RecipeFor id="fluxbound:wrench" />

## Controls

| Action | Function |
| --- | --- |
| Right-click an Anchor | Select its logical multiblock |
| Right-click another Anchor | Attempt to join the two structures; the selection is kept if the join fails |
| Right-click the selected machine again | Clear selection |
| Right-click in the air | Clear selection |
| Sneak + right-click an Anchor | Instantly pick up one physical block |

Selections are remembered by the individual wrench item. **A cyan highlight on the wrench indicates that a machine is selected.** The wrench shows brief action messages above the hotbar.

## Selecting and Joining Machines

Right-click an <ItemLink id="fluxbound:arrival_anchor" /> to select its logical machine, which can contain more than one block. Right-click a second machine to attempt a manual join.

Both machines must:

- Be horizontally adjacent.
- Have the **same owner** and **identical permission profiles**.
- Form one complete rectangle when joined.
- Fit within the current **8 x 8** maximum dimensions.

The player must have [Manage permission](permissions.md) on both machines.

Manual joining works even for Anchors deliberately separated through sneak-placement. A successful join combines the stored FE, enables normal automatic joining on the result, and clears the selection.

If a join fails, the selection is kept so you can try another machine. The selection is cleared if the selected machine no longer exists, for example after it was removed or restructured, or if you no longer have Manage permission on it.

Right-click in the air, or right-click the selected machine again, to cancel selection. The cyan wrench indicator turns off when the selection is cleared.

See [Multiblocks](arrival_anchor.md#multiblocks) for details on rectangular structures and shared energy.

## Picking Up Machine Blocks

Sneak-right-click an Anchor to instantly pick up **one physical block**. This works in both Survival and Creative Mode and does not use the block's normal loot drop.

The remaining placed structures retain as much FE as they can hold. Any excess FE is stored in the returned item, up to that item's capacity.

If the picked-up machine has no stored FE or meaningful configuration, the returned item is a normal stackable Arrival Anchor. Packed items preserve meaningful data, including nondefault permissions.

Sneak-placement is temporary: after pickup, placing the item normally allows automatic joining again.

The complete destruction and pickup rules are explained in [Breaking and Picking Up Machines](arrival_anchor.md#breaking-and-picking-up-machines).

## Permissions

Using the wrench requires **Manage** permission on the affected machine. Joining requires Manage permission on both machines, plus matching owners and permission profiles.

See [Machine Permissions](permissions.md) to configure access and trusted players.

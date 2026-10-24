---
navigation:
  title: Arrival Anchor
  position: 2
item_ids:
  - fluxbound:arrival_anchor
---

# Arrival Anchor

The Arrival Anchor is Fluxbound's first stationary machine: a destination that players arrive at, which cannot send anyone away by itself. It stores FE, combines with neighbouring Anchors into rectangular multiblocks, and belongs to an owner who decides who may use and manage it.

## Crafting

<RecipeFor id="fluxbound:arrival_anchor" />

The recipe uses a <ItemLink id="fluxbound:basic_teleportation_core" /> and a <ItemLink id="fluxbound:teleportation_chassis" />. Both are explained in [Crafting Components](components.md).

## Energy

Each physical Arrival Anchor contributes:

- **100,000 FE** of storage capacity.
- **200 FE/t** of maximum external input.
- **0 FE/t** of passive consumption.

Server operators and players can change these values in `config/fluxbound-common.toml`; changes require a game restart.

FE can enter through the **bottom and four horizontal sides**, but **not through the top**. The top is reserved for players, although this does not prevent other blocks from being physically placed above it.

You can supply energy using compatible external generators and cables. When the optional Jade mod is installed, looking at an Anchor shows its stored FE in a red energy bar.

## Multiblocks

Adjacent, compatible Arrival Anchors automatically form complete **horizontal rectangles**. The current maximum is **8 x 8 blocks**.

Every member of one multiblock shares:

- One logical machine identity and FE balance.
- One combined input limit across all permitted cable connections.
- One owner and [permission profile](permissions.md).

Capacity and input speed both scale with the number of member blocks. For example, a **3 x 3** structure holds **900,000 FE** and accepts up to **1,800 FE/t total**.

The **top surface visually connects into one continuous cyan teleportation field** when adjacent Anchors belong to the same logical machine. Copper edging appears around the outside of the structure rather than between joined blocks. Separate machines keep their own borders, even if they touch.

The outer side walls use a continuous dark-steel casing with a slim cyan indicator line. Copper reinforcing posts appear only at the outside corners, rather than repeating on every member block.

A placed Anchor joins a neighbouring machine only if together they form a complete rectangle; placing never splits an existing machine. Multiblocks can cross chunk boundaries without forcing chunks to stay loaded.

Machines only join automatically when their owners and permission profiles are compatible. See [Machine Permissions](permissions.md).

## Automatic and Manual Joining

Normal placement attempts to join compatible neighboring machines.

Hold the **Sprint key** (Ctrl by default) while placing an Anchor to keep it separate. That choice applies only to the current placement; it is not stored in the item. With the Toggle Sprint option enabled, placements stay separate while sprinting is toggled on.

Intentionally separated structures can later be joined using the <ItemLink id="fluxbound:wrench" />, provided their combined shape is a valid rectangle and the joining rules are met. See [Selecting and Joining Machines](wrench.md#selecting-and-joining-machines).

## Management Screen

Right-click an Anchor to open its management screen. This works with an empty hand or while holding most items. To place a block against an Anchor instead, hold **Sneak** while right-clicking. Any player can open the screen; the settings tabs require **Manage** permission.

The bar on the left shows the machine's energy. Hover over it to see the stored FE, the capacity, and the maximum input of the whole multiblock.

Below the bar is the **charging slot**. Place an item that stores FE there to charge it from the machine's energy, up to the machine's maximum input per tick. Inserting items requires **Charge** permission, but anyone can take an item out, so a public charger is not safe storage. Hover over the lightning icon below the slot to see whether the item is charging, and why not.

The tabs above the screen switch between:

- **Overview:** the owner, the number of blocks, the dimension, and the approximate centre position of the multiblock.
- **Permissions:** who may arrive at, manage, and charge from this machine. See [Configuring in the Screen](permissions.md#configuring-in-the-screen).
- **Trusted Players:** the players who receive the rights set to **Trusted**. See [Trusted Players](permissions.md#trusted-players).

Players without Manage permission can still see the Overview; the other two tabs tell them that Manage permission is required.

The **?** at the top right of each tab explains that tab when you hover over it. With the keyboard, **Tab** moves between the screen's controls and shows the same explanations, and **Ctrl+Tab** or **Ctrl+1–3** switches tabs. The screen closes when you move too far away, when the clicked block is removed, or when the multiblock is split or merged into a different machine. Reopen it to see the new machine.

## Breaking and Picking Up Machines

Arrival Anchors require a **stone-tier or better pickaxe** for normal mining drops.

Removing a block only affects its own machine. If the rest is no longer a rectangle, it splits into rectangles: the FE fills the largest part first, then the others in turn. Only energy that no longer fits can follow a dropped item; if there is no item, that excess is lost.

The charging slot belongs to the whole multiblock. Its item drops when the last block is removed. When two machines are joined and both hold an item, one item drops.

### Survival Mining

Mining with a suitable tool produces an Arrival Anchor item. If meaningful machine data must be preserved, the item carries a packed-machine reference so its FE and configuration can be restored when placed.

An Anchor with **0 FE and default settings** becomes an ordinary item that stacks with freshly crafted Anchors. Nondefault permissions count as meaningful configuration, even if its energy is empty.

### Resetting a Packed Anchor

Place a packed Anchor alone in a crafting grid to turn it into an ordinary Anchor that stacks with freshly crafted ones. Its stored FE and settings are lost.

### Creative Destruction

Normally destroying an Anchor in Creative Mode produces **no item**. Surviving structures keep as much FE as their remaining capacity allows; any excess is voided. Destroying the final block retires its machine data.

Using the <ItemLink id="fluxbound:wrench" /> to pick up a block is different: wrench pickup returns an item **even in Creative Mode**.

### Explosions

Explosions follow Minecraft's normal block-drop rules. If an Anchor item survives the explosion, its meaningful stored data is attached to that drop. If no item is generated, no packed item can be recovered.

### Wrench Pickup

Sneak-right-click an Anchor with the <ItemLink id="fluxbound:wrench" /> to pick up one block instantly, in Survival or Creative Mode. FE is retained in placed structures first; any excess is saved in the returned item when possible.

Read [Picking Up Machine Blocks](wrench.md#picking-up-machine-blocks) for the wrench controls.

## Ownership and Permissions

Every placed Anchor has an owner. Three permissions apply to Anchors: **Arrive**, **Manage**, and **Charge**, all defaulting to Owner Only.

Manage permission controls the wrench, the Permissions and Trusted Players tabs of the management screen, and the permission commands.

Permission settings apply to the entire logical multiblock and survive packed pickup. Machines with different permission settings never join. A machine with default settings can be joined to a configured one with the wrench and then takes on its settings.

See [Machine Permissions](permissions.md) for access modes, trusted players, and configuration commands.

---
navigation:
  title: Machine Permissions
  position: 4
---

# Machine Permissions

Every placed <ItemLink id="fluxbound:arrival_anchor" /> has an owner and a set of permissions. The owner always has full access.

All blocks in one [logical multiblock](arrival_anchor.md#multiblocks) share the same owner, trusted-player list, and permission settings.

## Permission Types

| Permission | Controls |
| --- | --- |
| **Arrive** | Who may arrive at this Anchor by teleportation |
| **Manage** | Who may change settings, edit the trusted list, and use the <ItemLink id="fluxbound:wrench" /> on the machine |
| **Charge** | Who may charge handheld teleporters at this Anchor |

All permissions default to **Owner Only**.

## Access Modes

Each permission can be set independently to one of these modes:

- **Owner:** Only the machine owner.
- **Trusted:** The owner and players on that machine's trusted list.
- **Everyone:** Any player.

The owner always retains access, regardless of the selected mode.

## Configuring in the Screen

Open the Anchor's [management screen](arrival_anchor.md#management-screen) and select the **Permissions** tab. Click a permission's field and choose a mode from the list. The list can also be used with the arrow keys and Enter; Esc closes it.

Two changes ask for confirmation first:

- Setting **Manage** to **Everyone**, because every player can then change the settings and pick up the machine with the wrench.
- A change that takes away your own Manage permission, because only the owner can give it back.

## Trusted Players

Each logical machine has one trusted list. A player on this list gains only the rights set to **Trusted**; adding someone does not automatically grant every permission.

In the **Trusted Players** tab of the management screen:

- Type a player name and press **Enter** or **Add**. While typing, the names of online players are completed in grey; press **Tab** or the right arrow key to accept. Players who are offline can be added by their exact name.
- A message below the name field shows the result, for example when a name was not found.
- Click the **×** next to a player to remove them. With keyboard focus on the list, the up and down arrow keys select a player and **Delete** or **Backspace** removes them.
- Removing yourself asks for confirmation first if it would take away your own Manage permission.

Players the server has no name for appear as "Unknown player"; hover over the entry to see their ID.

## Commands

Instead of the screen, you can aim directly at an Arrival Anchor from within approximately six blocks and use these commands:

| Command | Effect |
| --- | --- |
| `/fluxbound access show` | Display the current permission modes and trusted-player count |
| `/fluxbound access arrive trusted` | Allow trusted players to arrive at the Anchor |
| `/fluxbound access manage trusted` | Allow trusted players to manage the machine |
| `/fluxbound access charge owner` | Allow only the owner to charge |
| `/fluxbound trust add PlayerName` | Add a player to the trusted list by name |
| `/fluxbound trust remove PlayerName` | Remove a player from the trusted list |

Viewing and changing settings through these commands requires **Manage** permission. Players who are offline can be added by their exact name.

## Multiblocks and Joining

Machines with different owners or permission profiles do not automatically join. Manual joining with the <ItemLink id="fluxbound:wrench" /> also currently requires the same owner and identical permissions.

See [Selecting and Joining Machines](wrench.md#selecting-and-joining-machines) for the joining rules.

## Machine Pickup

Configured permissions survive when a machine is packed, even if it has no FE stored. Such items do not stack with unconfigured machines.

An empty machine with default permissions becomes an ordinary stackable item when picked up. Placing an Anchor separately does not count as persistent configuration.

For the differences between survival mining, creative destruction, explosions, and wrench pickup, read [Breaking and Picking Up Machines](arrival_anchor.md#breaking-and-picking-up-machines).

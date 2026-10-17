---
navigation:
  title: Machine Permissions
  position: 4
---

# Machine Permissions

Every placed <ItemLink id="fluxbound:arrival_anchor" /> has an owner and a set of permissions. The owner always has full access.

All blocks in one [logical multiblock](arrival_anchor.md#multiblocks) share the same owner, trusted-player list, and permission settings.

## Permission Types

| Permission | Purpose | Current status |
| --- | --- | --- |
| **Use** | Starting teleportation from a machine | Stored; teleportation not implemented yet |
| **Arrive** | Teleporting to a destination machine | Stored; reception not implemented yet |
| **Manage** | Modifying settings and using the <ItemLink id="fluxbound:wrench" /> | Enforced |
| **Charge** | Charging handheld teleporters | Stored; charging not implemented yet |

All four permissions default to **Owner Only**.

## Access Modes

Each permission can be set independently to one of these modes:

- **Owner:** Only the machine owner.
- **Trusted:** The owner and players on that machine's trusted list.
- **Everyone:** Any player.
- **Team:** Intended for the owner's FTB Team; until that optional integration is implemented, only the owner is allowed.

The owner always retains access, regardless of the selected mode.

## Trusted Players

Each logical machine maintains one list of trusted player UUIDs. A player on this list gains only the rights set to **Trusted**; adding someone does not automatically grant every permission.

Trust settings are shared across all members of the same multiblock.

## Configuring Permissions

A graphical permissions screen is planned but not implemented yet. For now, aim directly at an Arrival Anchor from within approximately six blocks and use the server-side commands below.

| Command | Effect |
| --- | --- |
| `/fluxbound access show` | Display current permission modes and trusted-player count |
| `/fluxbound access use everyone` | Allow anyone to use the machine when teleportation is implemented |
| `/fluxbound access arrive trusted` | Reserve arrival rights for trusted players when reception is implemented |
| `/fluxbound access manage trusted` | Allow trusted players to manage the machine |
| `/fluxbound access charge owner` | Restrict future charging to the owner |
| `/fluxbound trust add PlayerName` | Add an online player to the trusted list |
| `/fluxbound trust remove PlayerName` | Remove an online player from the trusted list |

Changing settings and viewing them through these commands requires **Manage** permission. Trust commands currently require the target player to be online, but saved trust uses player UUIDs.

## Multiblocks and Joining

Machines with different owners or permission profiles do not automatically join. Manual joining with the <ItemLink id="fluxbound:wrench" /> also currently requires the same owner and identical permissions.

See [Selecting and Joining Machines](wrench.md#selecting-and-joining-machines) for the joining rules.

## Machine Pickup

Configured permissions survive when a machine is packed, even if it has no FE stored. Such items do not stack with unconfigured machines.

An empty machine with default permissions becomes an ordinary stackable item when picked up. Sneak-placement does not count as persistent configuration.

For the differences between survival mining, creative destruction, explosions, and wrench pickup, read [Breaking and Picking Up Machines](arrival_anchor.md#breaking-and-picking-up-machines).

package io.github.chamoisest.fluxbound.machine.permission;

// Outcome of a request to add or remove a trusted player, shared by the GUI and commands so both can explain it.
// Also sent to the client, so changing the order of the constants changes the network format.
public enum TrustChange
{
    ADDED,
    REMOVED,
    // No such machine, or the requester lacks Manage.
    DENIED,
    ALREADY_TRUSTED,
    NOT_TRUSTED,
    // The owner always has access, so they are never put on the list.
    IS_OWNER,
    INVALID_NAME,
    NOT_FOUND,
    // The name service failed or could not be reached.
    LOOKUP_FAILED,
    // The requester has a lookup in flight or looked up a name too recently (M1 E4, B6a-1).
    LOOKUP_BUSY
}

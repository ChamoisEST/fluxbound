package io.github.chamoisest.fluxbound.machine.permission;

// Outcome of a requested permission change, so callers such as commands can explain a refusal.
public enum PermissionChange
{
    APPLIED,
    // No such machine, or the player lacks Manage.
    DENIED,
    RIGHT_NOT_USED,
    MODE_UNAVAILABLE
}

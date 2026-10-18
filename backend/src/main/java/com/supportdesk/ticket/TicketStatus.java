package com.supportdesk.ticket;

import java.util.Set;

/**
 * Ticket lifecycle:
 *
 *   OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
 *    │  ◄────────┘              │
 *    │  ◄───────────────────────┘ (customer reopens)
 *    └──────────────────────────────────► CLOSED (duplicate / spam)
 *
 * Keeping the allowed moves in one place means no controller or service can put a
 * ticket into an impossible state (for example CLOSED straight back to IN_PROGRESS).
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public Set<TicketStatus> allowedNext() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, CLOSED);
            case IN_PROGRESS -> Set.of(OPEN, RESOLVED);
            case RESOLVED -> Set.of(OPEN, CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canMoveTo(TicketStatus next) {
        return allowedNext().contains(next);
    }
}

package com.supportdesk.ticket;

/** Optional filters for the ticket list. Any field may be null. */
public record TicketFilter(
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Boolean assignedToMe,
        String q) {
}

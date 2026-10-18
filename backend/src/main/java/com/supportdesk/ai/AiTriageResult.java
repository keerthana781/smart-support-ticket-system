package com.supportdesk.ai;

import com.supportdesk.ticket.TicketCategory;
import com.supportdesk.ticket.TicketPriority;

/** The AI's suggestion for a new ticket. An agent always reviews it before anything reaches the customer. */
public record AiTriageResult(
        TicketCategory category,
        TicketPriority priority,
        String suggestedReply,
        String reasoning,
        AiSource source) {
}

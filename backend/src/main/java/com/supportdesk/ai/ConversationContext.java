package com.supportdesk.ai;

import java.util.List;

/**
 * Plain snapshot of a ticket conversation, built inside a database transaction and then handed
 * to the AI outside it, so a slow AI call never holds a database connection open.
 */
public record ConversationContext(
        String title,
        String description,
        String customerName,
        String category,
        List<Message> messages) {

    public record Message(String authorRole, String authorName, String body, boolean internal) {
    }
}

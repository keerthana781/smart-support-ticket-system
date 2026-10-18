package com.supportdesk.ticket;

import org.springframework.data.jpa.domain.Specification;

/**
 * Small, composable WHERE-clause building blocks for the ticket list filters.
 * Each method returns null when its filter is not used, and Spring Data simply skips it.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> createdBy(Long userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId);
    }

    public static Specification<Ticket> assignedTo(Long userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), userId);
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return priority == null ? null : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasCategory(TicketCategory category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Ticket> textContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern),
                cb.like(cb.lower(root.<String>get("description")), pattern));
    }
}

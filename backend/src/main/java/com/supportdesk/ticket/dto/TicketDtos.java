package com.supportdesk.ticket.dto;

import com.supportdesk.ai.AiSource;
import com.supportdesk.ticket.TicketCategory;
import com.supportdesk.ticket.TicketPriority;
import com.supportdesk.ticket.TicketStatus;
import com.supportdesk.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Request and response shapes for the ticket API, grouped in one file because they are small.
 * Using records keeps them immutable and free of boilerplate.
 */
public final class TicketDtos {

    private TicketDtos() {
    }

    // ---------- Requests ----------

    public record CreateTicketRequest(
            @NotBlank(message = "Title is required")
            @Size(min = 5, max = 150, message = "Title must be 5 to 150 characters")
            String title,

            @NotBlank(message = "Description is required")
            @Size(min = 10, max = 5000, message = "Description must be 10 to 5000 characters")
            String description) {
    }

    public record ChangeStatusRequest(@NotNull(message = "Status is required") TicketStatus status) {
    }

    /** Staff-only edit. Null fields are left unchanged. Set unassign=true to clear the assignee. */
    public record UpdateTicketRequest(
            TicketPriority priority,
            TicketCategory category,
            Long assigneeId,
            Boolean unassign) {
    }

    public record AddCommentRequest(
            @NotBlank(message = "Message cannot be empty")
            @Size(max = 5000, message = "Message must be at most 5000 characters")
            String body,
            boolean internal,
            boolean aiAssisted) {
    }

    // ---------- Responses ----------

    public record UserSummary(Long id, String fullName, String email, Role role) {
    }

    /** One row in the ticket list. */
    public record TicketSummary(
            Long id,
            String title,
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            UserSummary createdBy,
            UserSummary assignedTo,
            Instant createdAt,
            Instant updatedAt) {
    }

    /** AI suggestion block. Only sent to agents and admins. */
    public record AiInsight(
            TicketCategory suggestedCategory,
            TicketPriority suggestedPriority,
            String suggestedReply,
            String reasoning,
            AiSource source,
            boolean overriddenByAgent) {
    }

    public record CommentResponse(
            Long id,
            UserSummary author,
            String body,
            boolean internal,
            boolean aiAssisted,
            Instant createdAt) {
    }

    /** Full ticket view with its conversation. */
    public record TicketDetail(
            Long id,
            String title,
            String description,
            TicketStatus status,
            List<TicketStatus> allowedNextStatuses,
            TicketPriority priority,
            TicketCategory category,
            UserSummary createdBy,
            UserSummary assignedTo,
            AiInsight ai,
            List<CommentResponse> comments,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt) {
    }

    public record DraftReplyResponse(String reply, AiSource source) {
    }
}

package com.supportdesk.ticket;

import com.supportdesk.ticket.dto.TicketDtos.AiInsight;
import com.supportdesk.ticket.dto.TicketDtos.CommentResponse;
import com.supportdesk.ticket.dto.TicketDtos.TicketDetail;
import com.supportdesk.ticket.dto.TicketDtos.TicketSummary;
import com.supportdesk.ticket.dto.TicketDtos.UserSummary;
import com.supportdesk.user.User;

import java.util.List;

/** Converts entities to API responses. Must be called inside a transaction (lazy relations are read). */
final class TicketMapper {

    private TicketMapper() {
    }

    static UserSummary user(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    static TicketSummary summary(Ticket ticket) {
        return new TicketSummary(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                user(ticket.getCreatedBy()),
                user(ticket.getAssignedTo()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    static CommentResponse comment(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                user(comment.getAuthor()),
                comment.getBody(),
                comment.isInternal(),
                comment.isAiAssisted(),
                comment.getCreatedAt());
    }

    static TicketDetail detail(Ticket ticket, List<TicketComment> comments, boolean staffView,
                               List<TicketStatus> allowedNext) {
        AiInsight ai = null;
        if (staffView && ticket.getAiSource() != null) {
            boolean overridden = ticket.getCategory() != ticket.getAiCategory()
                    || ticket.getPriority() != ticket.getAiPriority();
            ai = new AiInsight(
                    ticket.getAiCategory(),
                    ticket.getAiPriority(),
                    ticket.getAiSuggestedReply(),
                    ticket.getAiReasoning(),
                    ticket.getAiSource(),
                    overridden);
        }

        return new TicketDetail(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                allowedNext,
                ticket.getPriority(),
                ticket.getCategory(),
                user(ticket.getCreatedBy()),
                user(ticket.getAssignedTo()),
                ai,
                comments.stream().map(TicketMapper::comment).toList(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt());
    }
}

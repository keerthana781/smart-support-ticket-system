package com.supportdesk.ticket;

import com.supportdesk.common.PageResponse;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.ticket.dto.TicketDtos.AddCommentRequest;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.DraftReplyResponse;
import com.supportdesk.ticket.dto.TicketDtos.TicketDetail;
import com.supportdesk.ticket.dto.TicketDtos.TicketSummary;
import com.supportdesk.ticket.dto.TicketDtos.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP layer: validates input, checks the caller's role, and delegates to TicketService.
 * No business logic lives here.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final String STAFF_ONLY = "hasAnyRole('AGENT', 'ADMIN')";

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public PageResponse<TicketSummary> list(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Boolean assignedToMe,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TicketFilter filter = new TicketFilter(status, priority, category, assignedToMe, q);
        return ticketService.list(currentUser, filter, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetail create(@AuthenticationPrincipal SecurityUser currentUser,
                               @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(currentUser, request);
    }

    @GetMapping("/{id}")
    public TicketDetail get(@AuthenticationPrincipal SecurityUser currentUser, @PathVariable Long id) {
        return ticketService.get(currentUser, id);
    }

    @PatchMapping("/{id}/status")
    public TicketDetail changeStatus(@AuthenticationPrincipal SecurityUser currentUser,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(currentUser, id, request.status());
    }

    @PatchMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    public TicketDetail update(@AuthenticationPrincipal SecurityUser currentUser,
                               @PathVariable Long id,
                               @RequestBody UpdateTicketRequest request) {
        return ticketService.update(currentUser, id, request);
    }

    @PostMapping("/{id}/assign-to-me")
    @PreAuthorize(STAFF_ONLY)
    public TicketDetail assignToMe(@AuthenticationPrincipal SecurityUser currentUser, @PathVariable Long id) {
        return ticketService.assignToMe(currentUser, id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetail addComment(@AuthenticationPrincipal SecurityUser currentUser,
                                   @PathVariable Long id,
                                   @Valid @RequestBody AddCommentRequest request) {
        return ticketService.addComment(currentUser, id, request);
    }

    @PostMapping("/{id}/ai/draft-reply")
    @PreAuthorize(STAFF_ONLY)
    public DraftReplyResponse draftReply(@AuthenticationPrincipal SecurityUser currentUser, @PathVariable Long id) {
        return ticketService.draftReply(currentUser, id);
    }
}

package com.supportdesk.ticket;

import com.supportdesk.ai.AiAssistantService;
import com.supportdesk.ai.AiDraftResult;
import com.supportdesk.ai.AiTriageResult;
import com.supportdesk.ai.ConversationContext;
import com.supportdesk.common.BadRequestException;
import com.supportdesk.common.NotFoundException;
import com.supportdesk.common.PageResponse;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.ticket.dto.TicketDtos.AddCommentRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.DraftReplyResponse;
import com.supportdesk.ticket.dto.TicketDtos.TicketDetail;
import com.supportdesk.ticket.dto.TicketDtos.TicketSummary;
import com.supportdesk.ticket.dto.TicketDtos.UpdateTicketRequest;
import com.supportdesk.user.Role;
import com.supportdesk.user.User;
import com.supportdesk.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * All ticket business rules live here:
 *  - who can see which tickets (customers: only their own; staff: all)
 *  - which status changes are allowed, and by whom
 *  - calling the AI on create, without letting a slow AI call hold a DB transaction open
 */
@Service
public class TicketService {

    /** The only status moves a customer may make on their own ticket. */
    private static final Set<String> CUSTOMER_MOVES = Set.of(
            "OPEN->CLOSED",       // withdraw a ticket
            "RESOLVED->CLOSED",   // confirm the fix
            "RESOLVED->OPEN");    // the fix did not work: reopen

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AiAssistantService aiAssistant;
    private final TransactionTemplate transactionTemplate;

    public TicketService(TicketRepository ticketRepository,
                         TicketCommentRepository commentRepository,
                         UserRepository userRepository,
                         AiAssistantService aiAssistant,
                         TransactionTemplate transactionTemplate) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.aiAssistant = aiAssistant;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> list(SecurityUser currentUser, TicketFilter filter, Pageable pageable) {
        Specification<Ticket> spec = Stream.of(
                        currentUser.isStaff() ? null : TicketSpecifications.createdBy(currentUser.getId()),
                        Boolean.TRUE.equals(filter.assignedToMe()) ? TicketSpecifications.assignedTo(currentUser.getId()) : null,
                        TicketSpecifications.hasStatus(filter.status()),
                        TicketSpecifications.hasPriority(filter.priority()),
                        TicketSpecifications.hasCategory(filter.category()),
                        TicketSpecifications.textContains(filter.q()))
                .filter(s -> s != null)
                .reduce((root, query, cb) -> cb.conjunction(), Specification::and);

        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        return PageResponse.from(page, TicketMapper::summary);
    }

    /**
     * Creates a ticket. The AI call happens BEFORE the database transaction starts:
     * an LLM can take several seconds, and we don't want a DB connection held open while waiting.
     */
    public TicketDetail create(SecurityUser currentUser, CreateTicketRequest request) {
        String title = request.title().trim();
        String description = request.description().trim();

        AiTriageResult triage = aiAssistant.triage(title, description, currentUser.getFullName());

        return transactionTemplate.execute(status -> {
            User creator = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            Ticket ticket = new Ticket(title, description, creator);
            ticket.applyAiTriage(triage.category(), triage.priority(), triage.suggestedReply(),
                    triage.reasoning(), triage.source());
            ticketRepository.save(ticket);
            return toDetail(ticket, currentUser);
        });
    }

    @Transactional(readOnly = true)
    public TicketDetail get(SecurityUser currentUser, Long ticketId) {
        return toDetail(findAccessible(currentUser, ticketId), currentUser);
    }

    @Transactional
    public TicketDetail changeStatus(SecurityUser currentUser, Long ticketId, TicketStatus next) {
        Ticket ticket = findAccessible(currentUser, ticketId);

        if (!currentUser.isStaff() && !CUSTOMER_MOVES.contains(ticket.getStatus() + "->" + next)) {
            throw new AccessDeniedException("Customers cannot make this status change");
        }
        try {
            ticket.changeStatus(next);
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        // An agent who starts work on an unassigned ticket picks it up automatically.
        if (next == TicketStatus.IN_PROGRESS && ticket.getAssignedTo() == null && currentUser.isStaff()) {
            ticket.setAssignedTo(userRepository.getReferenceById(currentUser.getId()));
        }
        return toDetail(ticket, currentUser);
    }

    /** Staff only (enforced in the controller): change priority, category or assignee. */
    @Transactional
    public TicketDetail update(SecurityUser currentUser, Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = findAccessible(currentUser, ticketId);

        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (Boolean.TRUE.equals(request.unassign())) {
            ticket.setAssignedTo(null);
        } else if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assignee not found"));
            if (assignee.getRole() == Role.CUSTOMER || !assignee.isActive()) {
                throw new BadRequestException("Tickets can only be assigned to active agents or admins");
            }
            ticket.setAssignedTo(assignee);
        }
        return toDetail(ticket, currentUser);
    }

    @Transactional
    public TicketDetail assignToMe(SecurityUser currentUser, Long ticketId) {
        Ticket ticket = findAccessible(currentUser, ticketId);
        ticket.setAssignedTo(userRepository.getReferenceById(currentUser.getId()));
        return toDetail(ticket, currentUser);
    }

    @Transactional
    public TicketDetail addComment(SecurityUser currentUser, Long ticketId, AddCommentRequest request) {
        Ticket ticket = findAccessible(currentUser, ticketId);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("This ticket is closed. Please raise a new ticket.");
        }
        boolean staff = currentUser.isStaff();
        // Customers can never post internal notes or claim AI assistance.
        boolean internal = staff && request.internal();
        boolean aiAssisted = staff && request.aiAssisted();

        User author = userRepository.getReferenceById(currentUser.getId());
        commentRepository.save(new TicketComment(ticket, author, request.body().trim(), internal, aiAssisted));

        // When an agent sends the first public reply, work has clearly started.
        if (staff && !internal && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.changeStatus(TicketStatus.IN_PROGRESS);
            if (ticket.getAssignedTo() == null) {
                ticket.setAssignedTo(author);
            }
        }
        return toDetail(ticket, currentUser);
    }

    /**
     * Staff only: asks the AI for a fresh draft reply based on the whole conversation.
     * The draft is returned to the agent to edit. Nothing is sent to the customer automatically.
     */
    public DraftReplyResponse draftReply(SecurityUser currentUser, Long ticketId) {
        ConversationContext context = transactionTemplate.execute(status -> {
            Ticket ticket = findAccessible(currentUser, ticketId);
            List<ConversationContext.Message> messages = commentRepository
                    .findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                    .map(c -> new ConversationContext.Message(
                            c.getAuthor().getRole().name(), c.getAuthor().getFullName(), c.getBody(), c.isInternal()))
                    .toList();
            return new ConversationContext(ticket.getTitle(), ticket.getDescription(),
                    ticket.getCreatedBy().getFullName(), ticket.getCategory().name(), messages);
        });

        AiDraftResult draft = aiAssistant.draftReply(context);
        return new DraftReplyResponse(draft.reply(), draft.source());
    }

    // ---------- helpers ----------

    /**
     * Loads a ticket the current user is allowed to see. A customer asking for someone else's ticket
     * gets "not found" rather than "forbidden", so ticket IDs of other customers are not revealed.
     */
    private Ticket findAccessible(SecurityUser currentUser, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (!currentUser.isStaff() && !ticket.isOwnedBy(currentUser.getId())) {
            throw new NotFoundException("Ticket not found");
        }
        return ticket;
    }

    private TicketDetail toDetail(Ticket ticket, SecurityUser currentUser) {
        boolean staff = currentUser.isStaff();
        List<TicketComment> comments = ticket.getId() == null ? List.of()
                : staff ? commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                : commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticket.getId());

        List<TicketStatus> allowedNext = ticket.getStatus().allowedNext().stream()
                .filter(next -> staff || CUSTOMER_MOVES.contains(ticket.getStatus() + "->" + next))
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();

        return TicketMapper.detail(ticket, comments, staff, allowedNext);
    }
}

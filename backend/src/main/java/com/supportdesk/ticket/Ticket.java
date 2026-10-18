package com.supportdesk.ticket;

import com.supportdesk.ai.AiSource;
import com.supportdesk.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_created_by", columnList = "created_by_id"),
        @Index(name = "idx_ticket_assigned_to", columnList = "assigned_to_id")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    /** Current values. Agents may change these after reviewing the AI suggestion. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketCategory category;

    /** What the AI originally suggested. Kept so we can measure how often agents override it. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TicketPriority aiPriority;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TicketCategory aiCategory;

    @Column(columnDefinition = "TEXT")
    private String aiSuggestedReply;

    @Column(length = 500)
    private String aiReasoning;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AiSource aiSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    protected Ticket() {
        // required by JPA
    }

    public Ticket(String title, String description, User createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Applies the AI triage result. Current priority/category start out equal to the AI suggestion. */
    public void applyAiTriage(TicketCategory category, TicketPriority priority,
                              String suggestedReply, String reasoning, AiSource source) {
        this.aiCategory = category;
        this.aiPriority = priority;
        this.category = category;
        this.priority = priority;
        this.aiSuggestedReply = suggestedReply;
        this.aiReasoning = reasoning;
        this.aiSource = source;
    }

    /** Moves the ticket through its lifecycle, refusing invalid jumps. */
    public void changeStatus(TicketStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Cannot move ticket from " + status + " to " + next);
        }
        this.status = next;
        if (next == TicketStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
        } else if (next == TicketStatus.OPEN || next == TicketStatus.IN_PROGRESS) {
            this.resolvedAt = null;
        }
    }

    public boolean isOwnedBy(Long userId) {
        return createdBy.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketPriority getAiPriority() {
        return aiPriority;
    }

    public TicketCategory getAiCategory() {
        return aiCategory;
    }

    public String getAiSuggestedReply() {
        return aiSuggestedReply;
    }

    public void setAiSuggestedReply(String aiSuggestedReply) {
        this.aiSuggestedReply = aiSuggestedReply;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public AiSource getAiSource() {
        return aiSource;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

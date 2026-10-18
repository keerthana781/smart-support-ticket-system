package com.supportdesk.ticket;

import com.supportdesk.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Internal notes are visible to agents and admins only, never to the customer. */
    @Column(nullable = false)
    private boolean internal;

    /** True when an agent started from an AI draft. Useful for measuring how helpful the AI is. */
    @Column(nullable = false)
    private boolean aiAssisted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketComment() {
        // required by JPA
    }

    public TicketComment(Ticket ticket, User author, String body, boolean internal, boolean aiAssisted) {
        this.ticket = ticket;
        this.author = author;
        this.body = body;
        this.internal = internal;
        this.aiAssisted = aiAssisted;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean isAiAssisted() {
        return aiAssisted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

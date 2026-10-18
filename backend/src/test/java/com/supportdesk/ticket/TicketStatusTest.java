package com.supportdesk.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    @Test
    void openTicketCanStartOrBeClosed() {
        assertThat(TicketStatus.OPEN.canMoveTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.OPEN.canMoveTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void openTicketCannotJumpStraightToResolved() {
        assertThat(TicketStatus.OPEN.canMoveTo(TicketStatus.RESOLVED)).isFalse();
    }

    @Test
    void resolvedTicketCanBeReopenedOrClosed() {
        assertThat(TicketStatus.RESOLVED.canMoveTo(TicketStatus.OPEN)).isTrue();
        assertThat(TicketStatus.RESOLVED.canMoveTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void closedIsFinal() {
        assertThat(TicketStatus.CLOSED.allowedNext()).isEmpty();
    }
}

package com.supportdesk.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countByStatus();

    @Query("select t.category, count(t) from Ticket t group by t.category")
    List<Object[]> countByCategory();

    @Query("select t.priority, count(t) from Ticket t group by t.priority")
    List<Object[]> countByPriority();

    @Query("select count(t) from Ticket t where t.aiCategory is not null")
    long countAiTriaged();

    /** Tickets where an agent disagreed with the AI and changed the category or priority. */
    @Query("select count(t) from Ticket t where t.aiCategory is not null "
            + "and (t.category <> t.aiCategory or t.priority <> t.aiPriority)")
    long countAiOverridden();
}

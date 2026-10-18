package com.supportdesk.ai;

import com.supportdesk.ticket.TicketCategory;
import com.supportdesk.ticket.TicketPriority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAiClientTest {

    private final RuleBasedAiClient client = new RuleBasedAiClient();

    @Test
    void duplicatePaymentIsHighPriorityBilling() {
        AiTriageResult result = client.triage("Charged twice",
                "My card was charged twice for the subscription, please refund", "Priya Sharma");

        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
        assertThat(result.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(result.source()).isEqualTo(AiSource.RULES);
    }

    @Test
    void outageIsUrgent() {
        AiTriageResult result = client.triage("Site is down",
                "The dashboard is down for all users, production is blocked", "Rahul");

        assertThat(result.priority()).isEqualTo(TicketPriority.URGENT);
    }

    @Test
    void featureIdeaIsLowPriorityFeatureRequest() {
        AiTriageResult result = client.triage("Dark mode",
                "It would be nice to have a dark mode feature", "Rahul");

        assertThat(result.category()).isEqualTo(TicketCategory.FEATURE_REQUEST);
        assertThat(result.priority()).isEqualTo(TicketPriority.LOW);
    }

    @Test
    void unknownTextFallsBackToGeneralMedium() {
        AiTriageResult result = client.triage("Hello", "I have a query about your company", "Rahul");

        assertThat(result.category()).isEqualTo(TicketCategory.GENERAL);
        assertThat(result.priority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void suggestedReplyGreetsCustomerByFirstName() {
        AiTriageResult result = client.triage("Login issue", "Cannot log in", "Priya Sharma");

        assertThat(result.suggestedReply()).startsWith("Hi Priya,");
    }

    @Test
    void draftReplyUsesTicketCategory() {
        ConversationContext context = new ConversationContext("Refund", "Need refund", "Priya Sharma",
                "BILLING", List.of());

        AiDraftResult draft = client.draftReply(context);

        assertThat(draft.reply()).contains("billing");
        assertThat(draft.source()).isEqualTo(AiSource.RULES);
    }
}

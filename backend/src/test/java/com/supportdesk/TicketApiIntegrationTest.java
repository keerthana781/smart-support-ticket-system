package com.supportdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportdesk.user.Role;
import com.supportdesk.user.User;
import com.supportdesk.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real HTTP layer, security filters and database (H2 in memory).
 * These prove the security rules actually work, not just that the code compiles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String agentEmail;

    @BeforeEach
    void createAgent() {
        agentEmail = "agent-" + UUID.randomUUID() + "@test.com";
        userRepository.save(new User("Test Agent", agentEmail, passwordEncoder.encode("Agent@123"), Role.AGENT));
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "not-an-email", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", agentEmail, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCreatesTicketAndAiTriagesIt() throws Exception {
        String customerToken = registerCustomer();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Charged twice this month",
                                "description", "My card was charged twice for the subscription. Please refund."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                // Customers must never see the AI block (it holds the internal draft reply).
                .andExpect(jsonPath("$.ai").doesNotExist());
    }

    @Test
    void customerCannotSeeAnotherCustomersTicket() throws Exception {
        String ownerToken = registerCustomer();
        long ticketId = createTicket(ownerToken);

        String otherToken = registerCustomer();
        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotUseAgentOnlyEndpoints() throws Exception {
        String customerToken = registerCustomer();
        long ticketId = createTicket(customerToken);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/ai/draft-reply")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentSeesAiSuggestionAndWorksTheTicket() throws Exception {
        String customerToken = registerCustomer();
        long ticketId = createTicket(customerToken);
        String agentToken = login(agentEmail, "Agent@123");

        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ai.source").value("RULES"))
                .andExpect(jsonPath("$.ai.suggestedReply").isNotEmpty());

        // A public reply from the agent moves the ticket to IN_PROGRESS and assigns it.
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "We are looking into it.", "internal", false, "aiAssisted", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo.email").value(agentEmail));

        // An internal note is hidden from the customer.
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "Checking payment gateway logs", "internal", true, "aiAssisted", false))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.comments.length()").value(1));

        // Invalid jump is rejected; valid one succeeds.
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CLOSED"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // The agent can ask for a fresh AI draft.
        mockMvc.perform(post("/api/tickets/" + ticketId + "/ai/draft-reply")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }

    // ---------- helpers ----------

    private String registerCustomer() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@test.com";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Test Customer", "email", email, "password", "Customer@123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).get("token").asText();
    }

    private long createTicket(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "App crashes on login",
                                "description", "The app shows an error and crashes when I log in."))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = read(result).get("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}

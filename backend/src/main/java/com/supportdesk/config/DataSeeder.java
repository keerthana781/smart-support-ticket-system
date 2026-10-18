package com.supportdesk.config;

import com.supportdesk.ai.AiTriageResult;
import com.supportdesk.ai.RuleBasedAiClient;
import com.supportdesk.ticket.Ticket;
import com.supportdesk.ticket.TicketComment;
import com.supportdesk.ticket.TicketCommentRepository;
import com.supportdesk.ticket.TicketRepository;
import com.supportdesk.ticket.TicketStatus;
import com.supportdesk.user.Role;
import com.supportdesk.user.User;
import com.supportdesk.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with demo accounts and tickets so the app is easy to try.
 * Turn off with SEED_DEMO_DATA=false. Never use these passwords anywhere real.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RuleBasedAiClient ruleBasedAi;

    public DataSeeder(AppProperties properties,
                      UserRepository userRepository,
                      TicketRepository ticketRepository,
                      TicketCommentRepository commentRepository,
                      PasswordEncoder passwordEncoder,
                      RuleBasedAiClient ruleBasedAi) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
        this.ruleBasedAi = ruleBasedAi;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.seed().enabled() || userRepository.count() > 0) {
            return;
        }

        User admin = createUser("Asha Admin", "admin@demo.com", "Admin@123", Role.ADMIN);
        User agent = createUser("Arjun Agent", "agent@demo.com", "Agent@123", Role.AGENT);
        User priya = createUser("Priya Sharma", "customer@demo.com", "Customer@123", Role.CUSTOMER);
        User rahul = createUser("Rahul Verma", "rahul@demo.com", "Customer@123", Role.CUSTOMER);

        createTicket(priya, "Charged twice for my monthly subscription",
                "I was charged ₹499 two times on 3rd October for the same subscription. "
                        + "Please refund the duplicate payment.");

        Ticket loginTicket = createTicket(rahul, "Cannot log in after password reset",
                "I reset my password yesterday but now login fails with 'invalid credentials'. "
                        + "I have tried three times and my account might be locked.");

        Ticket outage = createTicket(priya, "Dashboard is down - urgent",
                "Since this morning the dashboard shows a blank page for all users in our team. "
                        + "This is blocking our production work.");

        createTicket(rahul, "Feature suggestion: dark mode",
                "It would be nice to have a dark mode option in the app. Just a suggestion!");

        // A ticket already in progress with a short conversation.
        outage.setAssignedTo(agent);
        outage.changeStatus(TicketStatus.IN_PROGRESS);
        commentRepository.save(new TicketComment(outage, agent,
                "Hi Priya, thanks for flagging this. We have reproduced the issue and our engineers are on it.",
                false, true));
        commentRepository.save(new TicketComment(outage, agent,
                "Root cause looks like the CDN config change from last night. Rolling back.", true, false));

        // A resolved ticket the customer can confirm or reopen.
        loginTicket.setAssignedTo(admin);
        loginTicket.changeStatus(TicketStatus.IN_PROGRESS);
        commentRepository.save(new TicketComment(loginTicket, admin,
                "Hi Rahul, your account was temporarily locked after several failed attempts. "
                        + "I've unlocked it, please try again.", false, false));
        loginTicket.changeStatus(TicketStatus.RESOLVED);

        log.info("Demo data created. Logins: admin@demo.com / Admin@123, agent@demo.com / Agent@123, "
                + "customer@demo.com / Customer@123");
    }

    private User createUser(String name, String email, String password, Role role) {
        return userRepository.save(new User(name, email, passwordEncoder.encode(password), role));
    }

    private Ticket createTicket(User customer, String title, String description) {
        Ticket ticket = new Ticket(title, description, customer);
        AiTriageResult triage = ruleBasedAi.triage(title, description, customer.getFullName());
        ticket.applyAiTriage(triage.category(), triage.priority(), triage.suggestedReply(),
                triage.reasoning(), triage.source());
        return ticketRepository.save(ticket);
    }
}

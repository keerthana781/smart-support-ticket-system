package com.supportdesk.ai;

import com.supportdesk.ticket.TicketCategory;
import com.supportdesk.ticket.TicketPriority;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A simple, offline "AI" based on keywords.
 *
 * Why keep this when we have an LLM?
 *  1. The app works with no API key (handy for demos, tests and CI).
 *  2. If the LLM is down, slow or returns rubbish, ticket creation still succeeds.
 * This is a common production pattern: graceful degradation.
 */
@Component
public class RuleBasedAiClient {

    // LinkedHashMap keeps insertion order, so earlier categories win ties.
    private static final Map<TicketCategory, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put(TicketCategory.BILLING, List.of(
                "invoice", "payment", "refund", "charged", "charge", "billing", "bill", "price",
                "subscription", "upi", "credit card", "debit", "gst", "receipt"));
        CATEGORY_KEYWORDS.put(TicketCategory.ACCOUNT, List.of(
                "login", "log in", "password", "sign in", "signin", "account", "locked", "otp",
                "two-factor", "2fa", "username", "profile", "verify my email"));
        CATEGORY_KEYWORDS.put(TicketCategory.TECHNICAL, List.of(
                "error", "bug", "crash", "not working", "doesn't work", "does not work", "slow",
                "exception", "failed", "fails", "broken", "crashes", "crashed", "errors", "error 500", "timeout", "not loading", "blank page"));
        CATEGORY_KEYWORDS.put(TicketCategory.FEATURE_REQUEST, List.of(
                "feature", "suggestion", "suggest", "would be nice", "would love", "add support",
                "enhancement", "please add", "it would be great"));
    }

    private static final List<String> URGENT_WORDS = List.of(
            "urgent", "asap", "immediately", "outage", "is down", "production", "critical",
            "security", "hacked", "data loss", "all users", "cannot access", "can't access");

    private static final List<String> HIGH_WORDS = List.of(
            "not working", "blocked", "charged twice", "double charged", "failed", "error",
            "locked out", "locked", "fails", "crash", "crashes", "deadline");

    private static final List<String> LOW_WORDS = List.of(
            "question", "how do i", "how to", "suggestion", "feature", "would be nice", "just curious");

    public AiTriageResult triage(String title, String description, String customerName) {
        String text = (title + " " + description).toLowerCase(Locale.ROOT);

        TicketCategory category = detectCategory(text);
        TicketPriority priority = detectPriority(text);
        String reply = templateReply(category, customerName);
        String reasoning = "Keyword rules matched category " + category + " and priority " + priority + ".";

        return new AiTriageResult(category, priority, reply, reasoning, AiSource.RULES);
    }

    public AiDraftResult draftReply(ConversationContext context) {
        TicketCategory category;
        try {
            category = TicketCategory.valueOf(context.category());
        } catch (IllegalArgumentException | NullPointerException ex) {
            category = TicketCategory.GENERAL;
        }
        return new AiDraftResult(templateReply(category, context.customerName()), AiSource.RULES);
    }

    TicketCategory detectCategory(String text) {
        TicketCategory best = TicketCategory.GENERAL;
        int bestScore = 0;
        for (Map.Entry<TicketCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = (int) entry.getValue().stream().filter(k -> containsPhrase(text, k)).count();
            if (score > bestScore) {
                best = entry.getKey();
                bestScore = score;
            }
        }
        return best;
    }

    TicketPriority detectPriority(String text) {
        if (URGENT_WORDS.stream().anyMatch(k -> containsPhrase(text, k))) {
            return TicketPriority.URGENT;
        }
        if (HIGH_WORDS.stream().anyMatch(k -> containsPhrase(text, k))) {
            return TicketPriority.HIGH;
        }
        if (LOW_WORDS.stream().anyMatch(k -> containsPhrase(text, k))) {
            return TicketPriority.LOW;
        }
        return TicketPriority.MEDIUM;
    }

    private String templateReply(TicketCategory category, String customerName) {
        String greeting = "Hi " + (customerName == null || customerName.isBlank() ? "there" : firstName(customerName)) + ",\n\n";
        String body = switch (category) {
            case BILLING -> "Thanks for reaching out about your billing concern. I'm checking your payment "
                    + "records now. Could you share the transaction ID or invoice number, and the date of the "
                    + "payment? That will help me resolve this quickly.";
            case ACCOUNT -> "Sorry you're having trouble with your account. For your security, please confirm "
                    + "the email address registered with us and tell me what you see when you try to sign in "
                    + "(any error message or screenshot helps).";
            case TECHNICAL -> "Thanks for reporting this. To investigate, could you tell me which device and "
                    + "browser you're using, the steps that lead to the problem, and any error message you see? "
                    + "A screenshot would be very helpful.";
            case FEATURE_REQUEST -> "Thank you for the suggestion! I've shared it with our product team. "
                    + "Could you tell me a bit more about how you would use this feature, so we understand "
                    + "the need better?";
            case GENERAL -> "Thanks for contacting support. I'm looking into your request and will get back "
                    + "to you shortly. If you have any more details to add, please reply here.";
        };
        return greeting + body + "\n\nBest regards,\nSupport Team";
    }

    /**
     * Whole-word match, so "blocked" does not count as the account keyword "locked".
     */
    static boolean containsPhrase(String text, String phrase) {
        return Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b").matcher(text).find();
    }

    private String firstName(String fullName) {
        return fullName.trim().split("\\s+")[0];
    }
}

package com.supportdesk.ai;

import com.supportdesk.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The single entry point the rest of the app uses for AI features.
 *
 * It decides which engine to use (Gemini or rules) and guarantees that an AI problem never
 * breaks the main business flow: if Gemini fails, we log a warning and use the rules instead.
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final GeminiAiClient geminiClient;
    private final RuleBasedAiClient ruleBasedClient;
    private final boolean geminiSelected;

    public AiAssistantService(GeminiAiClient geminiClient, RuleBasedAiClient ruleBasedClient,
                              AppProperties properties) {
        this.geminiClient = geminiClient;
        this.ruleBasedClient = ruleBasedClient;
        this.geminiSelected = "gemini".equalsIgnoreCase(properties.ai().provider());

        if (geminiSelected && !geminiClient.isConfigured()) {
            log.warn("AI provider is 'gemini' but GEMINI_API_KEY is not set. Using rule-based triage.");
        }
    }

    public AiTriageResult triage(String title, String description, String customerName) {
        if (useGemini()) {
            try {
                return geminiClient.triage(title, description, customerName);
            } catch (RuntimeException ex) {
                log.warn("Gemini triage failed, falling back to rules: {}", ex.getMessage());
            }
        }
        return ruleBasedClient.triage(title, description, customerName);
    }

    public AiDraftResult draftReply(ConversationContext context) {
        if (useGemini()) {
            try {
                return geminiClient.draftReply(context);
            } catch (RuntimeException ex) {
                log.warn("Gemini draft failed, falling back to rules: {}", ex.getMessage());
            }
        }
        return ruleBasedClient.draftReply(context);
    }

    public String activeProvider() {
        return useGemini() ? AiSource.GEMINI.name() : AiSource.RULES.name();
    }

    private boolean useGemini() {
        return geminiSelected && geminiClient.isConfigured();
    }
}

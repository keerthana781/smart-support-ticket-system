package com.supportdesk.ai;

/** Which engine produced an AI suggestion. Shown in the UI so agents know what they are looking at. */
public enum AiSource {
    /** Google Gemini large language model. */
    GEMINI,
    /** Offline keyword rules, used when no API key is set or the LLM call fails. */
    RULES
}

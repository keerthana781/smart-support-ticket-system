package com.supportdesk.user;

public enum Role {
    /** Raises tickets and sees only their own tickets. */
    CUSTOMER,
    /** Works on tickets: assigns, changes status, replies (with AI help). */
    AGENT,
    /** Everything an agent can do, plus user management and dashboard stats. */
    ADMIN
}

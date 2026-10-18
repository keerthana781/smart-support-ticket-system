package com.supportdesk.user.dto;

import com.supportdesk.user.Role;

/** Admin update of a user. Null fields are left unchanged. */
public record UpdateUserRequest(Role role, Boolean active) {
}

package com.supportdesk.user.dto;

import com.supportdesk.user.Role;
import com.supportdesk.user.User;

import java.time.Instant;

/** What the API returns for a user. Never exposes the password hash. */
public record UserResponse(Long id, String fullName, String email, Role role, boolean active, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}

package com.supportdesk.auth.dto;

import com.supportdesk.user.dto.UserResponse;

public record AuthResponse(String token, long expiresInMinutes, UserResponse user) {
}

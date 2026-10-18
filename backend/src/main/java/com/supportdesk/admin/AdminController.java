package com.supportdesk.admin;

import com.supportdesk.admin.AdminService.DashboardStats;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.user.dto.UpdateUserRequest;
import com.supportdesk.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only endpoints. Protected twice: the URL rule in SecurityConfig and @PreAuthorize here. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateUser(@AuthenticationPrincipal SecurityUser currentUser,
                                   @PathVariable Long id,
                                   @RequestBody UpdateUserRequest request) {
        return adminService.updateUser(currentUser, id, request);
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return adminService.stats();
    }
}

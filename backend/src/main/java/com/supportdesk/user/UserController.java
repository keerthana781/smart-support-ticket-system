package com.supportdesk.user;

import com.supportdesk.common.NotFoundException;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The frontend calls this on page load to check the stored token is still valid. */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserResponse me(@AuthenticationPrincipal SecurityUser currentUser) {
        return userRepository.findById(currentUser.getId())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** People a ticket can be assigned to (for the assignee dropdown). */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponse> staff() {
        return userRepository.findByRoleInAndActiveTrueOrderByFullNameAsc(List.of(Role.AGENT, Role.ADMIN))
                .stream().map(UserResponse::from).toList();
    }
}

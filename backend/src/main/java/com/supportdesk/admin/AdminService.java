package com.supportdesk.admin;

import com.supportdesk.ai.AiAssistantService;
import com.supportdesk.common.BadRequestException;
import com.supportdesk.common.NotFoundException;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.ticket.TicketRepository;
import com.supportdesk.user.User;
import com.supportdesk.user.UserRepository;
import com.supportdesk.user.dto.UpdateUserRequest;
import com.supportdesk.user.dto.UserResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AiAssistantService aiAssistant;

    public AdminService(UserRepository userRepository, TicketRepository ticketRepository,
                        AiAssistantService aiAssistant) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.aiAssistant = aiAssistant;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("createdAt")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateUser(SecurityUser currentUser, Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // Stop an admin from accidentally locking themselves out.
        if (user.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot change your own role or status");
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        long triaged = ticketRepository.countAiTriaged();
        long overridden = ticketRepository.countAiOverridden();
        double agreementRate = triaged == 0 ? 0 : Math.round(1000.0 * (triaged - overridden) / triaged) / 10.0;

        return new DashboardStats(
                ticketRepository.count(),
                toMap(ticketRepository.countByStatus()),
                toMap(ticketRepository.countByCategory()),
                toMap(ticketRepository.countByPriority()),
                triaged,
                overridden,
                agreementRate,
                aiAssistant.activeProvider());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    /**
     * aiAgreementRate = % of AI-triaged tickets where agents kept the AI's category and priority.
     * A simple, honest way to show whether the AI feature is actually useful.
     */
    public record DashboardStats(
            long totalTickets,
            Map<String, Long> byStatus,
            Map<String, Long> byCategory,
            Map<String, Long> byPriority,
            long aiTriagedTickets,
            long aiOverriddenTickets,
            double aiAgreementRate,
            String aiProvider) {
    }
}

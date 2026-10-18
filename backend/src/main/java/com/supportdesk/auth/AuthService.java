package com.supportdesk.auth;

import com.supportdesk.auth.dto.AuthResponse;
import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.RegisterRequest;
import com.supportdesk.common.BadRequestException;
import com.supportdesk.security.JwtService;
import com.supportdesk.security.SecurityUser;
import com.supportdesk.user.Role;
import com.supportdesk.user.User;
import com.supportdesk.user.UserRepository;
import com.supportdesk.user.dto.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Public sign-up always creates a CUSTOMER. Only an admin can promote someone to AGENT or ADMIN. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), Role.CUSTOMER);
        userRepository.save(user);
        return tokenFor(new SecurityUser(user));
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException / DisabledException, which GlobalExceptionHandler maps to 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        return tokenFor((SecurityUser) authentication.getPrincipal());
    }

    private AuthResponse tokenFor(SecurityUser user) {
        User entity = userRepository.findById(user.getId()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(user), jwtService.getExpirationMinutes(),
                UserResponse.from(entity));
    }
}

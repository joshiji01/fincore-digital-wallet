package com.fincore.user.service;

import com.fincore.user.dto.AuthResponse;
import com.fincore.user.dto.LoginRequest;
import com.fincore.user.dto.RegisterRequest;
import com.fincore.user.dto.UserCreatedEvent;
import com.fincore.user.dto.UserResponse;
import com.fincore.user.entity.OutboxEvent;
import com.fincore.user.entity.Role;
import com.fincore.user.entity.User;
import com.fincore.user.repository.OutboxEventRepository;
import com.fincore.user.repository.UserRepository;
import com.fincore.user.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthService(
            UserRepository userRepository,
            OutboxEventRepository outboxEventRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordHash,
                Role.USER
        );

        User savedUser = userRepository.save(user);

        try {
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    savedUser.getId()
            );

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "UserCreated",
                    savedUser.getId(),
                    payload
            );

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create user event",
                    e
            );
        }

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        boolean validPassword =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!validPassword) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer");
    }
}
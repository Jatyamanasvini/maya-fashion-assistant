package com.maya.fashionassistant;

import com.maya.fashionassistant.dto.auth.AuthResponse;
import com.maya.fashionassistant.dto.auth.LoginRequest;
import com.maya.fashionassistant.dto.auth.RegisterRequest;
import com.maya.fashionassistant.entity.User;
import com.maya.fashionassistant.exception.BadRequestException;
import com.maya.fashionassistant.repository.UserRepository;
import com.maya.fashionassistant.security.JwtService;
import com.maya.fashionassistant.security.UserPrincipal;
import com.maya.fashionassistant.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void register_ShouldHashPassword_AndReturnToken() {
        RegisterRequest request = new RegisterRequest("Maya User", "user@example.com", "secret123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashedPassword");

        User savedUser = new User("Maya User", "user@example.com", "hashedPassword");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token-123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals(1L, response.getUserId());
        assertEquals("Maya User", response.getName());

        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WhenEmailAlreadyExists_ShouldThrowBadRequest() {
        RegisterRequest request = new RegisterRequest("Maya User", "user@example.com", "secret123");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        LoginRequest request = new LoginRequest("user@example.com", "secret123");
        User user = new User("Maya User", "user@example.com", "hashedPassword");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.create(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("jwt-login-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-login-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
    }
}

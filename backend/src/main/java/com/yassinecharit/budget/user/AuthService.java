package com.yassinecharit.budget.user;

import com.yassinecharit.budget.common.BadRequestException;
import com.yassinecharit.budget.security.JwtService;
import com.yassinecharit.budget.user.AuthDtos.AuthResponse;
import com.yassinecharit.budget.user.AuthDtos.LoginRequest;
import com.yassinecharit.budget.user.AuthDtos.RegisterRequest;
import com.yassinecharit.budget.user.AuthDtos.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("An account already exists with this email");
        }
        User user = users.save(new User(request.email().toLowerCase(),
                passwordEncoder.encode(request.password()), request.fullName().trim()));
        return new AuthResponse(jwtService.generateToken(user.getEmail()), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(user.getEmail()), UserResponse.from(user));
    }
}

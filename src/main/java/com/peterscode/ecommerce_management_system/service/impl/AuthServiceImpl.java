package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.model.dto.request.LoginRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.LoginResponse;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.security.JwtTokenProvider;
import com.peterscode.ecommerce_management_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Authenticate using Spring Security Manager
        // This will check the DB (or Redis Cache if already cached) for the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Set Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generate JWT Token
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtTokenProvider.generateToken(userDetails);

        // 4. Return Response
        return LoginResponse.builder()
                .token(jwt)
                .email(userDetails.getUsername())
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .build();
    }
}
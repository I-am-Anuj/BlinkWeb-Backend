package com.blinkweb.library.dto;

public record AuthResponse(
        boolean success,
        String token,
        AuthUserDto user
) {}

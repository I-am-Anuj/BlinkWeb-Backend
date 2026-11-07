package com.blinkweb.library.dto;

public record AuthUserDto(
        String id,
        String email,
        String role
) {}

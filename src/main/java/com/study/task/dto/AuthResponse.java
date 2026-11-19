package com.study.task.dto;

public record AuthResponse(String token, String accessToken, String refreshToken) {
}

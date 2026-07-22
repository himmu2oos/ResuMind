package com.portfolio.recruiterportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class ChatDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 1000, message = "Message too long (max 1000 characters)")
        private String message;
        private String sessionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatResponse {
        private String reply;
        private String sessionId;
        private long timestamp;
        private boolean rateLimited;
    }
}

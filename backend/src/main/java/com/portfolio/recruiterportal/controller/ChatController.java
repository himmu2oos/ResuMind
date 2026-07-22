package com.portfolio.recruiterportal.controller;

import com.portfolio.recruiterportal.dto.ChatDto;
import com.portfolio.recruiterportal.model.ChatMessage;
import com.portfolio.recruiterportal.repository.ChatMessageRepository;
import com.portfolio.recruiterportal.service.AiChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final AiChatService aiChatService;
    private final ChatMessageRepository chatRepo;
    private final int requestsPerHour;
    private final Map<String, AtomicInteger> rateLimitMap = new ConcurrentHashMap<>();

    public ChatController(AiChatService aiChatService,
                          ChatMessageRepository chatRepo,
                          @Value("${rate-limit.chat.requests-per-hour:30}") int requestsPerHour) {
        this.aiChatService = aiChatService;
        this.chatRepo = chatRepo;
        this.requestsPerHour = requestsPerHour;
    }

    @PostMapping
    public ResponseEntity<ChatDto.ChatResponse> chat(
            @Valid @RequestBody ChatDto.ChatRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        String sessionId = request.getSessionId() != null
            ? request.getSessionId()
            : UUID.randomUUID().toString();

        // Simple rate limiting
        AtomicInteger count = rateLimitMap.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > requestsPerHour) {
            return ResponseEntity.status(429).body(ChatDto.ChatResponse.builder()
                .reply("You've reached the message limit. Please try again later!")
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .rateLimited(true)
                .build());
        }

        // Save user message
        chatRepo.save(ChatMessage.builder()
            .sessionId(sessionId)
            .role(ChatMessage.Role.USER)
            .content(request.getMessage())
            .visitorIp(String.valueOf(clientIp.hashCode()))
            .build());

        // Get AI response
        String aiReply = aiChatService.chat(request.getMessage(), sessionId);

        // Save assistant response
        chatRepo.save(ChatMessage.builder()
            .sessionId(sessionId)
            .role(ChatMessage.Role.ASSISTANT)
            .content(aiReply)
            .build());

        return ResponseEntity.ok(ChatDto.ChatResponse.builder()
            .reply(aiReply)
            .sessionId(sessionId)
            .timestamp(System.currentTimeMillis())
            .rateLimited(false)
            .build());
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatRepo.findBySessionIdOrderByTimestampAsc(sessionId));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}

package com.portfolio.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.recruiterportal.config.RagConfig;
import com.portfolio.recruiterportal.model.*;
import com.portfolio.recruiterportal.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class AiChatService {

    private final ProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final WorkExperienceRepository workRepo;
    private final ProjectRepository projectRepo;
    private final ChatMessageRepository chatRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagConfig ragConfig;

    @Autowired(required = false)
    private VectorStoreService vectorStore;

    @Value("${ai.provider:demo}")
    private String aiProvider;

    @Value("${ai.api-key:demo}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.max-tokens:1000}")
    private int maxTokens;

    public AiChatService(ProfileRepository profileRepo, EducationRepository educationRepo,
                         WorkExperienceRepository workRepo, ProjectRepository projectRepo,
                         ChatMessageRepository chatRepo, RestTemplate restTemplate,
                         ObjectMapper objectMapper, RagConfig ragConfig) {
        this.profileRepo = profileRepo;
        this.educationRepo = educationRepo;
        this.workRepo = workRepo;
        this.projectRepo = projectRepo;
        this.chatRepo = chatRepo;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ragConfig = ragConfig;
    }

    public String chat(String userMessage, String sessionId) {
        try {
            // If RAG is enabled and vector store is available, use RAG pipeline
            if (ragConfig.isEnabled() && vectorStore != null) {
                return ragChat(userMessage, sessionId);
            }

            // Otherwise use original logic
            String systemPrompt = buildSystemPrompt();
            List<ChatMessage> history = chatRepo.findBySessionIdOrderByTimestampAsc(sessionId);

            return switch (aiProvider.toLowerCase()) {
                case "openai" -> callOpenAi(systemPrompt, history, userMessage);
                case "anthropic" -> callAnthropic(systemPrompt, history, userMessage);
                case "ollama" -> callOllama(systemPrompt, history, userMessage);
                default -> demoResponse(userMessage);
            };
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            return "I'm sorry, I'm having trouble responding right now. " +
                   "Please try again or explore the other sections of this portfolio.";
        }
    }

    /**
     * RAG-powered chat: retrieves relevant document chunks from ChromaDB,
     * builds a context-aware prompt, and sends to Ollama for generation.
     */
    private String ragChat(String userMessage, String sessionId) throws Exception {
        // 1. Retrieve relevant chunks from vector store
        List<String> relevantChunks = vectorStore.search(userMessage, 5);
        String documentContext = String.join("\n\n---\n\n", relevantChunks);

        // 2. Also include structured profile data for completeness
        String profileContext = buildSystemPrompt();

        // 3. Build the RAG system prompt
        String systemPrompt = """
                You are an AI assistant representing a job candidate on their personal portfolio website.
                Recruiters visit this site to learn about the candidate. Answer questions helpfully,
                professionally, and enthusiastically.
                
                RULES:
                - Use the DOCUMENT CONTEXT and PROFILE DATA below to answer questions.
                - If the answer is not in the context, say "I don't have that specific information, but feel free to reach out directly."
                - Be positive and highlight strengths, but stay honest. Never invent facts.
                - Keep answers concise (2-4 sentences for simple questions, more for detailed ones).
                - Never share sensitive info like SSN or exact salary.
                
                === DOCUMENT CONTEXT (from resume/documents) ===
                %s
                
                === PROFILE DATA ===
                %s
                """.formatted(documentContext, profileContext);

        // 4. Build messages with conversation history
        List<ChatMessage> history = chatRepo.findBySessionIdOrderByTimestampAsc(sessionId);

        String ollamaUrl = ragConfig.getOllama().getBaseUrl() + "/api/chat";
        String ollamaModel = ragConfig.getOllama().getModel();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage msg : history) {
            messages.add(Map.of("role",
                    msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant",
                    "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", ollamaModel,
                "messages", messages,
                "stream", false
        );

        ResponseEntity<String> response = restTemplate.postForEntity(ollamaUrl, body, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("message").path("content").asText();
    }

    /**
     * Demo mode — works without any AI API key.
     * Returns keyword-matched responses from the profile data.
     */
    private String demoResponse(String userMessage) {
        String msg = userMessage.toLowerCase();
        Profile profile = profileRepo.findAll().stream().findFirst().orElse(null);
        List<Education> education = educationRepo.findAllByOrderBySortOrderAsc();
        List<WorkExperience> work = workRepo.findAllByOrderBySortOrderAsc();
        List<Project> projects = projectRepo.findAllByOrderBySortOrderAsc();

        if (profile == null) return "Profile data is not loaded yet. Please try again.";

        if (msg.contains("skill") || msg.contains("tech") || msg.contains("stack")) {
            return String.format("%s is skilled in: %s. They are particularly strong in backend development with Java/Spring Boot and frontend with Angular/TypeScript.",
                profile.getFullName(), String.join(", ", profile.getSkills()));
        }
        if (msg.contains("education") || msg.contains("school") || msg.contains("degree") || msg.contains("university")) {
            StringBuilder sb = new StringBuilder("Here's the educational background:\n\n");
            for (Education e : education) {
                sb.append("• ").append(e.getDegree()).append(" from ").append(e.getInstitution());
                if (e.getGpa() != null) sb.append(" (GPA: ").append(e.getGpa()).append(")");
                sb.append("\n");
                if (e.getDescription() != null) sb.append("  ").append(e.getDescription()).append("\n");
            }
            return sb.toString();
        }
        if (msg.contains("experience") || msg.contains("work") || msg.contains("job") || msg.contains("career")) {
            StringBuilder sb = new StringBuilder("Here's the work experience:\n\n");
            for (WorkExperience w : work) {
                sb.append("• ").append(w.getJobTitle()).append(" at ").append(w.getCompany());
                sb.append(" (").append(w.getStartDate()).append(" – ").append(w.getEndDate() != null ? w.getEndDate() : "Present").append(")\n");
                sb.append("  ").append(w.getDescription()).append("\n\n");
            }
            return sb.toString();
        }
        if (msg.contains("project") || msg.contains("built") || msg.contains("github") || msg.contains("portfolio")) {
            StringBuilder sb = new StringBuilder("Here are the notable projects:\n\n");
            for (Project p : projects) {
                sb.append("• ").append(p.getName()).append(": ").append(p.getDescription()).append("\n");
                sb.append("  Tech: ").append(String.join(", ", p.getTechStack())).append("\n");
                if (p.getGithubUrl() != null) sb.append("  GitHub: ").append(p.getGithubUrl()).append("\n");
                sb.append("\n");
            }
            return sb.toString();
        }
        if (msg.contains("contact") || msg.contains("email") || msg.contains("reach") || msg.contains("hire")) {
            return String.format("You can reach %s at %s. They're also on LinkedIn at %s and GitHub at %s.",
                profile.getFullName(), profile.getEmail(), profile.getLinkedinUrl(), profile.getGithubUrl());
        }
        if (msg.contains("who") || msg.contains("about") || msg.contains("tell me") || msg.contains("introduce")) {
            return String.format("%s is a %s based in %s. %s\n\nThey have %d skills including %s. Check out the Experience and Projects sections for more detail!",
                profile.getFullName(), profile.getTitle(), profile.getLocation(), profile.getSummary(),
                profile.getSkills().size(), String.join(", ", profile.getSkills().subList(0, Math.min(5, profile.getSkills().size()))));
        }
        if (msg.contains("senior") || msg.contains("fit") || msg.contains("why") || msg.contains("good")) {
            return String.format("%s brings a strong combination of technical depth and practical experience. With expertise in %s, plus hands-on experience building production systems, they're well-suited for senior engineering roles. Check the Experience section for specific achievements!",
                profile.getFullName(), String.join(", ", profile.getSkills().subList(0, Math.min(4, profile.getSkills().size()))));
        }

        return String.format("Great question! %s is a %s with experience in %s. Feel free to ask me about their skills, work experience, education, or projects — or explore those sections directly in the navigation above!",
            profile.getFullName(), profile.getTitle(), String.join(", ", profile.getSkills().subList(0, Math.min(3, profile.getSkills().size()))));
    }

    private String buildSystemPrompt() {
        Profile profile = profileRepo.findAll().stream().findFirst().orElse(null);
        List<Education> education = educationRepo.findAllByOrderBySortOrderAsc();
        List<WorkExperience> work = workRepo.findAllByOrderBySortOrderAsc();
        List<Project> projects = projectRepo.findAllByOrderBySortOrderAsc();

        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are an AI assistant on a personal portfolio website. Recruiters visit this site to learn about the candidate.
            Answer questions helpfully, professionally, and enthusiastically.
            
            RULES:
            - Only share information provided below. Never invent facts.
            - Be positive and highlight strengths, but stay honest.
            - If you don't know something, say so and suggest they reach out directly.
            - Keep answers concise (2-4 sentences for simple questions).
            - Never share sensitive info like SSN or exact salary.
            
            """);

        if (profile != null) {
            sb.append("=== CANDIDATE PROFILE ===\n");
            sb.append("Name: ").append(profile.getFullName()).append("\n");
            sb.append("Title: ").append(profile.getTitle()).append("\n");
            sb.append("Location: ").append(profile.getLocation()).append("\n");
            sb.append("Summary: ").append(profile.getSummary()).append("\n");
            sb.append("Email: ").append(profile.getEmail()).append("\n");
            if (profile.getGithubUrl() != null) sb.append("GitHub: ").append(profile.getGithubUrl()).append("\n");
            if (profile.getLinkedinUrl() != null) sb.append("LinkedIn: ").append(profile.getLinkedinUrl()).append("\n");
            if (profile.getSkills() != null) sb.append("Skills: ").append(String.join(", ", profile.getSkills())).append("\n");
            if (profile.getLanguages() != null) sb.append("Languages: ").append(String.join(", ", profile.getLanguages())).append("\n");
            sb.append("\n");
        }

        if (!education.isEmpty()) {
            sb.append("=== EDUCATION ===\n");
            for (Education edu : education) {
                sb.append("- ").append(edu.getDegree()).append(" from ").append(edu.getInstitution());
                if (edu.getStartDate() != null) sb.append(" (").append(edu.getStartDate()).append(" to ").append(edu.getEndDate() != null ? edu.getEndDate() : "Present").append(")");
                if (edu.getGpa() != null) sb.append(" GPA: ").append(edu.getGpa());
                sb.append("\n");
                if (edu.getDescription() != null) sb.append("  ").append(edu.getDescription()).append("\n");
            }
            sb.append("\n");
        }

        if (!work.isEmpty()) {
            sb.append("=== WORK EXPERIENCE ===\n");
            for (WorkExperience w : work) {
                sb.append("- ").append(w.getJobTitle()).append(" at ").append(w.getCompany());
                if (w.getStartDate() != null) sb.append(" (").append(w.getStartDate()).append(" to ").append(w.getEndDate() != null ? w.getEndDate() : "Present").append(")");
                sb.append("\n");
                if (w.getDescription() != null) sb.append("  ").append(w.getDescription()).append("\n");
                if (w.getHighlights() != null) w.getHighlights().forEach(h -> sb.append("  * ").append(h).append("\n"));
                if (w.getTechnologies() != null) sb.append("  Tech: ").append(String.join(", ", w.getTechnologies())).append("\n");
            }
            sb.append("\n");
        }

        if (!projects.isEmpty()) {
            sb.append("=== PROJECTS ===\n");
            for (Project p : projects) {
                sb.append("- ").append(p.getName()).append(": ").append(p.getDescription());
                if (p.getTechStack() != null) sb.append(" [").append(String.join(", ", p.getTechStack())).append("]");
                if (p.getGithubUrl() != null) sb.append(" GitHub: ").append(p.getGithubUrl());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    // ── OpenAI ──
    private String callOpenAi(String systemPrompt, List<ChatMessage> history, String userMessage) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant", "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of("model", model, "messages", messages, "max_tokens", maxTokens, "temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    // ── Anthropic ──
    private String callAnthropic(String systemPrompt, List<ChatMessage> history, String userMessage) throws Exception {
        String url = "https://api.anthropic.com/v1/messages";

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant", "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of("model", "claude-sonnet-4-20250514", "system", systemPrompt, "messages", messages, "max_tokens", maxTokens);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("content").get(0).path("text").asText();
    }

    // ── Ollama (local, without RAG) ──
    private String callOllama(String systemPrompt, List<ChatMessage> history, String userMessage) throws Exception {
        String url = "http://localhost:11434/api/chat";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant", "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of("model", model, "messages", messages, "stream", false);
        ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("message").path("content").asText();
    }
}

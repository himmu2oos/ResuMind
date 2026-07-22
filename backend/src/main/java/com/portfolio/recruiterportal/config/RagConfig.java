package com.portfolio.recruiterportal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private boolean enabled = false;
    private String documentsPath = "./documents";
    private ChromaConfig chroma = new ChromaConfig();
    private OllamaConfig ollama = new OllamaConfig();

    @Data
    public static class ChromaConfig {
        private String host = "localhost";
        private int port = 8000;
        private String collectionName = "resume-embeddings";
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2";
        private String embeddingModel = "nomic-embed-text";
    }
}

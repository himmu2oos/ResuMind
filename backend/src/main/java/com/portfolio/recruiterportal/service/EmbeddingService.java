package com.portfolio.recruiterportal.service;

import com.portfolio.recruiterportal.config.RagConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class EmbeddingService {

    private final OllamaEmbeddingModel embeddingModel;

    public EmbeddingService(RagConfig config) {
        log.info("Initializing Ollama embedding model: {} at {}",
                config.getOllama().getEmbeddingModel(), config.getOllama().getBaseUrl());
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(config.getOllama().getBaseUrl())
                .modelName(config.getOllama().getEmbeddingModel())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    public List<Float> embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return toFloatList(embedding.vector());
    }

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) {
            list.add(f);
        }
        return list;
    }
}

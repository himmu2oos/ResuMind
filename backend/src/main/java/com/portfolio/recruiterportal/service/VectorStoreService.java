package com.portfolio.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.recruiterportal.config.RagConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * ChromaDB integration via REST API — no external Java client needed.
 * API docs: https://docs.trychroma.com/reference/js-client/Collection
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class VectorStoreService {

    private final RagConfig config;
    private final EmbeddingService embeddingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String collectionId;
    private String baseUrl;

    public VectorStoreService(RagConfig config, EmbeddingService embeddingService,
                              RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.config = config;
        this.embeddingService = embeddingService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws Exception {
        this.baseUrl = "http://" + config.getChroma().getHost() + ":" + config.getChroma().getPort();
        log.info("Connecting to ChromaDB at: {}", baseUrl);

        try {
            // Check heartbeat
            restTemplate.getForObject(baseUrl + "/api/v2/heartbeat", String.class);
            log.info("ChromaDB heartbeat OK");

            // Create or get collection
            String collectionName = config.getChroma().getCollectionName();
            String createUrl = baseUrl + "/api/v2/tenants/default_tenant/databases/default_database/collections";

            Map<String, Object> body = new HashMap<>();
            body.put("name", collectionName);
            body.put("get_or_create", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    createUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            this.collectionId = root.path("id").asText();
            log.info("ChromaDB collection '{}' ready (id: {})", collectionName, collectionId);

        } catch (Exception e) {
            log.error("Failed to connect to ChromaDB: {}. RAG will be disabled.", e.getMessage());
            throw e;
        }
    }

    public void addDocuments(List<String> chunks, List<String> ids,
                             List<Map<String, String>> metadata) throws Exception {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String chunk : chunks) {
            embeddings.add(embeddingService.embed(chunk));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("documents", chunks);
        body.put("embeddings", embeddings);
        body.put("metadatas", metadata);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.exchange(
                baseUrl + "/api/v2/tenants/default_tenant/databases/default_database/collections/" + collectionId + "/add",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
    }

    public List<String> search(String query, int topK) throws Exception {
        List<Float> queryEmbedding = embeddingService.embed(query);

        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(queryEmbedding));
        body.put("n_results", topK);
        body.put("include", List.of("documents"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v2/tenants/default_tenant/databases/default_database/collections/" + collectionId + "/query",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        List<String> results = new ArrayList<>();

        JsonNode documents = root.path("documents");
        if (documents.isArray() && !documents.isEmpty()) {
            for (JsonNode docList : documents) {
                for (JsonNode doc : docList) {
                    results.add(doc.asText());
                }
            }
        }

        return results;
    }
}
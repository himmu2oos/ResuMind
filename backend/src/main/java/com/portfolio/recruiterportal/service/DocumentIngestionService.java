package com.portfolio.recruiterportal.service;

import com.portfolio.recruiterportal.config.RagConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class DocumentIngestionService {

    private final RagConfig config;
    private final DocumentParser parser;
    private final TextChunker chunker;
    private final VectorStoreService vectorStore;

    public DocumentIngestionService(RagConfig config, DocumentParser parser,
                                    TextChunker chunker, VectorStoreService vectorStore) {
        this.config = config;
        this.parser = parser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestDocumentsOnStartup() {
        log.info("Starting document ingestion from: {}", config.getDocumentsPath());

        File documentsDir = new File(config.getDocumentsPath());
        if (!documentsDir.exists() || !documentsDir.isDirectory()) {
            log.warn("Documents directory does not exist: {}. Creating it...", config.getDocumentsPath());
            documentsDir.mkdirs();
            log.info("Place your resume (PDF/DOCX/TXT) in: {}", documentsDir.getAbsolutePath());
            return;
        }

        File[] files = documentsDir.listFiles((dir, name) ->
                name.endsWith(".pdf") || name.endsWith(".docx") ||
                name.endsWith(".txt") || name.endsWith(".md"));

        if (files == null || files.length == 0) {
            log.warn("No documents found in: {}. Place your resume there and restart.", config.getDocumentsPath());
            return;
        }

        int totalChunks = 0;
        for (File file : files) {
            try {
                log.info("Processing: {}", file.getName());
                String content = parser.parseFile(file);
                List<String> chunks = chunker.chunk(content);

                List<String> ids = new ArrayList<>();
                List<Map<String, String>> metadata = new ArrayList<>();

                for (int i = 0; i < chunks.size(); i++) {
                    ids.add(file.getName() + "_chunk_" + i);
                    metadata.add(Map.of(
                            "source", file.getName(),
                            "chunk_index", String.valueOf(i)
                    ));
                }

                vectorStore.addDocuments(chunks, ids, metadata);
                totalChunks += chunks.size();
                log.info("Ingested {} chunks from {}", chunks.size(), file.getName());

            } catch (Exception e) {
                log.error("Failed to process {}: {}", file.getName(), e.getMessage());
            }
        }

        log.info("Document ingestion complete. Total chunks indexed: {}", totalChunks);
    }
}

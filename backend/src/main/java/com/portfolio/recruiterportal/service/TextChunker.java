package com.portfolio.recruiterportal.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > CHUNK_SIZE && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                String overlap = current.substring(Math.max(0, current.length() - CHUNK_OVERLAP));
                current = new StringBuilder(overlap);
            }
            current.append(sentence).append(" ");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}

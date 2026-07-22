package com.portfolio.recruiterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.username:}")
    private String githubUsername;

    @Value("${github.token:}")
    private String githubToken;

    private static final String GITHUB_API = "https://api.github.com";

    public List<Map<String, Object>> getUserRepos() {
        try {
            String url = GITHUB_API + "/users/" + githubUsername + "/repos?sort=updated&per_page=30";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildRequest(), String.class);
            JsonNode repos = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode repo : repos) {
                if (!repo.path("fork").asBoolean()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", repo.path("name").asText());
                    item.put("description", repo.path("description").asText(""));
                    item.put("url", repo.path("html_url").asText());
                    item.put("stars", repo.path("stargazers_count").asInt());
                    item.put("forks", repo.path("forks_count").asInt());
                    item.put("language", repo.path("language").asText(""));
                    item.put("updatedAt", repo.path("updated_at").asText());
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("GitHub API error: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getRepoDetails(String repoName) {
        try {
            String url = GITHUB_API + "/repos/" + githubUsername + "/" + repoName;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildRequest(), String.class);
            JsonNode repo = objectMapper.readTree(response.getBody());

            String langUrl = url + "/languages";
            ResponseEntity<String> langResponse = restTemplate.exchange(langUrl, HttpMethod.GET, buildRequest(), String.class);
            JsonNode languages = objectMapper.readTree(langResponse.getBody());
            Map<String, Object> langMap = new LinkedHashMap<>();
            languages.fields().forEachRemaining(entry -> langMap.put(entry.getKey(), entry.getValue().asInt()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", repo.path("name").asText());
            result.put("description", repo.path("description").asText(""));
            result.put("url", repo.path("html_url").asText());
            result.put("homepage", repo.path("homepage").asText(""));
            result.put("stars", repo.path("stargazers_count").asInt());
            result.put("forks", repo.path("forks_count").asInt());
            result.put("language", repo.path("language").asText(""));
            result.put("languages", langMap);
            result.put("createdAt", repo.path("created_at").asText());
            result.put("updatedAt", repo.path("updated_at").asText());
            return result;
        } catch (Exception e) {
            log.error("GitHub repo fetch error for {}: {}", repoName, e.getMessage());
            return Map.of("error", "Repository not found");
        }
    }

    private HttpEntity<Void> buildRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        return new HttpEntity<>(headers);
    }
}

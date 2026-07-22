package com.portfolio.recruiterportal.controller;

import com.portfolio.recruiterportal.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;

    @GetMapping("/repos")
    public ResponseEntity<List<Map<String, Object>>> getRepos() {
        return ResponseEntity.ok(gitHubService.getUserRepos());
    }

    @GetMapping("/repos/{repoName}")
    public ResponseEntity<Map<String, Object>> getRepoDetails(@PathVariable String repoName) {
        return ResponseEntity.ok(gitHubService.getRepoDetails(repoName));
    }
}

package com.portfolio.recruiterportal.controller;

import com.portfolio.recruiterportal.model.*;
import com.portfolio.recruiterportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final WorkExperienceRepository workRepo;
    private final ProjectRepository projectRepo;

    @GetMapping("/profile")
    public ResponseEntity<Profile> getProfile() {
        List<Profile> profiles = profileRepo.findAll();
        if (profiles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profiles.get(0));
    }


    @GetMapping("/education")
    public ResponseEntity<List<Education>> getEducation() {
        return ResponseEntity.ok(educationRepo.findAllByOrderBySortOrderAsc());
    }

    @GetMapping("/work-history")
    public ResponseEntity<List<WorkExperience>> getWorkHistory() {
        return ResponseEntity.ok(workRepo.findAllByOrderBySortOrderAsc());
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjects(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            var projectsByCategory = projectRepo.findByCategoryOrderBySortOrderAsc(category);
            return ResponseEntity.ok(projectsByCategory);
        }
        var projects = projectRepo.findAllByOrderBySortOrderAsc();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/featured")
    public ResponseEntity<List<Project>> getFeaturedProjects() {
        return ResponseEntity.ok(projectRepo.findByFeaturedTrueOrderBySortOrderAsc());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return projectRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

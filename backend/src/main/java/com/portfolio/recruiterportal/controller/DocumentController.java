package com.portfolio.recruiterportal.controller;

import com.portfolio.recruiterportal.model.*;
import com.portfolio.recruiterportal.repository.*;
import com.portfolio.recruiterportal.service.ResumeParserService;
import com.portfolio.recruiterportal.service.ResumeParserService.ParsedResume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * REST endpoint for uploading a resume document.
 * On upload, the file is saved to the documents directory, parsed,
 * and the database is re-populated with the extracted data.
 */
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final ResumeParserService resumeParser;
    private final ProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final WorkExperienceRepository workRepo;
    private final ProjectRepository projectRepo;

    @Value("${resume.documents-path:./documents}")
    private String documentsPath;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No file provided"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) filename = "resume";
        String lower = filename.toLowerCase();

        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".txt")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message",
                            "Unsupported file type. Please upload a PDF, DOCX, or TXT file."));
        }

        try {
            // Ensure documents directory exists
            Path dirPath = Path.of(documentsPath);
            Files.createDirectories(dirPath);

            // Save the uploaded file
            Path filePath = dirPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved uploaded resume: {}", filePath);

            // Parse the document
            File savedFile = filePath.toFile();
            ParsedResume parsed = resumeParser.parse(savedFile);

            // Clear existing data
            projectRepo.deleteAll();
            workRepo.deleteAll();
            educationRepo.deleteAll();
            profileRepo.deleteAll();

            // Save parsed data
            int profileCount = 0, eduCount = 0, workCount = 0, projectCount = 0;

            if (parsed.getProfile() != null) {
                Profile profile = parsed.getProfile();
                if (profile.getFullName() == null || profile.getFullName().isBlank()) {
                    profile.setFullName("Your Name");
                }
                profileRepo.save(profile);
                profileCount = 1;
            }

            if (parsed.getEducation() != null) {
                for (Education edu : parsed.getEducation()) {
                    if (edu.getInstitution() != null && edu.getDegree() != null) {
                        educationRepo.save(edu);
                        eduCount++;
                    }
                }
            }

            if (parsed.getWorkExperience() != null) {
                for (WorkExperience work : parsed.getWorkExperience()) {
                    if (work.getCompany() != null && work.getJobTitle() != null) {
                        workRepo.save(work);
                        workCount++;
                    }
                }
            }

            if (parsed.getProjects() != null) {
                for (Project project : parsed.getProjects()) {
                    if (project.getName() != null) {
                        projectRepo.save(project);
                        projectCount++;
                    }
                }
            }

            log.info("Re-seeded database from uploaded resume: {} profile, {} education, {} work, {} projects",
                    profileCount, eduCount, workCount, projectCount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resume uploaded and parsed successfully",
                    "filename", filename,
                    "profileCount", profileCount,
                    "educationCount", eduCount,
                    "workExperienceCount", workCount,
                    "projectCount", projectCount
            ));

        } catch (IOException e) {
            log.error("Failed to save uploaded file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to save file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse uploaded resume: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to parse resume: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDocumentStatus() {
        File dir = new File(documentsPath);
        boolean hasDocuments = false;
        String currentFile = null;

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> {
                String l = name.toLowerCase();
                return l.endsWith(".pdf") || l.endsWith(".docx") || l.endsWith(".txt");
            });
            if (files != null && files.length > 0) {
                hasDocuments = true;
                currentFile = files[0].getName();
            }
        }

        return ResponseEntity.ok(Map.of(
                "hasDocuments", hasDocuments,
                "currentFile", currentFile != null ? currentFile : "",
                "documentsPath", dir.getAbsolutePath(),
                "profileCount", profileRepo.count(),
                "educationCount", educationRepo.count(),
                "workExperienceCount", workRepo.count(),
                "projectCount", projectRepo.count()
        ));
    }
}

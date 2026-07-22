package com.portfolio.recruiterportal.config;

import com.portfolio.recruiterportal.model.*;
import com.portfolio.recruiterportal.repository.*;
import com.portfolio.recruiterportal.service.ResumeParserService;
import com.portfolio.recruiterportal.service.ResumeParserService.ParsedResume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Seeds the database on startup.
 *
 * Strategy:
 *   1. Look for a resume document (PDF/DOCX/TXT) in the configured documents directory.
 *   2. If found → parse it with ResumeParserService and populate the database.
 *   3. If not found → fall back to hardcoded placeholder data so the app still works.
 *
 * To use your own resume, place your PDF or Word document in backend/documents/
 * and restart the application. The file name does not matter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final WorkExperienceRepository workRepo;
    private final ProjectRepository projectRepo;
    private final ResumeParserService resumeParser;

    @Value("${resume.documents-path:./documents}")
    private String documentsPath;

    @Override
    public void run(String... args) {
        if (profileRepo.count() > 0) {
            log.info("Data already seeded, skipping.");
            return;
        }

        // Try to find and parse a resume document
        if (seedFromDocument()) {
            log.info("Database seeded successfully from resume document!");
            return;
        }

        // Fallback to hardcoded data
        log.info("No resume document found in '{}'. Seeding with placeholder data...", documentsPath);
        seedFallbackData();
        log.info("Database seeded with placeholder data. Place your resume in '{}' and restart to use your own data.", documentsPath);
    }

    /**
     * Scan the documents directory for a resume file, parse it, and seed the DB.
     * @return true if a document was found and successfully parsed.
     */
    private boolean seedFromDocument() {
        File dir = new File(documentsPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Documents directory not found: {}", documentsPath);
            return false;
        }

        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".pdf") || lower.endsWith(".docx")
                    || lower.endsWith(".doc") || lower.endsWith(".txt");
        });

        if (files == null || files.length == 0) {
            log.warn("No resume documents (PDF/DOCX/TXT) found in: {}", dir.getAbsolutePath());
            return false;
        }

        // Use the first document found (prefer PDF, then DOCX, then TXT)
        File resumeFile = selectBestFile(files);
        log.info("Found resume document: {}", resumeFile.getName());

        try {
            ParsedResume parsed = resumeParser.parse(resumeFile);

            // Save profile
            if (parsed.getProfile() != null) {
                Profile profile = parsed.getProfile();
                if (profile.getFullName() == null || profile.getFullName().isBlank()) {
                    profile.setFullName("Your Name");
                }
                if (profile.getSummary() == null || profile.getSummary().isBlank()){
                    profile.setSummary("I am a Software Engineer and Integration Developer specializing in cloud architecture and artificial intelligence. " +
                            "Currently, at Old Republic Aerospace, I architect high-throughput data solutions using the Azure ecosystem, where I've successfully reduced manual processing times by 75%. " +
                            "With a Master’s in Computer Science from Drexel University, my expertise bridges the gap between robust backend integration (Java/.NET) and cutting-edge Machine Learning. " +
                            "From building AI chatbots to developing computer vision algorithms for image processing, I focus on creating efficient, scalable, and data-driven applications.");
                }
                profileRepo.save(profile);
                log.info("Saved profile: {}", profile.getFullName());
            }

            // Save education
            if (parsed.getEducation() != null && !parsed.getEducation().isEmpty()) {
                for (Education edu : parsed.getEducation()) {
                    if (edu.getInstitution() != null && edu.getDegree() != null) {
                        educationRepo.save(edu);
                    }
                }
                log.info("Saved {} education entries", parsed.getEducation().size());
            }

            // Save work experience
            if (parsed.getWorkExperience() != null && !parsed.getWorkExperience().isEmpty()) {
                for (WorkExperience work : parsed.getWorkExperience()) {
                    if (work.getCompany() != null && work.getJobTitle() != null) {
                        workRepo.save(work);
                    }
                }
                log.info("Saved {} work experience entries", parsed.getWorkExperience().size());
            }

            // Save projects
            if (parsed.getProjects() != null && !parsed.getProjects().isEmpty()) {
                for (Project project : parsed.getProjects()) {
                    if (project.getName() != null) {
                        projectRepo.save(project);
                    }
                }
                log.info("Saved {} project entries", parsed.getProjects().size());
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to parse resume document '{}': {}", resumeFile.getName(), e.getMessage(), e);
            return false;
        }
    }

    private File selectBestFile(File[] files) {
        File pdf = null, docx = null, txt = null;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".pdf") && pdf == null) pdf = f;
            else if (name.endsWith(".docx") && docx == null) docx = f;
            else if (name.endsWith(".txt") && txt == null) txt = f;
        }
        if (pdf != null) return pdf;
        if (docx != null) return docx;
        if (txt != null) return txt;
        return files[0];
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FALLBACK — Placeholder data (used only when no document is found)
    // ═══════════════════════════════════════════════════════════════════════

    private void seedFallbackData() {
        profileRepo.save(Profile.builder()
            .fullName("Your Full Name")
            .title("Full Stack Software Engineer")
            .summary("Passionate software engineer with 4+ years of experience building " +
                     "scalable web applications. Skilled in Java, Angular, TypeScript, and " +
                     "cloud technologies. I love solving complex problems and building " +
                     "products that make a difference.")
            .email("your.email@example.com")
            .phone("+1-555-123-4567")
            .location("Atlanta, GA")
            .photoUrl("/assets/profile.jpg")
            .linkedinUrl("https://linkedin.com/in/your-profile")
            .githubUrl("https://github.com/your-username")
            .portfolioUrl("https://yourname.dev")
            .skills(List.of(
                "Java", "Spring Boot", "Angular", "TypeScript", "Python",
                "PostgreSQL", "MongoDB", "Docker", "AWS", "Git",
                "REST APIs", "Microservices", "CI/CD", "Agile/Scrum"
            ))
            .languages(List.of("English", "Spanish"))
            .build());

        educationRepo.save(Education.builder()
            .institution("Georgia Institute of Technology")
            .degree("Bachelor of Science in Computer Science")
            .fieldOfStudy("Computer Science")
            .startDate("2018-08")
            .endDate("2022-05")
            .gpa(3.7)
            .description("Concentration in Systems & Architecture. Dean's List. " +
                         "Senior capstone project on distributed systems.")
            .sortOrder(1)
            .build());

        educationRepo.save(Education.builder()
            .institution("Sandy Springs High School")
            .degree("High School Diploma")
            .startDate("2014-08")
            .endDate("2018-05")
            .gpa(3.9)
            .description("Valedictorian. AP Computer Science, AP Calculus.")
            .sortOrder(2)
            .build());

        workRepo.save(WorkExperience.builder()
            .company("Tech Company Inc.")
            .jobTitle("Senior Software Engineer")
            .location("Atlanta, GA")
            .startDate("2023-01")
            .endDate(null)
            .isCurrent(true)
            .description("Lead backend development for the core platform team, " +
                         "building microservices that handle 10M+ requests per day.")
            .highlights(List.of(
                "Designed real-time notification system reducing latency by 60%",
                "Led migration from monolith to microservices serving 2M users",
                "Mentored 3 junior engineers through code reviews and pair programming",
                "Implemented CI/CD pipelines reducing deploy time from 2hrs to 15min"
            ))
            .technologies(List.of("Java", "Spring Boot", "Kubernetes", "PostgreSQL", "Kafka", "AWS"))
            .sortOrder(1)
            .build());

        workRepo.save(WorkExperience.builder()
            .company("Startup Co.")
            .jobTitle("Software Engineer")
            .location("Remote")
            .startDate("2022-06")
            .endDate("2022-12")
            .isCurrent(false)
            .description("Full stack development for an early-stage SaaS platform " +
                         "in the HR tech space.")
            .highlights(List.of(
                "Built Angular frontend from scratch, delivering MVP in 3 months",
                "Integrated Stripe, SendGrid, and Twilio APIs",
                "Achieved 85% test coverage with unit and integration tests"
            ))
            .technologies(List.of("Angular", "TypeScript", "Node.js", "MongoDB", "Docker"))
            .sortOrder(2)
            .build());

        projectRepo.save(Project.builder()
            .name("AI Recruiter Portal")
            .description("This website! An AI-powered personal portfolio that lets " +
                         "recruiters interactively learn about me via an AI chatbot.")
            .githubUrl("https://github.com/your-username/recruiter-portal")
            .liveUrl("https://yourname.dev")
            .techStack(List.of("Angular", "TypeScript", "Java", "Spring Boot", "PostgreSQL", "OpenAI"))
            .category("Full Stack")
            .featured(true)
            .stars(12)
            .sortOrder(1)
            .build());

        projectRepo.save(Project.builder()
            .name("E-Commerce Platform")
            .description("Full-featured e-commerce app with product catalog, cart, " +
                         "payment processing, and order management.")
            .githubUrl("https://github.com/your-username/ecommerce-platform")
            .techStack(List.of("Java", "Spring Boot", "Angular", "Stripe", "PostgreSQL", "Redis"))
            .category("Full Stack")
            .featured(true)
            .stars(8)
            .sortOrder(2)
            .build());

        projectRepo.save(Project.builder()
            .name("Real-Time Chat App")
            .description("WebSocket-based chat with rooms, DMs, typing indicators, " +
                         "and message persistence.")
            .githubUrl("https://github.com/your-username/chat-app")
            .techStack(List.of("Angular", "TypeScript", "Spring Boot", "WebSocket", "MongoDB"))
            .category("Full Stack")
            .featured(true)
            .stars(15)
            .sortOrder(3)
            .build());

        projectRepo.save(Project.builder()
            .name("ML Sentiment Analyzer")
            .description("ML model that analyzes customer review sentiment with 92% accuracy.")
            .githubUrl("https://github.com/your-username/sentiment-analyzer")
            .techStack(List.of("Python", "TensorFlow", "Flask", "Docker"))
            .category("ML/AI")
            .featured(false)
            .sortOrder(4)
            .build());
    }
}

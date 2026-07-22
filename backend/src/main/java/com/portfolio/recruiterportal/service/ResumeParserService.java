package com.portfolio.recruiterportal.service;

import com.portfolio.recruiterportal.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a resume document (PDF or DOCX) and extracts structured portfolio data.
 *
 * Extraction strategy:
 *   1. Parse raw text from the document using DocumentParser.
 *   2. Split text into logical sections (Education, Experience, Projects, Skills, etc.)
 *      using common resume section headers.
 *   3. Within each section, apply regex and heuristic rules to build model objects.
 *
 * This is intentionally rule-based (no AI calls) so it works offline and instantly.
 * The patterns cover the most common resume formats. Users can adjust section headers
 * or parsing logic if their resume uses an unusual layout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final DocumentParser documentParser;

    // ── Section header patterns (case-insensitive) ──────────────────────────
    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^\\s*(EDUCATION|EXPERIENCE|WORK EXPERIENCE|PROFESSIONAL EXPERIENCE|EMPLOYMENT|" +
            "PROJECTS|PERSONAL PROJECTS|TECHNICAL PROJECTS|ACADEMIC PROJECTS|" +
            "SKILLS|TECHNICAL SKILLS|CORE COMPETENCIES|" +
            "SUMMARY|PROFESSIONAL SUMMARY|OBJECTIVE|PROFILE|ABOUT|" +
            "CONTACT|CONTACT INFORMATION|" +
            "LANGUAGES|CERTIFICATIONS|CERTIFICATES|AWARDS|HONORS|" +
            "PUBLICATIONS|VOLUNTEER|INTERESTS)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    // Email / phone / LinkedIn / GitHub patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?1[\\-\\s.]?)?\\(?\\d{3}\\)?[\\-\\s.]?\\d{3}[\\-\\s.]?\\d{4}");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w\\-]+/?");
    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?github\\.com/[\\w\\-]+/?");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    // Date range: "Aug 2018 – May 2022", "2018-08 - 2022-05", "Jan 2023 - Present"
    private static final Pattern DATE_RANGE = Pattern.compile(
            "((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?\\s+\\d{4}|\\d{4}[\\-/]\\d{2}|\\d{4})" +
            "\\s*[–\\-—to]+\\s*" +
            "((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?\\s+\\d{4}|\\d{4}[\\-/]\\d{2}|\\d{4}|Present|Current)",
            Pattern.CASE_INSENSITIVE);
    // GPA: "GPA: 3.7", "GPA 3.70/4.0"
    private static final Pattern GPA_PATTERN = Pattern.compile(
            "GPA[:\\s]+([0-9]+\\.[0-9]+)", Pattern.CASE_INSENSITIVE);

    // ── Public entry point ──────────────────────────────────────────────────

    /**
     * Parse a resume file and return all extracted entities.
     */
    public ParsedResume parse(File file) throws IOException {
        String rawText = documentParser.parseFile(file);
        log.info("Parsed {} characters from {}", rawText.length(), file.getName());

        Map<String, String> sections = splitIntoSections(rawText);
        log.info("Identified sections: {}", sections.keySet());

        ParsedResume result = new ParsedResume();
        result.setProfile(extractProfile(rawText, sections));
        result.setEducation(extractEducation(sections));
        result.setWorkExperience(extractWorkExperience(sections));
        result.setProjects(extractProjects(sections));

        return result;
    }

    // ── Section splitting ───────────────────────────────────────────────────

    private Map<String, String> splitIntoSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher m = SECTION_HEADER.matcher(text);

        List<int[]> headerPositions = new ArrayList<>();
        List<String> headerNames = new ArrayList<>();

        while (m.find()) {
            headerPositions.add(new int[]{m.start(), m.end()});
            headerNames.add(m.group(1).trim().toUpperCase());
        }

        // Everything before the first section header is the "HEADER" (name, contact info)
        if (!headerPositions.isEmpty()) {
            String headerText = text.substring(0, headerPositions.get(0)[0]).trim();
            if (!headerText.isEmpty()) {
                sections.put("HEADER", headerText);
            }
        } else {
            // No section headers found — treat entire text as HEADER
            sections.put("HEADER", text);
            return sections;
        }

        for (int i = 0; i < headerPositions.size(); i++) {
            int contentStart = headerPositions.get(i)[1];
            int contentEnd = (i + 1 < headerPositions.size())
                    ? headerPositions.get(i + 1)[0]
                    : text.length();
            String content = text.substring(contentStart, contentEnd).trim();

            // Normalise similar section names
            String name = normaliseSectionName(headerNames.get(i));
            // If there's already content for that name, append
            sections.merge(name, content, (a, b) -> a + "\n" + b);
        }

        return sections;
    }

    private String normaliseSectionName(String raw) {
        return switch (raw) {
            case "WORK EXPERIENCE", "PROFESSIONAL EXPERIENCE", "EMPLOYMENT" -> "EXPERIENCE";
            case "TECHNICAL SKILLS", "CORE COMPETENCIES" -> "SKILLS";
            case "PROFESSIONAL SUMMARY", "OBJECTIVE", "PROFILE", "ABOUT" -> "SUMMARY";
            case "CONTACT INFORMATION" -> "CONTACT";
            case "PERSONAL PROJECTS", "TECHNICAL PROJECTS", "ACADEMIC PROJECTS" -> "PROJECTS";
            case "CERTIFICATIONS", "CERTIFICATES" -> "CERTIFICATIONS";
            case "HONORS" -> "AWARDS";
            default -> raw;
        };
    }

    // ── Profile extraction ──────────────────────────────────────────────────

    private Profile extractProfile(String fullText, Map<String, String> sections) {
        String header = sections.getOrDefault("HEADER", "");
        String summarySection = sections.getOrDefault("SUMMARY", "");
        String skillsSection = sections.getOrDefault("SKILLS", "");
        String languagesSection = sections.getOrDefault("LANGUAGES", "");
        String contactSection = sections.getOrDefault("CONTACT", "");
        String searchArea = header + "\n" + contactSection;

        Profile.ProfileBuilder pb = Profile.builder();

        // Name: first non-empty line of the header
        String[] headerLines = header.split("\\n");
        for (String line : headerLines) {
            String cleaned = line.trim();
            if (!cleaned.isEmpty() && !EMAIL_PATTERN.matcher(cleaned).find()
                    && !PHONE_PATTERN.matcher(cleaned).find()
                    && !URL_PATTERN.matcher(cleaned).find()) {
                pb.fullName(cleaned);
                break;
            }
        }

        // Title: second meaningful line (often "Software Engineer" etc.)
        int titleLine = 0;
        for (String line : headerLines) {
            String cleaned = line.trim();
            if (cleaned.isEmpty() || EMAIL_PATTERN.matcher(cleaned).find()
                    || PHONE_PATTERN.matcher(cleaned).find()
                    || URL_PATTERN.matcher(cleaned).find()) continue;
            titleLine++;
            if (titleLine == 2) {
                pb.title(cleaned);
                break;
            }
        }

        // Contact info from header + contact section
        pb.email(findFirst(EMAIL_PATTERN, searchArea));
        pb.phone(findFirst(PHONE_PATTERN, searchArea));
        pb.linkedinUrl(findFirst(LINKEDIN_PATTERN, searchArea));
        pb.githubUrl(findFirst(GITHUB_PATTERN, searchArea));

        // Location: look for common patterns like "City, ST" in header lines
        for (String line : searchArea.split("\\n")) {
            String l = line.trim();
            if (l.matches(".*[A-Z][a-z]+,\\s*[A-Z]{2}.*") && l.length() < 80) {
                // Extract just the "City, ST" portion
                Matcher locM = Pattern.compile("([A-Z][a-z]+(?:\\s[A-Z][a-z]+)*,\\s*[A-Z]{2})").matcher(l);
                if (locM.find()) {
                    pb.location(locM.group(1));
                    break;
                }
            }
        }

        // Summary
        if (!summarySection.isEmpty()) {
            pb.summary(summarySection.replaceAll("\\s+", " ").trim());
        }

        // Skills: split on commas, pipes, bullets, or newlines
        pb.skills(parseList(skillsSection));

        // Languages
        pb.languages(parseList(languagesSection));

        return pb.build();
    }

    // ── Education extraction ────────────────────────────────────────────────

    private List<Education> extractEducation(Map<String, String> sections) {
        String text = sections.getOrDefault("EDUCATION", "");
        if (text.isEmpty()) return Collections.emptyList();

        List<Education> results = new ArrayList<>();
        // Split on date ranges or blank lines to separate entries
        List<String> entries = splitEntries(text);

        int order = 1;
        for (String entry : entries) {
            if (entry.trim().length() < 10) continue;

            Education.EducationBuilder eb = Education.builder();
            String[] lines = entry.trim().split("\\n");

            // First non-empty line is usually the institution
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    eb.institution(line.trim());
                    break;
                }
            }

            // Look for degree line
            for (String line : lines) {
                String l = line.trim();
                if (l.toLowerCase().contains("bachelor") || l.toLowerCase().contains("master")
                        || l.toLowerCase().contains("associate") || l.toLowerCase().contains("ph.d")
                        || l.toLowerCase().contains("diploma") || l.toLowerCase().contains("b.s.")
                        || l.toLowerCase().contains("b.a.") || l.toLowerCase().contains("m.s.")
                        || l.toLowerCase().contains("m.a.") || l.toLowerCase().contains("b.tech")
                        || l.toLowerCase().contains("m.tech") || l.toLowerCase().contains("b.e.")
                        || l.toLowerCase().contains("m.e.") || l.toLowerCase().contains("mba")) {
                    eb.degree(l);
                    break;
                }
            }
            if (eb.build().getDegree() == null) {
                // Fallback: second line as degree
                if (lines.length > 1) eb.degree(lines[1].trim());
            }

            // Date range
            Matcher dm = DATE_RANGE.matcher(entry);
            if (dm.find()) {
                eb.startDate(normalizeDate(dm.group(1)));
                String end = dm.group(2);
                if (end.equalsIgnoreCase("Present") || end.equalsIgnoreCase("Current")) {
                    eb.endDate(null);
                } else {
                    eb.endDate(normalizeDate(end));
                }
            }

            // GPA
            Matcher gm = GPA_PATTERN.matcher(entry);
            if (gm.find()) {
                try { eb.gpa(Double.parseDouble(gm.group(1))); } catch (NumberFormatException ignored) {}
            }

            // Description: any remaining lines that aren't institution/degree/date
            StringBuilder desc = new StringBuilder();
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;
                if (l.equals(eb.build().getInstitution())) continue;
                if (l.equals(eb.build().getDegree())) continue;
                if (DATE_RANGE.matcher(l).find()) continue;
                if (GPA_PATTERN.matcher(l).find()) continue;
                if (l.startsWith("•") || l.startsWith("-") || l.startsWith("●")) {
                    desc.append(l.replaceFirst("^[•\\-●]\\s*", "")).append(". ");
                }
            }
            if (!desc.isEmpty()) {
                eb.description(desc.toString().trim());
            }

            eb.sortOrder(order++);
            results.add(eb.build());
        }

        return results;
    }

    // ── Work experience extraction ──────────────────────────────────────────

    private List<WorkExperience> extractWorkExperience(Map<String, String> sections) {
        String text = sections.getOrDefault("EXPERIENCE", "");
        if (text.isEmpty()) return Collections.emptyList();

        List<WorkExperience> results = new ArrayList<>();
        List<String> entries = splitEntries(text);

        int order = 1;
        for (String entry : entries) {
            if (entry.trim().length() < 10) continue;

            WorkExperience.WorkExperienceBuilder wb = WorkExperience.builder();
            String[] lines = entry.trim().split("\\n");

            // First line: company name (or combined "Company — Title")
            String firstLine = lines.length > 0 ? lines[0].trim() : "";
            if (firstLine.contains(" — ") || firstLine.contains(" – ") || firstLine.contains(" - ")) {
                String[] parts = firstLine.split("\\s*[—–\\-]\\s*", 2);
                wb.company(parts[0].trim());
                if (parts.length > 1) wb.jobTitle(parts[1].trim());
            } else {
                wb.company(firstLine);
            }

            // Second line might be job title if not found above
            if (wb.build().getJobTitle() == null && lines.length > 1) {
                String secondLine = lines[1].trim();
                if (!DATE_RANGE.matcher(secondLine).find() && !secondLine.startsWith("•")
                        && !secondLine.startsWith("-") && secondLine.length() < 100) {
                    wb.jobTitle(secondLine);
                }
            }

            // Date range
            Matcher dm = DATE_RANGE.matcher(entry);
            if (dm.find()) {
                wb.startDate(normalizeDate(dm.group(1)));
                String end = dm.group(2);
                if (end.equalsIgnoreCase("Present") || end.equalsIgnoreCase("Current")) {
                    wb.endDate(null);
                    wb.isCurrent(true);
                } else {
                    wb.endDate(normalizeDate(end));
                    wb.isCurrent(false);
                }
            }

            // Location: look for "City, ST" pattern
            for (String line : lines) {
                Matcher locM = Pattern.compile("([A-Z][a-z]+(?:\\s[A-Z][a-z]+)*,\\s*[A-Z]{2})").matcher(line);
                if (locM.find()) {
                    wb.location(locM.group(1));
                    break;
                }
                if (line.trim().equalsIgnoreCase("Remote")) {
                    wb.location("Remote");
                    break;
                }
            }

            // Highlights: bullet points
            List<String> highlights = new ArrayList<>();
            StringBuilder description = new StringBuilder();
            for (String line : lines) {
                String l = line.trim();
                if (l.startsWith("•") || l.startsWith("-") || l.startsWith("●") || l.startsWith("▪")) {
                    highlights.add(l.replaceFirst("^[•\\-●▪]\\s*", ""));
                }
            }
            wb.highlights(highlights);

            // Description: combine non-bullet, non-header lines
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;
                if (l.equals(firstLine)) continue;
                if (l.startsWith("•") || l.startsWith("-") || l.startsWith("●") || l.startsWith("▪")) continue;
                if (DATE_RANGE.matcher(l).find()) continue;
                if (l.equals(wb.build().getJobTitle())) continue;
                if (l.equals(wb.build().getLocation())) continue;
                description.append(l).append(" ");
            }
            if (!description.isEmpty()) {
                wb.description(description.toString().trim());
            }

            // Technologies: look for "Technologies:" or "Tech Stack:" line
            for (String line : lines) {
                String l = line.trim();
                if (l.toLowerCase().startsWith("technologies:") || l.toLowerCase().startsWith("tech stack:")
                        || l.toLowerCase().startsWith("tools:") || l.toLowerCase().startsWith("stack:")) {
                    String techStr = l.substring(l.indexOf(":") + 1).trim();
                    wb.technologies(parseCommaSeparated(techStr));
                    break;
                }
            }
            // If no explicit tech line found, try to extract from highlights
            if (wb.build().getTechnologies() == null || wb.build().getTechnologies().isEmpty()) {
                wb.technologies(Collections.emptyList());
            }

            wb.sortOrder(order++);
            results.add(wb.build());
        }

        return results;
    }

    // ── Projects extraction ─────────────────────────────────────────────────

    private List<Project> extractProjects(Map<String, String> sections) {
        String text = sections.getOrDefault("PROJECTS", "");
        if (text.isEmpty()) return Collections.emptyList();

        List<Project> results = new ArrayList<>();
        List<String> entries = splitEntries(text);

        int order = 1;
        for (String entry : entries) {
            if (entry.trim().length() < 10) continue;

            Project.ProjectBuilder pb = Project.builder();
            String[] lines = entry.trim().split("\\n");

            // First line: project name
            String firstLine = lines.length > 0 ? lines[0].trim() : "Project";
            // Remove any link from the name line
            firstLine = firstLine.replaceAll("\\s*https?://\\S+", "").trim();
            pb.name(firstLine);

            // Description: non-bullet lines after the name
            StringBuilder desc = new StringBuilder();
            List<String> techStack = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String l = lines[i].trim();
                if (l.isEmpty()) continue;

                // Tech stack line
                if (l.toLowerCase().startsWith("tech") || l.toLowerCase().startsWith("stack")
                        || l.toLowerCase().startsWith("built with") || l.toLowerCase().startsWith("tools:")) {
                    String techStr = l.contains(":") ? l.substring(l.indexOf(":") + 1).trim() : l;
                    techStack.addAll(parseCommaSeparated(techStr));
                    continue;
                }

                // URLs
                Matcher gm = GITHUB_PATTERN.matcher(l);
                if (gm.find()) {
                    String url = gm.group();
                    if (!url.startsWith("http")) url = "https://" + url;
                    pb.githubUrl(url);
                }
                Matcher um = URL_PATTERN.matcher(l);
                while (um.find()) {
                    String url = um.group();
                    if (!url.contains("github.com") && !url.contains("linkedin.com")) {
                        pb.liveUrl(url);
                    }
                }

                // Bullet points go into description
                if (l.startsWith("•") || l.startsWith("-") || l.startsWith("●")) {
                    desc.append(l.replaceFirst("^[•\\-●]\\s*", "")).append(". ");
                } else if (!URL_PATTERN.matcher(l).find()) {
                    desc.append(l).append(" ");
                }
            }

            pb.description(desc.toString().trim());
            pb.techStack(techStack.isEmpty() ? Collections.emptyList() : techStack);
            pb.featured(order <= 3); // First 3 projects are featured
            pb.category(inferCategory(desc.toString(), techStack));
            pb.sortOrder(order++);
            results.add(pb.build());
        }

        return results;
    }

    // ── Utility methods ─────────────────────────────────────────────────────

    /**
     * Splits a section into individual entries.
     * Entries are separated by blank lines or by lines that contain a date range.
     */
    private List<String> splitEntries(String sectionText) {
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : sectionText.split("\\n")) {
            // A blank line or a line starting a new entry (date range on a line with a new heading)
            if (line.trim().isEmpty() && !current.isEmpty() && current.toString().trim().length() > 20) {
                // Check if the current block looks like a complete entry
                if (DATE_RANGE.matcher(current).find() || current.toString().contains("•")) {
                    entries.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty() && current.toString().trim().length() > 5) {
            entries.add(current.toString());
        }

        // If we only got one big block, try splitting on date ranges
        if (entries.size() <= 1 && !sectionText.isEmpty()) {
            entries.clear();
            // Split before lines that look like they start a new entry
            // (line followed by a date range within 3 lines)
            String[] allLines = sectionText.split("\\n");
            current = new StringBuilder();
            for (int i = 0; i < allLines.length; i++) {
                String line = allLines[i];
                // If this line has a date range and current already has content,
                // it might be a new entry
                if (i > 0 && DATE_RANGE.matcher(line).find() && !current.isEmpty()
                        && current.toString().trim().length() > 15) {
                    // Check if previous lines already had a date range
                    if (DATE_RANGE.matcher(current).find()) {
                        entries.add(current.toString());
                        current = new StringBuilder();
                    }
                }
                current.append(line).append("\n");
            }
            if (!current.isEmpty()) {
                entries.add(current.toString());
            }
        }

        return entries;
    }

    private String findFirst(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group() : null;
    }

    private List<String> parseList(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        // Split on commas, pipes, bullets, or newlines
        String[] items = text.split("[,|•●▪\\n]");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String cleaned = item.replaceAll("^[\\-\\s]+", "").trim();
            if (!cleaned.isEmpty() && cleaned.length() < 60) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private List<String> parseCommaSeparated(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        String[] items = text.split("[,|]");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String cleaned = item.trim();
            if (!cleaned.isEmpty()) result.add(cleaned);
        }
        return result;
    }

    /**
     * Splits a project description into project categories.
     */
    private String inferCategory(String description, List<String> techStack) {
        String combined = (description + " " + String.join(" ", techStack)).toLowerCase();

        if (combined.contains("machine learning") || combined.contains("ml") ||
                combined.contains("tensorflow") || combined.contains("pytorch") ||
                combined.contains("deep learning") || combined.contains("neural") ||
                combined.contains("nlp") || combined.contains("computer vision") ||
                combined.contains("ai") || combined.contains("artificial intelligence") ||
                combined.contains("logistic regression")) {
            return "ML/AI";
        }
        if (combined.contains("android") || combined.contains("ios") ||
                combined.contains("flutter") || combined.contains("react native") ||
                combined.contains("swift") || combined.contains("kotlin") ||
                combined.contains("mobile")) {
            return "Mobile";
        }
        if (combined.contains("azure") || combined.contains("aws") ||
                combined.contains("gcp") || combined.contains("docker") ||
                combined.contains("kubernetes") || combined.contains("terraform") ||
                combined.contains("devops") || combined.contains("ci/cd") ||
                combined.contains("cloud")) {
            return "Cloud/DevOps";
        }
        if (combined.contains("python") || combined.contains("data") ||
                combined.contains("pandas") || combined.contains("analytics") ||
                combined.contains("visualization") || combined.contains("etl")) {
            return "Data";
        }
        if ((combined.contains("react") || combined.contains("angular") ||
                combined.contains("vue") || combined.contains("frontend") ||
                combined.contains("html") || combined.contains("css")) &&
                !(combined.contains("spring") || combined.contains("node") ||
                        combined.contains("express") || combined.contains("django"))) {
            return "Frontend";
        }
        if ((combined.contains("spring") || combined.contains("node") ||
                combined.contains("express") || combined.contains("django") ||
                combined.contains(".net") || combined.contains("api")) &&
                !(combined.contains("react") || combined.contains("angular") ||
                        combined.contains("vue"))) {
            return "Backend";
        }
        return "Full Stack";
    }



    /**
     * Normalise dates like "Aug 2018", "August 2018", "2018-08" → "2018-08"
     */
    private String normalizeDate(String raw) {
        if (raw == null) return null;
        raw = raw.trim();

        // Already in YYYY-MM format
        if (raw.matches("\\d{4}[\\-/]\\d{2}")) {
            return raw.replace("/", "-");
        }
        // Just a year
        if (raw.matches("\\d{4}")) {
            return raw;
        }

        // "Aug 2018" or "August 2018"
        Map<String, String> months = Map.ofEntries(
                Map.entry("jan", "01"), Map.entry("feb", "02"), Map.entry("mar", "03"),
                Map.entry("apr", "04"), Map.entry("may", "05"), Map.entry("jun", "06"),
                Map.entry("jul", "07"), Map.entry("aug", "08"), Map.entry("sep", "09"),
                Map.entry("oct", "10"), Map.entry("nov", "11"), Map.entry("dec", "12")
        );
        Matcher m = Pattern.compile("([A-Za-z]+)\\.?\\s+(\\d{4})").matcher(raw);
        if (m.find()) {
            String monthStr = m.group(1).toLowerCase().substring(0, 3);
            String month = months.getOrDefault(monthStr, "01");
            return m.group(2) + "-" + month;
        }

        return raw;
    }

    // ── Result container ────────────────────────────────────────────────────

    @lombok.Data
    public static class ParsedResume {
        private Profile profile;
        private List<Education> education = new ArrayList<>();
        private List<WorkExperience> workExperience = new ArrayList<>();
        private List<Project> projects = new ArrayList<>();
    }
}

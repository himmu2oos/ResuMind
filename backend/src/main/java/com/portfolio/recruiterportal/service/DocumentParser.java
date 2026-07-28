package com.portfolio.recruiterportal.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentParser {

    public String parseFile(File file) throws IOException {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            return parsePdf(file);
        } else if (name.endsWith(".docx")) {
            return parseDocx(file);
        } else if (name.endsWith(".txt") || name.endsWith(".md")) {
            return Files.readString(file.toPath());
        }

        throw new IllegalArgumentException("Unsupported file type: " + name);
    }

    private String parsePdf(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // Some resumes show links as an icon or short label (e.g. "LinkedIn")
            // rather than the literal URL text, so the visible-text stripper alone
            // misses them. Append the actual hyperlink targets so downstream regex
            // matching in ResumeParserService can still find them.
            List<String> linkUrls = extractHyperlinks(doc);
            if (!linkUrls.isEmpty()) {
                text += "\n" + String.join("\n", linkUrls);
            }

            return text;
        }
    }

    private List<String> extractHyperlinks(PDDocument doc) throws IOException {
        List<String> urls = new ArrayList<>();
        for (PDPage page : doc.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link) {
                    PDAction action = link.getAction();
                    if (action instanceof PDActionURI uriAction) {
                        urls.add(uriAction.getURI());
                    }
                }
            }
        }
        return urls;
    }

    private String parseDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }
}

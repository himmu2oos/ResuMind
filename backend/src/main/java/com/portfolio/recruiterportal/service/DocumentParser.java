package com.portfolio.recruiterportal.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
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
            return stripper.getText(doc);
        }
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

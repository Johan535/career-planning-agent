package com.johan.careerplanningagent.tool;

import com.johan.careerplanningagent.service.GeneratedFileStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        GeneratedFileStore store = new GeneratedFileStore();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(store);
        String filename = "test.pdf";
        String content = "This is a test PDF.";
        String result = pdfGenerationTool.generatePDF(filename, content);
        assertNotNull(result);
        assertTrue(result.contains("fileId=") || result.contains("PDF"));
    }
}
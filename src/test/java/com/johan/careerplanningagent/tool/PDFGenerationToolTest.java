package com.johan.careerplanningagent.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        String filename = "test.pdf";
        String content = "This is a test PDF.";
        String result = pdfGenerationTool.generatePDF(filename, content);
        assertNotNull(result);
    }
}
package com.travel.ai.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "示例项目.pdf";
        String content = "示例项目 https://example.com";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}
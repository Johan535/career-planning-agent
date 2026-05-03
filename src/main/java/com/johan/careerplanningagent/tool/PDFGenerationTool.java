package com.johan.careerplanningagent.tool;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.johan.careerplanningagent.constant.FileConstant;
import com.johan.careerplanningagent.manus.ManusSseContext;
import com.johan.careerplanningagent.service.GeneratedFileStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * PDF 生成：写入磁盘并注册 fileId，同时通过 Manus SSE 推送 artifact 供前端下载。
 */
@Component
public class PDFGenerationTool {

    private final GeneratedFileStore generatedFileStore;

    public PDFGenerationTool(GeneratedFileStore generatedFileStore) {
        this.generatedFileStore = generatedFileStore;
    }

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String safeBase = fileName == null || fileName.isBlank() ? "report.pdf" : fileName.trim();
        if (!safeBase.toLowerCase().endsWith(".pdf")) {
            safeBase = safeBase + ".pdf";
        }
        String unique = UUID.randomUUID().toString().replace("-", "") + "_" + safeBase;
        String fileDir = FileConstant.FILE_SAVE_PATH + "/pdf";
        String filePath = fileDir + "/" + unique;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                PdfFont font;
                try {
                    font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                } catch (Exception e) {
                    font = PdfFontFactory.createFont("Helvetica");
                }
                document.setFont(font);
                
                String[] lines = (content == null ? "" : content).split("\n");
                for (String line : lines) {
                    Paragraph paragraph = new Paragraph(line.trim());
                    paragraph.setTextAlignment(TextAlignment.LEFT);
                    paragraph.setMultipliedLeading(1.5f);
                    document.add(paragraph);
                }
            }
            Path absolute = Path.of(filePath).toAbsolutePath().normalize();
            String owner = ManusSseContext.currentSessionKey();
            String fileId = generatedFileStore.register(absolute, owner, safeBase);
            ManusSseContext.sendArtifact(fileId, safeBase);
            return "PDF 已生成，fileId=" + fileId + "，展示文件名=" + safeBase + "。请提示用户可在页面「生成文件」区域点击「下载到本地」保存。";
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}

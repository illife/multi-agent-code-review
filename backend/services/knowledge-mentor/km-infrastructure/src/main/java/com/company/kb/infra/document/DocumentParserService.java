package com.company.kb.infra.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务
 *
 * <p>Supports PDF, Word, and other document formats parsing.
 * Provides both simple parsing (file path based) and streaming parsing (input stream based).</p>
 *
 * <p>For large files (>50MB), use streaming parsing with Apache Tika to avoid OOM.</p>
 *
 * @see StreamingDocumentParser
 */
@Slf4j
@Service
public class DocumentParserService {

    @Autowired(required = false)
    private StreamingDocumentParser streamingDocumentParser;

    /**
     * 解析文档内容 (Simple parsing - file path based)
     *
     * <p>This method uses traditional parsers (PDFBox, POI) and loads the entire
     * file into memory. Suitable for small files (<50MB).</p>
     *
     * @param filePath 文件路径
     * @return 文档文本内容
     */
    public String parse(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        String fileName = file.getName().toLowerCase();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        switch (extension) {
            case "pdf":
                return parsePDF(file);
            case "doc":
                return parseDoc(file);
            case "docx":
                return parseDocx(file);
            case "txt":
                return parseTxt(file);
            case "md":
                return parseMarkdown(file);
            default:
                throw new UnsupportedOperationException("不支持的文件格式: " + extension);
        }
    }

    /**
     * 解析PDF文档
     */
    private String parsePDF(File file) throws Exception {
        log.info("开始解析PDF: {}", file.getName());

        try (PDDocument document = PDDocument.load(file)) {
            log.info("PDF加载成功: pageCount={}", document.getNumberOfPages());

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);

            String content = textStripper.getText(document);
            log.info("PDF原始内容: contentLength={}, hasContent={}",
                content.length(), !content.isEmpty());

            String trimmed = content.trim();
            log.info("PDF trim后内容: contentLength={}, hasContent={}",
                trimmed.length(), !trimmed.isEmpty());

            if (trimmed.length() == 0 && content.length() > 0) {
                log.warn("PDF内容全是空白字符: originalLength={}", content.length());
            }

            return trimmed;

        } catch (IOException e) {
            log.error("PDF解析失败: {}", file.getName(), e);
            throw new Exception("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Word文档（.doc格式）
     */
    private String parseDoc(File file) throws Exception {
        log.debug("开始解析DOC: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {

            String content = extractor.getText();
            log.debug("DOC解析完成: contentLength={}", content.length());

            return content.trim();

        } catch (IOException e) {
            log.error("DOC解析失败: {}", file.getName(), e);
            throw new Exception("DOC解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Word文档（.docx格式）
     */
    private String parseDocx(File file) throws Exception {
        log.debug("开始解析DOCX: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder content = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            for (XWPFParagraph paragraph : paragraphs) {
                content.append(paragraph.getText()).append("\n");
            }

            log.debug("DOCX解析完成: contentLength={}", content.length());

            return content.toString().trim();

        } catch (IOException e) {
            log.error("DOCX解析失败: {}", file.getName(), e);
            throw new Exception("DOCX解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析文本文档
     */
    private String parseTxt(File file) throws Exception {
        log.debug("开始解析TXT: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);

            // 使用编码检测，支持Windows中文GBK编码
            String content = tryDecodeWithEncoding(data, new String[]{"UTF-8", "GBK", "GB2312"});

            log.debug("TXT解析完成: contentLength={}", content.length());
            return content.trim();

        } catch (IOException e) {
            log.error("TXT解析失败: {}", file.getName(), e);
            throw new Exception("TXT解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Markdown文档
     */
    private String parseMarkdown(File file) throws Exception {
        log.debug("开始解析Markdown: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);

            // 使用编码检测，支持Windows中文GBK编码
            String content = tryDecodeWithEncoding(data, new String[]{"UTF-8", "GBK", "GB2312"});

            log.debug("Markdown解析完成: contentLength={}", content.length());
            return content.trim();

        } catch (IOException e) {
            log.error("Markdown解析失败: {}", file.getName(), e);
            throw new Exception("Markdown解析失败: " + e.getMessage(), e);
        }
    }

    // ========== Streaming parsing methods (Apache Tika) ==========

    /**
     * 解析文档内容 (Streaming parsing - input stream based)
     *
     * <p>This method uses Apache Tika for streaming document parsing.
     * Advantages:</p>
     * <ul>
     *   <li>Memory efficient - no OOM for large files</li>
     *   <li>Supports 1000+ file formats</li>
     *   <li>Automatic character encoding detection</li>
     *   <li>Metadata extraction</li>
     * </ul>
     *
     * <p>Recommended for files >50MB.</p>
     *
     * <p>⚠️ CURRENTLY DISABLED: Will fallback to simple parsing with warning.</p>
     *
     * @param inputStream Input stream of the document file
     * @param fileName    File name (used for MIME type detection)
     * @return Document text content
     * @throws Exception If parsing fails
     */
    public String parseStreaming(InputStream inputStream, String fileName) throws Exception {
        log.debug("开始流式解析文档: fileName={}", fileName);

        if (streamingDocumentParser == null) {
            log.warn("StreamingDocumentParser未启用，使用传统解析方式: fileName={}", fileName);
            // Fallback: read as text file
            return parseTextFromStream(inputStream, fileName);
        }

        return streamingDocumentParser.parseStreaming(inputStream, fileName);
    }

    /**
     * 简单解析 (使用 Tika facade)
     *
     * <p>Convenience method that uses Tika's facade for automatic parsing.
     * Simpler than parseStreaming but loads entire content into memory.</p>
     *
     * <p>⚠️ CURRENTLY DISABLED: Will fallback to simple parsing.</p>
     *
     * @param inputStream Input stream of the document file
     * @param fileName    File name
     * @return Document text content
     * @throws Exception If parsing fails
     */
    public String parseSimple(InputStream inputStream, String fileName) throws Exception {
        log.debug("开始简单解析文档: fileName={}", fileName);

        if (streamingDocumentParser == null) {
            log.warn("StreamingDocumentParser未启用，使用传统解析方式: fileName={}", fileName);
            // Fallback: read as text file
            return parseTextFromStream(inputStream, fileName);
        }

        return streamingDocumentParser.parseSimple(inputStream, fileName);
    }

    /**
     * 检测文档 MIME 类型
     *
     * <p>⚠️ CURRENTLY DISABLED: Will return null.</p>
     *
     * @param inputStream Input stream of the document file
     * @param fileName    File name
     * @return MIME type (e.g., "application/pdf") or null if disabled
     * @throws Exception If detection fails
     */
    public String detectMimeType(InputStream inputStream, String fileName) throws Exception {
        if (streamingDocumentParser == null) {
            log.debug("StreamingDocumentParser未启用，跳过MIME检测: fileName={}", fileName);
            return null;
        }
        return streamingDocumentParser.detectMimeType(inputStream, fileName);
    }

    /**
     * Helper method: Read text from input stream (fallback)
     */
    private String parseTextFromStream(InputStream inputStream, String fileName) throws Exception {
        try {
            byte[] data = inputStream.readAllBytes();

            // 尝试多种编码，优先UTF-8，然后尝试GBK（Windows中文默认编码）
            String content = tryDecodeWithEncoding(data, new String[]{"UTF-8", "GBK", "GB2312", "ISO-8859-1"});

            log.debug("传统解析完成: fileName={}, contentLength={}", fileName, content.length());
            return content.trim();
        } catch (Exception e) {
            log.error("传统解析失败: fileName={}", fileName, e);
            throw new Exception("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试用多种编码解码字节数组
     */
    private String tryDecodeWithEncoding(byte[] data, String[] encodings) {
        for (String encoding : encodings) {
            try {
                String content = new String(data, encoding);
                // 简单验证：检查是否包含过多替换字符（表示编码错误）
                if (!content.contains("") && !content.contains("")) {
                    log.debug("使用编码 {} 成功解码", encoding);
                    return content;
                }
            } catch (Exception e) {
                // 继续尝试下一个编码
            }
        }
        // 如果所有编码都失败，使用UTF-8作为默认
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }
}

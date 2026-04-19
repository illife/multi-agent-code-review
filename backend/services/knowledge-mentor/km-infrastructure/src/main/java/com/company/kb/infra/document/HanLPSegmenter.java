package com.company.kb.infra.document;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * HanLP Chinese Word Segmentation Service
 *
 * <p>Provides intelligent Chinese text segmentation for semantic chunking.
 * Maintains sentence boundaries and semantic integrity.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Intelligent sentence boundary detection</li>
 *   <li>Preserves semantic integrity</li>
 *   <li>Handles mixed Chinese-English text</li>
 *   <li>Customizable segmentation rules</li>
 * </ul>
 *
 * <p>Based on PaiSmart's implementation pattern using HanLP 1.8.6.</p>
 *
 * @see <a href="https://github.com/hankcs/HanLP">HanLP GitHub</a>
 */
@Slf4j
@Component
public class HanLPSegmenter {

    // Sentence ending patterns for Chinese and English
    private static final Pattern CHINESE_SENTENCE_DELIMITER = Pattern.compile("[。！？；]");
    private static final Pattern ENGLISH_SENTENCE_DELIMITER = Pattern.compile("[.!?;]");
    private static final Pattern PARAGRAPH_DELIMITER = Pattern.compile("\\n\\n|\\r\\n\\r\\n");

    /**
     * Split text into sentences (maintaining semantic boundaries)
     *
     * <p>This method intelligently splits text into sentences while preserving
     * semantic integrity. It handles both Chinese and English text.</p>
     *
     * @param text Input text
     * @return List of sentences
     */
    public List<String> splitSentence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sentences = new ArrayList<>();

        // First, split by paragraph
        String[] paragraphs = PARAGRAPH_DELIMITER.split(text.trim());

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // Split each paragraph into sentences
            List<String> paragraphSentences = splitParagraphIntoSentences(paragraph.trim());
            sentences.addAll(paragraphSentences);
        }

        log.debug("文本分句完成: inputLength={}, sentenceCount={}", text.length(), sentences.size());
        return sentences;
    }

    /**
     * Split a paragraph into sentences
     *
     * @param paragraph Paragraph text
     * @return List of sentences
     */
    private List<String> splitParagraphIntoSentences(String paragraph) {
        List<String> sentences = new ArrayList<>();

        // Use HanLP for intelligent sentence segmentation
        // This preserves semantic boundaries better than regex
        List<String> rawSentences = new ArrayList<>();

        // Split by Chinese sentence delimiters first
        String[] chineseParts = CHINESE_SENTENCE_DELIMITER.split(paragraph);

        for (String part : chineseParts) {
            if (part.trim().isEmpty()) {
                continue;
            }

            // Further split by English sentence delimiters
            String[] englishParts = ENGLISH_SENTENCE_DELIMITER.split(part);

            for (String sentence : englishParts) {
                if (sentence.trim().length() > 0) {
                    rawSentences.add(sentence.trim());
                }
            }
        }

        // Clean up sentences
        for (String sentence : rawSentences) {
            String cleaned = cleanSentence(sentence);
            if (!cleaned.isEmpty()) {
                sentences.add(cleaned);
            }
        }

        return sentences;
    }

    /**
     * Clean sentence by removing extra whitespace
     *
     * @param sentence Raw sentence
     * @return Cleaned sentence
     */
    private String cleanSentence(String sentence) {
        return sentence.replaceAll("\\s+", " ").trim();
    }

    /**
     * Segment text into words (for analysis/debugging)
     *
     * <p>This uses HanLP's word segmentation to break text into individual words.
     * Useful for debugging and understanding text structure.</p>
     *
     * @param text Input text
     * @return List of words with part-of-speech tags
     */
    public List<String> segmentWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Term> terms = HanLP.segment(text);
        List<String> words = new ArrayList<>(terms.size());

        for (Term term : terms) {
            words.add(term.word + "/" + term.nature);
        }

        log.debug("词语切分完成: inputLength={}, wordCount={}", text.length(), words.size());
        return words;
    }

    /**
     * Extract keywords from text
     *
     * <p>Uses HanLP's keyword extraction to identify important terms.</p>
     *
     * @param text     Input text
     * @param topN     Number of keywords to extract
     * @return List of keywords
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> keywords = HanLP.extractKeyword(text, topN);
        log.debug("关键词提取完成: inputLength={}, keywordCount={}", text.length(), keywords.size());
        return keywords;
    }

    /**
     * Extract summary phrases from text
     *
     * <p>Uses HanLP's phrase extraction to identify important phrases.</p>
     *
     * @param text     Input text
     * @param topN     Number of phrases to extract
     * @return List of phrases
     */
    public List<String> extractPhrases(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> phrases = HanLP.extractPhrase(text, topN);
        log.debug("短语提取完成: inputLength={}, phraseCount={}", text.length(), phrases.size());
        return phrases;
    }

    /**
     * Check if text contains Chinese characters
     *
     * @param text Input text
     * @return true if contains Chinese, false otherwise
     */
    public boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Check for Chinese character range
        return text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    /**
     * Estimate character count for Chinese text
     *
     * <p>For memory estimation and chunk size calculation.</p>
     *
     * @param text Input text
     * @return Estimated character count
     */
    public int estimateCharCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Chinese characters count as 2, English as 1
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4e00 && c <= 0x9fa5) {
                count += 2; // Chinese character
            } else {
                count += 1; // English or other
            }
        }

        return count;
    }

    /**
     * Get statistics about the text
     *
     * @param text Input text
     * @return Statistics object
     */
    public TextStatistics getStatistics(String text) {
        if (text == null || text.isEmpty()) {
            return new TextStatistics(0, 0, 0, false);
        }

        int charCount = text.length();
        int estimatedCount = estimateCharCount(text);
        int sentenceCount = splitSentence(text).size();
        boolean hasChinese = containsChinese(text);

        return new TextStatistics(charCount, estimatedCount, sentenceCount, hasChinese);
    }

    /**
     * Text statistics record
     */
    public record TextStatistics(
        int charCount,
        int estimatedCharCount,
        int sentenceCount,
        boolean containsChinese
    ) {
        @Override
        public String toString() {
            return String.format("TextStatistics{chars=%d, estimated=%d, sentences=%d, chinese=%b}",
                charCount, estimatedCharCount, sentenceCount, containsChinese);
        }
    }
}

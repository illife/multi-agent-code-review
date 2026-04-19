package com.company.kb.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询扩展服务
 * 通过同义词、相关词、查询重写等方式提高搜索召回率和准确率
 */
@Slf4j
@Service
public class QueryExpansionService {

    /**
     * 内置同义词词典（技术领域相关）
     */
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();

    /**
     * 相关词词典（基于共现和语义相关）
     */
    private static final Map<String, List<String>> RELATED_WORDS = new HashMap<>();

    static {
        // 技术术语同义词
        addSynonym("区块链", "blockchain", "分布式账本", "链上");
        addSynonym("智能合约", "smart contract", "合约", "链上合约");
        addSynonym("版权", "copyright", "著作权", "知识产权");
        addSynonym("数字资产", "数字内容", "虚拟资产");
        addSynonym("去中心化", "分布式", "decentralized");
        addSynonym("共识机制", "consensus", "一致性算法");
        addSynonym("加密", "密码学", "encryption");
        addSynonym("交易", "transaction", "tx", "转账");
        addSynonym("钱包", "wallet", "账户", "地址");
        addSynonym("节点", "node", "服务器", "验证节点");

        // 中文同义词
        addSynonym("搜索", "检索", "查询", "查找");
        addSynonym("文档", "文件", "资料", "论文");
        addSynonym("系统", "平台", "应用", "软件");
        addSynonym("用户", "使用者", "客户");
        addSynonym("开发", "研发", "编程", "实现");
        addSynonym("测试", "验证", "检验");
        addSynonym("部署", "发布", "上线");

        // 相关词
        addRelated("区块链", List.of("比特币", "以太坊", "FISCO", "BCOS", "联盟链", "公链"));
        addRelated("智能合约", List.of("Solidity", "合约部署", "合约调用", "ABI"));
        addRelated("版权", List.of("原创", "盗版", "授权", "许可", "保护"));
        addRelated("文档", List.of("上传", "下载", "预览", "编辑"));
        addRelated("搜索", List.of("匹配", "排序", "相关性", "召回"));
    }

    private static void addSynonym(String word, String... synonyms) {
        SYNONYMS.put(word, Arrays.asList(synonyms));
        for (String synonym : synonyms) {
            SYNONYMS.putIfAbsent(synonym, new ArrayList<>());
            SYNONYMS.get(synonym).add(word);
        }
    }

    private static void addRelated(String word, List<String> related) {
        RELATED_WORDS.put(word, related);
    }

    /**
     * 扩展查询（主要方法）
     * @param originalQuery 原始查询
     * @return 扩展后的查询列表（包含原始查询）
     */
    public List<String> expandQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return Collections.singletonList(originalQuery);
        }

        log.debug("开始扩展查询: {}", originalQuery);

        Set<String> expandedQueries = new LinkedHashSet<>();
        expandedQueries.add(originalQuery);  // 始终包含原始查询

        // 1. 同义词扩展
        List<String> synonymQueries = expandWithSynonyms(originalQuery);
        expandedQueries.addAll(synonymQueries);

        // 2. 相关词扩展
        List<String> relatedQueries = expandWithRelatedWords(originalQuery);
        expandedQueries.addAll(relatedQueries);

        // 3. 查询重写（生成变体）
        List<String> rewrittenQueries = rewriteQuery(originalQuery);
        expandedQueries.addAll(rewrittenQueries);

        List<String> result = new ArrayList<>(expandedQueries);
        log.debug("查询扩展完成: 原始='{}', 扩展后数量={}", originalQuery, result.size());

        return result;
    }

    /**
     * 同义词扩展
     * 将查询中的词替换为其同义词
     */
    private List<String> expandWithSynonyms(String query) {
        List<String> expanded = new ArrayList<>();

        for (String word : extractWords(query)) {
            List<String> synonyms = SYNONYMS.get(word);
            if (synonyms != null && !synonyms.isEmpty()) {
                for (String synonym : synonyms) {
                    String expandedQuery = query.replace(word, synonym);
                    if (!expandedQuery.equals(query)) {
                        expanded.add(expandedQuery);
                    }
                }
            }
        }

        return expanded;
    }

    /**
     * 相关词扩展
     * 为查询添加相关词
     */
    private List<String> expandWithRelatedWords(String query) {
        List<String> expanded = new ArrayList<>();

        for (String word : extractWords(query)) {
            List<String> related = RELATED_WORDS.get(word);
            if (related != null && !related.isEmpty()) {
                // 添加相关词到查询中
                String withRelated = query + " " + String.join(" ", related.subList(0, Math.min(3, related.size())));
                expanded.add(withRelated);
            }
        }

        return expanded;
    }

    /**
     * 查询重写
     * 生成查询的多种变体
     */
    private List<String> rewriteQuery(String query) {
        List<String> rewritten = new ArrayList<>();

        // 1. 去除停用词
        String withoutStopWords = removeStopWords(query);
        if (!withoutStopWords.equals(query) && !withoutStopWords.isEmpty()) {
            rewritten.add(withoutStopWords);
        }

        // 2. 提取关键词（核心词）
        List<String> keywords = extractKeywords(query);
        if (keywords.size() > 1 && keywords.size() < query.length() / 2) {
            String keywordQuery = String.join(" ", keywords);
            rewritten.add(keywordQuery);
        }

        // 3. 生成布尔查询变体（OR连接）
        if (keywords.size() > 1) {
            String orQuery = String.join(" OR ", keywords);
            rewritten.add(orQuery);
        }

        return rewritten;
    }

    /**
     * 提取查询中的词语
     */
    private List<String> extractWords(String query) {
        // 简单的分词（按空格和标点）
        return Arrays.stream(query.split("[\\s\\p{Punct}]+"))
            .filter(w -> w.length() > 1)  // 过滤单字符
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 提取关键词（去除常见停用词）
     */
    private List<String> extractKeywords(String query) {
        Set<String> stopWords = Set.of(
            "的", "了", "是", "在", "有", "和", "与", "或", "但是", "如果",
            "the", "a", "an", "is", "are", "and", "or", "but", "if"
        );

        return extractWords(query).stream()
            .filter(w -> !stopWords.contains(w.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * 去除停用词
     */
    private String removeStopWords(String query) {
        List<String> keywords = extractKeywords(query);
        return String.join(" ", keywords);
    }

    /**
     * 生成增强的BM25查询
     * 使用boost提升重要词的权重
     */
    public String generateBoostedQuery(String originalQuery) {
        List<String> keywords = extractKeywords(originalQuery);
        if (keywords.isEmpty()) {
            return originalQuery;
        }

        // 第一个词boost最高
        StringBuilder boosted = new StringBuilder();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) boosted.append(" ");
            int boost = Math.max(1, 3 - i);  // 3, 2, 1
            boosted.append(keywords.get(i)).append("^").append(boost);
        }

        return boosted.toString();
    }

    /**
     * 获取查询建议（拼写纠错）
     * 简单实现：基于编辑距离
     */
    public List<String> getSuggestions(String query, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();

        // 如果查询很短，不做建议
        if (query.length() < 2) {
            return suggestions;
        }

        // 从同义词词典中查找相似词
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String word = entry.getKey();
            if (editDistance(query, word) <= 2 && !query.equals(word)) {
                suggestions.add(word);
                if (suggestions.size() >= maxSuggestions) {
                    break;
                }
            }
        }

        return suggestions;
    }

    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int editDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j],
                        Math.min(dp[i][j - 1], dp[i - 1][j - 1])
                    );
                }
            }
        }

        return dp[len1][len2];
    }
}

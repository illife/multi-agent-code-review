package com.think.platform.shared.ai.llm;

/**
 * LLM 提供者类型
 *
 * @author AI Code Mentor Team
 */
public enum LlmProviderType {

    /**
     * 通义千问 (阿里云)
     */
    QWEN("qwen", "通义千问", "https://dashscope.aliyuncs.com"),

    /**
     * 智谱 GLM
     */
    GLM("glm", "智谱GLM", "https://open.bigmodel.cn"),

    /**
     * OpenAI
     */
    OPENAI("openai", "OpenAI", "https://api.openai.com"),

    /**
     * Anthropic Claude
     */
    CLAUDE("claude", "Claude", "https://api.anthropic.com"),

    /**
     * 百度文心
     */
    ERNIE("ernie", "百度文心", "https://aip.baidubce.com"),

    /**
     * 腾讯混元
     */
    HUNYUAN("hunyuan", "腾讯混元", "https://hunyuan.tencentcloudapi.com"),

    /**
     * 本地模型
     */
    LOCAL("local", "本地模型", "http://localhost:11434");

    private final String code;
    private final String name;
    private final String baseUrl;

    LlmProviderType(String code, String name, String baseUrl) {
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 根据 code 获取类型
     */
    public static LlmProviderType fromCode(String code) {
        for (LlmProviderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown LLM provider type: " + code);
    }
}

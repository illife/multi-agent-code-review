package com.think.platform.shared.ai.config;

import com.think.platform.shared.ai.llm.LlmProvider;
import com.think.platform.shared.ai.llm.QwenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service Auto-Configuration
 * Automatically configures LLM providers based on application properties
 *
 * @author AI Code Mentor Team
 */
@Configuration
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiAutoConfiguration {

    /**
     * Configure Qwen provider as default LLM provider
     */
    @Bean
    @ConditionalOnMissingBean(LlmProvider.class)
    public LlmProvider qwenProvider() {
        return new QwenProvider();
    }
}

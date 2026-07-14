package com.darkps.agentterminal.model

data class AiModel(
    val id: String,
    val name: String,
    val provider: String
) {
    companion object {
        val AVAILABLE_MODELS = listOf(
            AiModel("deepseek-v3", "DeepSeek V3", "DeepSeek"),
            AiModel("deepseek-v3.2", "DeepSeek V3.2", "DeepSeek"),
            AiModel("deepseek-r1", "DeepSeek R1", "DeepSeek"),
            AiModel("deepseek-v4-flash", "DeepSeek V4 Flash", "DeepSeek"),
            AiModel("deepseek-v4-pro", "DeepSeek V4 Pro", "DeepSeek"),
            AiModel("glm-4.5", "GLM 4.5", "Z-AI"),
            AiModel("glm-4.6", "GLM 4.6", "Z-AI"),
            AiModel("glm-4.7", "GLM 4.7", "Z-AI"),
            AiModel("glm-5", "GLM 5", "Z-AI"),
            AiModel("glm-5.1", "GLM 5.1", "Z-AI"),
            AiModel("glm-5-turbo", "GLM 5 Turbo", "Z-AI"),
            AiModel("kimi-k2.5", "Kimi K2.5", "MoonshotAI"),
            AiModel("kimi-k2.6", "Kimi K2.6", "MoonshotAI"),
            AiModel("gemini-2.5-flash", "Gemini 2.5 Flash", "Google"),
            AiModel("gemini-3-flash-preview", "Gemini 3 Flash", "Google"),
            AiModel("gpt-5", "GPT 5", "DarkPS"),
            AiModel("gpt-5.3-codex", "GPT 5.3 Codex", "DarkPS"),
            AiModel("gpt-5.5", "GPT 5.5", "DarkPS"),
            AiModel("gpt-5.5-pro", "GPT 5.5 Pro", "DarkPS"),
            AiModel("gpt-oss-120b", "GPT OSS 120B", "DarkPS"),
            AiModel("qwen3-235b-a22b", "Qwen3 235B", "Qwen"),
            AiModel("qwen3-coder-480b-a35b", "Qwen3 Coder 480B", "Qwen")
        )

        val DEFAULT_MODEL = AiModel("kimi-k2.6", "Kimi K2.6", "MoonshotAI")
    }
}

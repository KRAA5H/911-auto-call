package example.agents.specialists.medical

import io.github.cdimascio.dotenv.dotenv

object Config {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    // ============ API Keys ============
    val ANTHROPIC_API_KEY: String = dotenv["ANTHROPIC_API_KEY"]
        ?: throw IllegalStateException("ANTHROPIC_API_KEY not found in .env")

    // ============ Retrieval Configuration ============
    val MAX_PROTOCOLS_FOR_SELECTION: Int = dotenv["MAX_PROTOCOLS_FOR_SELECTION"]?.toIntOrNull() ?: 20


    // ============ Safety Configuration ============
    val ENABLE_FALLBACK: Boolean = dotenv["ENABLE_FALLBACK"]?.toBoolean() ?: true
    // ============ File Paths ============
    val CRITICAL_KEYWORDS_PATH: String = dotenv["CRITICAL_KEYWORDS_PATH"]
        ?: "critical_keywords.json"

}
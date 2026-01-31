package `medical-protocol-agent`

import kotlinx.serialization.json.Json
import java.io.File

object KeywordMatcher {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var keywordMap: Map<String, Int>

    fun initialize() {
        val file = File(Config.CRITICAL_KEYWORDS_PATH)
        val content = file.readText()
        val mapping = json.decodeFromString<KeywordMapping>(content)
        keywordMap = mapping.keywords
        println("✓ Loaded ${keywordMap.size} critical keywords")
    }

    fun findProtocolId(utterance: String): Int? {
        val lowerUtterance = utterance.lowercase()

        // Check each keyword
        for ((keyword, protocolId) in keywordMap) {
            if (keyword in lowerUtterance) {
                return protocolId
            }
        }

        return null  // No match found
    }
}
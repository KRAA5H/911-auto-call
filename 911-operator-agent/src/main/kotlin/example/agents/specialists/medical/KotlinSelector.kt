package example.agents.specialists.medical

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object ClaudeSelector {
    private val json = Json { ignoreUnknownKeys = true }

    fun selectBestProtocol(
        utterance: String,
        patientContext: PatientContext,
        candidates: List<Protocol>
    ): Protocol? = runBlocking {
        if (candidates.isEmpty()) return@runBlocking null
        if (candidates.size == 1) return@runBlocking candidates.first()

        val prompt = buildSelectionPrompt(utterance, patientContext, candidates)
        val response = ClaudeClient.ask(prompt)

        // Parse the protocol ID from Claude's response
        val protocolId = parseProtocolId(response)

        return@runBlocking candidates.find { it.id == protocolId }
    }

    private fun buildSelectionPrompt(
        utterance: String,
        patientContext: PatientContext,
        candidates: List<Protocol>
    ): String {
        val candidatesList = candidates.mapIndexed { index, protocol ->
            """
            Protocol ${protocol.id}:
            - Condition: ${protocol.conditionName}
            - Category: ${protocol.conditionCategory}
            - Immediate Action: ${protocol.immediateAction}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
            You are a medical triage system. Select the most relevant emergency protocol.
            
            CALLER SAID: "$utterance"
            
            PATIENT CONTEXT:
            - Age Group: ${patientContext.ageGroup ?: "unknown"}
            - Conscious: ${patientContext.conscious ?: "unknown"}
            - Breathing: ${patientContext.breathing ?: "unknown"}
            
            AVAILABLE PROTOCOLS:
            $candidatesList
            
            TASK: Which protocol ID best matches this emergency?
            
            Return ONLY a JSON object in this exact format:
            {
              "protocolId": <number>,
              "reasoning": "<brief explanation>"
            }
        """.trimIndent()
    }

    private fun parseProtocolId(response: String): Int? {
        return try {
            // Try to parse as JSON first
            val cleanResponse = response.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val selectionResponse = json.decodeFromString<ClaudeSelectionResponse>(cleanResponse)
            selectionResponse.protocolId
        } catch (e: Exception) {
            // Fallback: try to extract any number from the response
            val numbers = Regex("""protocolId['":\s]+(\d+)""").find(response)
            numbers?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}
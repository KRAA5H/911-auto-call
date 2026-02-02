package example.agents.specialists.location

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

class LocationAgent(apiKey: String) {

    private val emergencyExportDir = "/Users/andrewrobertson/dev/911-auto-call/911-operator-agent/to_emergency"
    private val conversationHistory = mutableListOf<String>()

    private val tools = LocationTools()

    // Create the specialized registry for this agent
    private val registry = ToolRegistry {
        tools(tools.asTools())
    }

    private val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = Haiku_4_5,
        // Specialized System Prompt: It only cares about location accuracy
        systemPrompt = "You are a Location Specialist. Verify addresses or landmarks. If a location is a known landmark (like 'Tower Bridge'), accept the general coordinates and calculate the ETA to the example.main entrance/center. Do not ask for clarification unless the location is completely unknown.",
        toolRegistry = registry
    )
    
    private fun exportEmergencyReport(query: String, response: String) {
        val dir = java.io.File(emergencyExportDir)
        if (!dir.exists()) {
            dir.mkdirs()
            println("[INFO] Created directory: $emergencyExportDir")
        }
        
        try {
            val timestamp = java.time.LocalDateTime.now()
            val dateStr = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            
            // Extract location info for filename
            val locationPreview = query.take(30).replace("[^a-zA-Z0-9]+".toRegex(), "_").trim('_')
            val fileName = "LOCATION_${locationPreview}_$dateStr.txt"
            val filePath = "$emergencyExportDir/$fileName"
            
            val fileContent = buildString {
                appendLine("==== LOCATION VERIFICATION REPORT ====")
                appendLine("Timestamp: $timestamp")
                appendLine("\n--- Location Query ---")
                appendLine(query)
                appendLine("\n--- Verification Result ---")
                appendLine(response)
                appendLine("\n--- Conversation History ---")
                conversationHistory.forEach { appendLine(it) }
            }
            
            java.io.File(filePath).writeText(fileContent)
            println("[INFO] Location report exported to: $filePath")
        } catch (e: Exception) {
            println("[ERROR] Could not export location report: ${e.message}")
            e.printStackTrace()
        }
    }

    // The public method the rest of the app uses to talk to this agent
    suspend fun ask(query: String): String {
        conversationHistory.add("Query: $query")
        val response = agent.run(query)
        conversationHistory.add("Response: $response")
        
        // Export location reports for dispatch coordination
        exportEmergencyReport(query, response)
        
        return response
    }
}
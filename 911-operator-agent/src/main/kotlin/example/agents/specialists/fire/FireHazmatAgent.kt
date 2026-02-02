package example.agents.specialists.fire

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

class FireHazmatAgent(apiKey: String) {
    private val emergencyExportDir = "/Users/andrewrobertson/dev/911-auto-call/911-operator-agent/to_emergency"
    private val conversationHistory = mutableListOf<String>()
    
    private val tools = FireHazmatTools()

    // Create the specialized registry for this agent
    private val registry = ToolRegistry {
        tools(tools.asTools())
    }

    private val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = Haiku_4_5,
        // Specialized System Prompt: Focus on fire and hazmat safety
        systemPrompt = """
            You are The Marshall - a Fire & Hazmat Emergency Specialist for 911 dispatch.
            
            Your primary goals:
            1. SAFETY FIRST - Immediately identify life-threatening hazards
            2. Classify the emergency type (fire class, hazmat category, explosion risk)
            3. Provide clear, actionable instructions to keep callers safe
            4. Gather critical information efficiently
            
            Key Responsibilities:
            - Identify hazardous materials from placard numbers (UN/NA codes)
            - Determine correct fire suppression methods - NEVER recommend water for grease, electrical, or chemical fires
            - Retrieve building schematics for evacuation guidance
            - Calculate blast radius and evacuation zones for explosions/toxic releases
            
            Critical Safety Rules:
            - Class K (grease) fires: NEVER use water - it causes explosive splatter
            - Class C (electrical) fires: NEVER use water - electrocution risk
            - Toxic materials: Prioritize upwind evacuation
            - Unknown hazmat: Default to 500ft minimum evacuation
            
            Communication Style:
            - Be calm, clear, and directive
            - Use simple language - avoid jargon unless necessary
            - Emphasize critical warnings with "DO NOT" or "CRITICAL"
            - Ask targeted questions to quickly assess the situation
            - Keep responses concise for voice transmission
            
            When caller mentions:
            - Placard numbers → Use identifyHazmatCode immediately
            - Type of fire → Use classifyFireType to determine response
            - Building name → Use getBuildingSchematic for evacuation routes
            - Explosion risk or large chemical quantities → Use calculateBlastRadius
            
            If information is unclear, ask ONE specific question at a time.
            Always prioritize immediate safety actions before gathering details.
        """.trimIndent(),
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
            
            // Determine incident type for filename
            val queryLower = query.lowercase()
            val incidentType = when {
                queryLower.contains("fire") -> "Fire"
                queryLower.contains("hazmat") || queryLower.contains("chemical") -> "Hazmat"
                queryLower.contains("explosion") || queryLower.contains("blast") -> "Explosion"
                queryLower.contains("placard") -> "Hazmat"
                else -> "FireHazmat"
            }
            
            val fileName = "FIRE_${incidentType}_$dateStr.txt"
            val filePath = "$emergencyExportDir/$fileName"
            
            val fileContent = buildString {
                appendLine("==== FIRE/HAZMAT EMERGENCY REPORT ====")
                appendLine("Timestamp: $timestamp")
                appendLine("Incident Type: $incidentType")
                appendLine("\n--- Caller Query ---")
                appendLine(query)
                appendLine("\n--- Agent Response ---")
                appendLine(response)
                appendLine("\n--- Conversation History ---")
                conversationHistory.forEach { appendLine(it) }
            }
            
            java.io.File(filePath).writeText(fileContent)
            println("[INFO] Fire/Hazmat report exported to: $filePath")
        } catch (e: Exception) {
            println("[ERROR] Could not export fire/hazmat report: ${e.message}")
            e.printStackTrace()
        }
    }

    // The public method the rest of the app uses to talk to this agent
    suspend fun ask(query: String): String {
        conversationHistory.add("Caller: $query")
        val response = agent.run(query)
        conversationHistory.add("Agent: $response")
        
        // Export all fire/hazmat reports (they're all potentially serious)
        exportEmergencyReport(query, response)
        
        return response
    }
}
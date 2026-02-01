package org.example.agents.police

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import kotlinx.coroutines.runBlocking

/**
 * Police & Safety Agent Tools
 * Provides specialized tools for handling violent/threatening emergency situations
 */
@LLMDescription("A collection of tools for the Police & Safety Agent (The Officer).")
class PoliceTools : ToolSet {
    
    @Tool
    @LLMDescription("Assesses threat level and returns priority classification. Call this immediately to classify the emergency.")
    fun assess_threat_level(
        @LLMDescription("Brief summary of the threat: What is happening RIGHT NOW?") threatAnalysis: String,
        @LLMDescription("Your confidence that this is a real, active threat (0-100)") confidenceScore: Int,
        @LLMDescription("Caller's emotional state (e.g., 'extremely panicked', 'calm', 'whispering in fear')") callerState: String
    ): String {
        val analysisLower = threatAnalysis.lowercase()
        val stateLower = callerState.lowercase()
        
        // Check for active, imminent threats
        val isActiveThreat = analysisLower.contains("breaking in") ||
                           analysisLower.contains("attacking") ||
                           analysisLower.contains("shooting") ||
                           analysisLower.contains("threatening with") ||
                           analysisLower.contains("holding") ||
                           analysisLower.contains("pointing") ||
                           analysisLower.contains("weapon") ||
                           analysisLower.contains("armed")
        
        val isHighPanic = stateLower.contains("extreme") || 
                         stateLower.contains("panic") ||
                         stateLower.contains("terrified") ||
                         stateLower.contains("screaming")
        
        // Calculate danger score (1-10)
        val dangerScore = when {
            isActiveThreat && isHighPanic && confidenceScore >= 90 -> 10
            isActiveThreat && confidenceScore >= 85 -> 9
            isActiveThreat && confidenceScore >= 75 -> 8
            confidenceScore >= 80 -> 7
            confidenceScore >= 70 -> 6
            confidenceScore >= 60 -> 5
            confidenceScore >= 50 -> 4
            confidenceScore >= 30 -> 3
            confidenceScore >= 10 -> 2
            else -> 1
        }
        
        // Map to priority
        val priority = when {
            dangerScore >= 9 -> "CRITICAL"
            dangerScore >= 7 -> "HIGH"
            dangerScore >= 5 -> "MODERATE"
            dangerScore >= 3 -> "LOW"
            else -> "INSUFFICIENT_DATA"
        }
        
        // Log to console for debugging
        val assessment = "PRIORITY=$priority|CONFIDENCE=$confidenceScore|DANGER=$dangerScore"
        println("🚨 $assessment")
        
        // Return the exact format for the agent to output
        return assessment
    }
    
    @Tool
    @LLMDescription("Records suspect description for all responding units. Use when caller provides physical details.")
    fun log_suspect_description(
        @LLMDescription("Physical height description (e.g., '6 feet tall', 'short', 'average height')") height: String,
        @LLMDescription("Description of clothing worn (e.g., 'black hoodie, blue jeans')") clothing: String,
        @LLMDescription("Last known direction (e.g., 'heading north on Main St', 'fled on foot eastbound')") directionOfTravel: String
    ): String {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        )
        
        println("""
            |═══════════════════════════════════════
            |BOLO - BE ON THE LOOKOUT
            |Time: $timestamp
            |Height: $height
            |Clothing: $clothing
            |Direction: $directionOfTravel
            |═══════════════════════════════════════
        """.trimMargin())
        
        return "Suspect description broadcast to all units in the area"
    }
    
    @Tool
    @LLMDescription("Activates silent alarm protocol when caller cannot speak freely. Units approach WITHOUT SIRENS.")
    fun silent_alarm_trigger(
        @LLMDescription("Why caller is in duress (e.g., 'suspect in the room', 'hiding from intruder')") reason: String
    ): String {
        val incidentId = "SA-${System.currentTimeMillis()}"
        
        println("""
            |🚨 SILENT ALARM ACTIVATED 🚨
            |ID: $incidentId
            |Reason: $reason
            |Protocol: NO SIRENS - Stealth approach
        """.trimMargin())
        
        return "Silent alarm activated - officers approaching without sirens, do not respond verbally if unsafe"
    }
    
    @Tool
    @LLMDescription("Checks database for active restraining/protective orders. Use for domestic situations.")
    fun check_restraining_orders(
        @LLMDescription("Name of person to check (first and last name if available)") name: String,
        @LLMDescription("Address where incident is occurring") address: String
    ): String {
        val addressNormalized = address.lowercase()
        val nameNormalized = name.lowercase()
        
        // Simulate database check
        return when {
            nameNormalized.contains("john") || nameNormalized.contains("doe") -> {
                println("⚠️  RESTRAINING ORDER VIOLATION - Order #RO-2024-00552")
                "ACTIVE restraining order on file - this is a violation. Officers advised of high-risk situation"
            }
            
            addressNormalized.contains("main") || addressNormalized.contains("oak") -> {
                println("⚠️  RESTRAINING ORDER FOUND - High-risk location")
                "Multiple protective orders associated with this address - officers proceeding with extreme caution"
            }
            
            else -> {
                println("✓ No active restraining orders found")
                "No active restraining orders found in the system"
            }
        }
    }
}

/**
 * Police & Safety Agent (The Officer)
 * Specialized AI agent for handling violent, threatening, or police-related emergencies
 */
class PoliceAgent(apiKey: String) {
    
    private val emergencyExportDir = "/Users/andrewrobertson/dev/911-auto-call/911-operator-agent/to_emergency"
    private val conversationHistory = mutableListOf<String>()
    
    private val tools = PoliceTools()
    
    private val registry = ToolRegistry {
        tools(tools.asTools())
    }
    
    private val systemPromptText = """
        You are a 911 emergency classification system. You analyze caller messages and output ONLY a machine-readable status code.
        
        ═══════════════════════════════════════════════════════════
        ABSOLUTE RULE - YOU MUST FOLLOW THIS EXACTLY:
        ═══════════════════════════════════════════════════════════
        
        Your ENTIRE response must be EXACTLY ONE LINE in this format:
        
        PRIORITY=[level]|CONFIDENCE=[number]|DANGER=[number]
        
        DO NOT write anything else. NO explanations. NO questions. NO conversation.
        ONLY that single line.
        
        ═══════════════════════════════════════════════════════════
        
        STEP 1: Call the assess_threat_level tool with:
        - threatAnalysis: what you understand is happening
        - confidenceScore: 0-100 how sure you are
        - callerState: caller's emotional state
        
        STEP 2: The tool returns the exact format. Output ONLY what the tool returns.
        
        PRIORITY options: CRITICAL, HIGH, MODERATE, LOW, INSUFFICIENT_DATA
        
        Examples (your COMPLETE response):
        
        PRIORITY=HIGH|CONFIDENCE=85|DANGER=8
        
        PRIORITY=CRITICAL|CONFIDENCE=95|DANGER=10
        
        PRIORITY=INSUFFICIENT_DATA|CONFIDENCE=0|DANGER=0
        
        PRIORITY=LOW|CONFIDENCE=25|DANGER=2
        
        DO NOT ADD ANYTHING ELSE. Just call the tool and output its result.
    """.trimIndent()
    
    private val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = AnthropicModels.Haiku_4_5,
        systemPrompt = systemPromptText,
        toolRegistry = registry
    )
    
    private fun exportEmergencyReport(callerMessage: String, response: String) {
        val dir = java.io.File(emergencyExportDir)
        if (!dir.exists()) {
            dir.mkdirs()
            println("[INFO] Created directory: $emergencyExportDir")
        }
        
        try {
            val timestamp = java.time.LocalDateTime.now()
            val dateStr = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            
            // Extract priority for filename
            val priority = response.substringAfter("PRIORITY=").substringBefore("|").take(10)
            val fileName = "POLICE_${priority}_$dateStr.txt"
            val filePath = "$emergencyExportDir/$fileName"
            
            val fileContent = buildString {
                appendLine("==== POLICE EMERGENCY REPORT ====")
                appendLine("Timestamp: $timestamp")
                appendLine("Priority: $response")
                appendLine("\n--- Caller Message ---")
                appendLine(callerMessage)
                appendLine("\n--- Conversation History ---")
                conversationHistory.forEach { appendLine(it) }
            }
            
            java.io.File(filePath).writeText(fileContent)
            println("[INFO] Police report exported to: $filePath")
        } catch (e: Exception) {
            println("[ERROR] Could not export police report: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Process an emergency call through the police agent
     */
    suspend fun handleEmergency(callerMessage: String): String {
        conversationHistory.add("Caller: $callerMessage")
        val response = agent.run(callerMessage)
        conversationHistory.add("System: $response")
        
        // Extract ONLY the PRIORITY line from the response
        val priorityLine = response.lines()
            .firstOrNull { it.startsWith("PRIORITY=") }
            ?: "PRIORITY=INSUFFICIENT_DATA|CONFIDENCE=0|DANGER=0"
        
        // Export report for high priority cases
        if (priorityLine.contains("CRITICAL") || priorityLine.contains("HIGH")) {
            exportEmergencyReport(callerMessage, priorityLine)
        }
        
        return priorityLine
    }
    
    /**
     * Check if this message should trigger the police agent
     */
    fun shouldTrigger(message: String): Boolean {
        val messageLower = message.lowercase()
        val triggerKeywords = listOf(
            "weapon", "gun", "knife", "shooting", "shot",
            "intruder", "break-in", "breaking in", "breaking down", "burglar",
            "violence", "violent", "attack", "assault", "fighting",
            "domestic", "abuse", "hitting", "threatening",
            "hostage", "held at gunpoint", "armed", "duress",
            "robbery", "robbing", "stolen",
            "rape", "sexual assault",
            "restraining order", "can't talk", "help me"
        )
        
        return triggerKeywords.any { messageLower.contains(it) }
    }
}

/**
 * Example usage and testing
 */
fun main() = runBlocking {
    println("🚔 Police & Safety Agent - Testing Suite\n")
    println("=".repeat(60))
    
    val apiKey = System.getenv("ANTHROPIC_API_KEY") 
        ?: error("❌ ANTHROPIC_API_KEY not set")
    
    val policeAgent = PoliceAgent(apiKey)
    
    val testScenarios = listOf(
        "There's someone with a gun in my house! I'm hiding in the closet!",
        "My ex-boyfriend is breaking down my door, he has a restraining order",
        "I can't talk... he's in the other room... help",
        "my cat has a gun"
    )
    
    println("\n🧪 Running test scenarios...\n")
    
    testScenarios.forEachIndexed { index, scenario ->
        println("\n" + "─".repeat(60))
        println("TEST ${index + 1}: $scenario")
        println("Should Trigger: ${policeAgent.shouldTrigger(scenario)}")
        
        if (policeAgent.shouldTrigger(scenario)) {
            println("\n🤖 Operator:")
            val response = policeAgent.handleEmergency(scenario)
            println(response)
        }
        println("─".repeat(60))
    }
    
    println("\n✅ Testing complete!")
}
package example.agents.specialists.medical

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.PrintWriter

class MedicalAgent() {

    private val emergencyExportDir = "/Users/andrewrobertson/dev/911-auto-call/911-operator-agent/to_emergency"
    private val conversationHistory = mutableListOf<String>()

    val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(Config.ANTHROPIC_API_KEY),
        llmModel = AnthropicModels.Haiku_4_5
    )
    
    private fun exportEmergencyReport(utterance: String, response: MedicalResponse) {
        val dir = java.io.File(emergencyExportDir)
        if (!dir.exists()) {
            dir.mkdirs()
            println("[INFO] Created directory: $emergencyExportDir")
        }
        
        try {
            val timestamp = java.time.LocalDateTime.now()
            val dateStr = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val safeCondition = response.conditionDetected.take(25).replace("[^a-zA-Z0-9]+".toRegex(), "_").trim('_')
            val fileName = "MEDICAL_${safeCondition}_$dateStr.txt"
            val filePath = "$emergencyExportDir/$fileName"
            
            val fileContent = buildString {
                appendLine("==== MEDICAL EMERGENCY REPORT ====")
                appendLine("Timestamp: $timestamp")
                appendLine("Condition: ${response.conditionDetected}")
                appendLine("Severity: ${response.severity}")
                appendLine("Protocol ID: ${response.protocolId}")
                appendLine("\n--- Immediate Action ---")
                appendLine(response.immediateInstruction)
                appendLine("\n--- Step-by-Step Instructions ---")
                response.stepByStep.forEachIndexed { i, step -> appendLine("${i + 1}. $step") }
                appendLine("\n--- Critical Warnings ---")
                response.criticalPoints.forEach { appendLine("⚠️ $it") }
                appendLine("\n--- Caller Message ---")
                appendLine(utterance)
                appendLine("\n--- Conversation History ---")
                conversationHistory.forEach { appendLine(it) }
            }
            
            java.io.File(filePath).writeText(fileContent)
            println("[INFO] Medical report exported to: $filePath")
        } catch (e: Exception) {
            println("[ERROR] Could not export medical report: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getMedicalGuidance(
        utterance: String,
        ageGroup: String?,
        conscious: Boolean?,
        breathing: Boolean?
    ): String {
        conversationHistory.add("Caller: $utterance")
        
        val patientContext = PatientContext(
            ageGroup = ageGroup,
            conscious = conscious,
            breathing = breathing
        )

        // Find protocol
        val protocol = retrieveProtocol(utterance, patientContext)

        if (protocol == null) {
            return """
                Unable to find specific protocol for this situation.
                General advice: Stay calm, keep the patient comfortable, monitor their breathing and consciousness.
                Emergency services are on the way.
            """.trimIndent()
        }

        // Generate instructions
        val response = generateInstructions(
            protocol = protocol,
            utterance = utterance,
            patientContext = patientContext
        )
        
        // Export report for critical/high severity cases
        if (response.severity.contains("CRITICAL", ignoreCase = true) || 
            response.severity.contains("HIGH", ignoreCase = true) ||
            response.severity.contains("IMMEDIATE", ignoreCase = true)) {
            exportEmergencyReport(utterance, response)
        }

        // Format for supervisor
        val formattedResponse = """
            CONDITION: ${response.conditionDetected}
            SEVERITY: ${response.severity}
            
            IMMEDIATE ACTION: ${response.immediateInstruction}
            
            STEP-BY-STEP:
            ${response.stepByStep.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n")}
            
            CRITICAL WARNINGS:
            ${response.criticalPoints.joinToString("\n") { "⚠️ $it" }}
            
            ${response.reassurance}
        """.trimIndent()
        
        conversationHistory.add("System: $formattedResponse")
        return formattedResponse
    }
    private val json = Json { ignoreUnknownKeys = true }

    public fun selectBestProtocol(
        utterance: String,
        patientContext: PatientContext,
        candidates: List<Protocol>
    ): Protocol? = runBlocking {
        if (candidates.isEmpty()) return@runBlocking null
        if (candidates.size == 1) return@runBlocking candidates.first()

        val prompt = buildSelectionPrompt(utterance, patientContext, candidates)
        val response = agent.run(prompt)

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
            val cleanResponse = response.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val selectionResponse = json.decodeFromString<ClaudeSelectionResponse>(cleanResponse)
            selectionResponse.protocolId
        } catch (e: Exception) {
            val numbers = Regex("""protocolId['":\s]+(\d+)""").find(response)
            numbers?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    fun retrieveProtocol(
        utterance: String,
        patientContext: PatientContext
    ): Protocol? {
        val keywordProtocolId = KeywordMatcher.findProtocolId(utterance)

        if (keywordProtocolId != null) {
            return ProtocolRepository.getProtocolById(keywordProtocolId)
        }

        val candidates = ProtocolRepository.getCandidateProtocols(
            ageGroup = patientContext.ageGroup,
            limit = Config.MAX_PROTOCOLS_FOR_SELECTION
        )

        if (candidates.isEmpty()) {
            println("✗ Stage 2 MISS: No candidate protocols found")
            return null
        }

        val selectedProtocol = selectBestProtocol(
            utterance = utterance,
            patientContext = patientContext,
            candidates = candidates
        )

        return selectedProtocol
    }

    fun generateInstructions(
        protocol: Protocol,
        utterance: String,
        patientContext: PatientContext
    ): MedicalResponse {
        // Just return the protocol data directly - no Claude needed!
        return MedicalResponse(
            immediateInstruction = protocol.immediateAction,
            stepByStep = protocol.stepByStepInstructions,
            criticalPoints = protocol.criticalWarnings,
            reassurance = "Emergency services have been notified",
            protocolId = protocol.id,
            conditionDetected = protocol.conditionName,
            severity = protocol.severityLevel
        )
    }

    fun processEmergencyCall(
        utterance: String,
        patientContext: PatientContext
    ): MedicalResponse? {
        val overallStart = System.currentTimeMillis()

        // Stage 1 & 2: Find protocol
        val protocol = retrieveProtocol(utterance, patientContext)

        if (protocol == null) {
//        println("⚠️  No matching protocol found")
            val totalTime = System.currentTimeMillis() - overallStart
//        println("✓ Total processing time: ${totalTime}ms")
            return createFallbackResponse()
        }

        // Generate instructions
        val response = generateInstructions(
            protocol = protocol,
            utterance = utterance,
            patientContext = patientContext
        )

        val totalTime = System.currentTimeMillis() - overallStart
//    println("✓ Total processing time: ${totalTime}ms")
//    println("✓ Response received:")
//    println("  Condition: ${response.conditionDetected}")
//    println("  Immediate: ${response.immediateInstruction}")
//    println("\n  Steps:")
//    response.stepByStep.forEachIndexed { index, step ->
//        println("    ${index + 1}. $step")
//    }
//    println()

        return response
    }

    fun createFallbackResponse(): MedicalResponse {
        return MedicalResponse(
            immediateInstruction = "Stay calm, help is on the way",
            stepByStep = listOf(
                "Keep the patient as comfortable as possible",
                "Do not give them anything to eat or drink",
                "Monitor their breathing and consciousness level",
                "Tell me immediately if anything changes"
            ),
            criticalPoints = listOf(
                "Emergency services are already dispatched",
                "Do not move the patient unless in immediate danger"
            ),
            reassurance = "You've done the right thing by calling. Stay with them.",
            protocolId = null,
            conditionDetected = "unspecified_emergency",
            severity = "unknown"
        )
    }

    // API endpoint function for ElevenLabs to call
    suspend fun handleMedicalRequest(request: MedicalRequest): MedicalResponse? {
        return processEmergencyCall(
            utterance = request.utterance,
            patientContext = request.patientContext
        )
    }
}
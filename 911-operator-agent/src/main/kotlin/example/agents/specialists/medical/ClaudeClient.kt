package example.agents.specialists.medical

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import kotlinx.coroutines.runBlocking

object ClaudeClient {
    fun ask(prompt: String): String = runBlocking {
        // Create a NEW agent for each call
        val agent = AIAgent(
            promptExecutor = simpleAnthropicExecutor(Config.ANTHROPIC_API_KEY),
            llmModel = AnthropicModels.Haiku_4_5
        )
        agent.run(prompt)
    }
}

fun main() = runBlocking {
    println("🚀 Starting Medical Protocol Agent...")

    // Initialize components (no database!)
    ProtocolRepository.initialize()
    KeywordMatcher.initialize()

    println("✓ Medical Protocol Agent initialized successfully")
    println("\n" + "=".repeat(60))

    // Test the agent
    testAgent()
}

fun testAgent() {
    println("\n🧪 Testing Medical Protocol Agent\n")

    // Test case 1: Critical keyword match
    println("Test 1: Critical Emergency (Keyword Match)")
    println("Input: 'My dad collapsed and he's not breathing!'")

    val response1 = processEmergencyCall(
        utterance = "My dad collapsed and he's not breathing!",
        patientContext = PatientContext(
            ageGroup = "adult",
            conscious = false,
            breathing = false
        )
    )

    if (response1 != null) {
        println("✓ Response received:")
        println("  Condition: ${response1.conditionDetected}")
        println("  Immediate: ${response1.immediateInstruction}")
        println("  Steps: ${response1.stepByStep.size} steps")
    } else {
        println("✗ No protocol found")
    }

    println("\n" + "-".repeat(60))

    println("\n------------------------------------------------------------\n")

// Test 2: Non-Critical Emergency (Claude Selection)
    println("Test 2: Non-Critical Emergency (Claude Selection)")
    println("Input: 'My grandfather's chest is paining'")

    val response2 = processEmergencyCall(
        utterance = "My grandfather's chest is paining",
        patientContext = PatientContext(
            ageGroup = "adult",
            conscious = true,
            breathing = true
        )
    )

    if (response2 != null) {
        println("✓ Response received:")
        println("  Condition: ${response2.conditionDetected}")
        println("  Immediate: ${response2.immediateInstruction}")
    } else {
        println("✗ No protocol found (expected - may not have ankle protocol)")
    }

    println("\n" + "=".repeat(60))
    println("\n✓ Testing complete!")
}

fun processEmergencyCall(
    utterance: String,
    patientContext: PatientContext
): MedicalResponse? {
    val overallStart = System.currentTimeMillis()

    // Stage 1 & 2: Find protocol
    val protocol = ProtocolRetriever.retrieveProtocol(utterance, patientContext)

    if (protocol == null) {
        println("⚠️  No matching protocol found")
        val totalTime = System.currentTimeMillis() - overallStart
        println("✓ Total processing time: ${totalTime}ms")
        return createFallbackResponse()
    }

    // Generate instructions
    val response = ProtocolRetriever.generateInstructions(
        protocol = protocol,
        utterance = utterance,
        patientContext = patientContext
    )

    val totalTime = System.currentTimeMillis() - overallStart
    println("✓ Total processing time: ${totalTime}ms")
    println("✓ Response received:")
    println("  Condition: ${response.conditionDetected}")
    println("  Immediate: ${response.immediateInstruction}")
    println("\n  Steps:")
    response.stepByStep.forEachIndexed { index, step ->
        println("    ${index + 1}. $step")
    }
    println()

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
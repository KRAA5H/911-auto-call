package agents.supervisor

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

class SupervisorAgent {
    private val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: error("Missing API Key")

    // 1. Instantiate Specialists
    private val locAgent = LocationAgent(apiKey)
    private val medAgent = MedicalAgent(apiKey)
    // private val fireAgent = FireAgent(apiKey)

    // 2. Create Bridges (Tools)
    private val bridges = ToolRegistry.Companion {
        tools(LocationBridge(locAgent).asTools())
        tools(MedicalBridge(medAgent).asTools()) // New Medical Bridge
        // tools(FireBridge(fireAgent).asTools())
    }

    // 3. The Supervisor Brain
    private val agent = AIAgent.Companion(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = AnthropicModels.Haiku_4_5,
        systemPrompt = """
            You are the 911 Operations Supervisor.
            Your Goal: Coordinate the response to an emergency call.
            
            Resources:
            - Use 'consultLocationSpecialist' for address verification and ETA.
            - Use 'consultMedicalSpecialist' for CPR instructions or triage.
            - Use 'consultFireSpecialist' for hazard assessment.
            
            Protocol:
            1. Analyze the incoming transcript.
            2. Delegate tasks to the appropriate specialist tools immediately.
            3. Synthesize their technical responses into a clear, spoken response for the caller.
        """.trimIndent(),
        toolRegistry = bridges
    )

    fun handleRequest(transcript: String): String {
        return agent.run(transcript)
    }
}
package example.agents.supervisor

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import example.agents.specialists.location.LocationAgent
import example.agents.specialists.location.LocationBridge
import example.agents.specialists.medical.MedicalBridge
import example.agents.specialists.medical.MedicalProtocolSubAgent
import org.example.agents.police.PoliceAgent
import example.agents.specialists.police.PoliceBridge

class SupervisorAgent {
    private val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: error("Missing API Key")

    // Keep the executor static/shared if it's just an HTTP client wrapper to save resources.
    // If it holds state, move it inside handleRequest too.
    private val executor = simpleAnthropicExecutor(apiKey)

    suspend fun handleRequest(transcript: String): String {
        // 1. Instantiate Specialists PER REQUEST
        // If these agents extend StatefulSingleUseAIAgent, they CANNOT be class properties.
        val locAgent = LocationAgent(apiKey)
        val medAgent = MedicalProtocolSubAgent(apiKey)
        val policeAgent = PoliceAgent(apiKey)

        // 2. Create Bridges (Tools) PER REQUEST
        val bridges = ToolRegistry.Companion {
            tools(LocationBridge(locAgent).asTools())
            tools(MedicalBridge(medAgent).asTools())
            tools(PoliceBridge(policeAgent).asTools())
            // tools(FireBridge(fireAgent).asTools())
        }

        // 3. The Supervisor Brain (Fresh Instance)
        val agent = AIAgent.Companion(
            promptExecutor = executor,
            llmModel = AnthropicModels.Haiku_4_5,
            systemPrompt = """
                You are the 911 Operations Supervisor.
                Your Goal: Coordinate the response to an emergency call.
                
                Resources:
                - Use 'consultLocationSpecialist' for address verification and ETA.
                - Use 'consultMedicalSpecialist' for CPR instructions or triage.
                - Use 'consultPoliceSpecialist' for robbery, violent, criminal, or safety threats.
                - Use 'consultFireSpecialist' for hazard assessment.
                
                Protocol:
                1. Analyze the incoming transcript.
                2. Delegate tasks to the appropriate specialist tools immediately.
                3. Synthesize their technical responses into a clear, spoken response for the caller.
            """.trimIndent(),
            toolRegistry = bridges
        )

        // 4. Run the fresh agent
        return agent.run(transcript)
    }
}
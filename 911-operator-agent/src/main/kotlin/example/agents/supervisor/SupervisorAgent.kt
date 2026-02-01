package example.agents.supervisor

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import example.agents.specialists.fire.FireHazmatAgent
import example.agents.specialists.fire.FireHazmatBridge
import example.agents.specialists.location.LocationAgent
import example.agents.specialists.location.LocationBridge
import example.agents.specialists.medical.MedicalBridge
import example.agents.specialists.medical.MedicalAgent
import org.example.agents.police.PoliceAgent
import example.agents.specialists.police.PoliceBridge
import java.io.File
import java.io.PrintWriter

class SupervisorAgent {
    private val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: error("Missing API Key")

    // Keep the executor static/shared if it's just an HTTP client wrapper to save resources.
    // If it holds state, move it inside handleRequest too.
    private val executor = simpleAnthropicExecutor(apiKey)

    suspend fun handleRequest(transcript: String): String {
        // 1. Instantiate Specialists PER REQUEST
        // If these agents extend StatefulSingleUseAIAgent, they CANNOT be class properties.
        val locAgent = LocationAgent(apiKey)
        val medAgent = MedicalAgent(apiKey)
        val policeAgent = PoliceAgent(apiKey)
        val fireAgent = FireHazmatAgent(apiKey)

        // 2. Create Bridges (Tools) PER REQUEST
        val bridges = ToolRegistry.Companion {
            tools(LocationBridge(locAgent).asTools())
            tools(MedicalBridge(medAgent).asTools())
            tools(PoliceBridge(policeAgent).asTools())
            tools(FireHazmatBridge(fireAgent).asTools())
        }

        // 3. The Supervisor Brain (Fresh Instance)
        val agent = AIAgent.Companion(
            promptExecutor = executor,
            llmModel = AnthropicModels.Haiku_4_5,
            systemPrompt = """
            You are the 911 Operations Supervisor AI connected to an emergency operator. Your Goal: Support and coordinate emergency response and provide clear, spoken instructions to the operator, who is speaking to the caller.
            
            Resources:
            - Use 'consultLocationSpecialist' to verify addresses and get unit ETAs.
            - Use 'consultMedicalSpecialist' for medical triage and CPR instructions.
            - Use 'consultPoliceSpecialist' for crimes, violence, or immediate safety threats.
            - Use 'consultFireSpecialist' for structure fires, hazardous materials (hazmat placards), explosion risk assessments, or building floor plans.
            
            Protocol:
            1. Analyze the transcript.
            2. IMMEDIATELY delegate to the correct specialist(s). Do not guess.
            3. Synthesize their findings into a natural, spoken response.
            
            Voice Output Rules (CRITICAL):
            - DO NOT use Markdown, asterisks (**), or bullet points.
            - Speak in full, calm sentences.
            - Prioritize the most urgent instruction first (e.g., "Help is on the way. Tell caller to start CPR now.").
            - If a unit is dispatched, state the ETA clearly (e.g., "The ambulance will be there in 8 minutes.").
            - Keep it concise. Do not read raw data fields (e.g., do not read JSON output directly).
            """.trimIndent(),
            toolRegistry = bridges
        )

        // 4. Run the fresh agent
        return agent.run(transcript)
    }
}
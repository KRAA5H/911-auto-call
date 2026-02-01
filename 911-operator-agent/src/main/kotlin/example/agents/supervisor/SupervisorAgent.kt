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
import example.agents.specialists.nhs111.MedicalAssessmentAgent
import example.agents.specialists.nhs111.NHSBridge
import org.example.agents.police.PoliceAgent
import example.agents.specialists.police.PoliceBridge

class SupervisorAgent {
    private val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: error("Missing API Key")

    // Keep the executor static/shared if it's just an HTTP client wrapper to save resources.
    // If it holds state, move it inside handleRequest too.
    private val executor = simpleAnthropicExecutor(apiKey)
    
    // Store agents per conversation to maintain state
    private val conversationAgents = mutableMapOf<String, ConversationAgents>()
    
    data class ConversationAgents(
        val locAgent: LocationAgent,
        val medAgent: MedicalAgent,
        val policeAgent: PoliceAgent,
        val fireAgent: FireHazmatAgent,
        val nhsAgent: MedicalAssessmentAgent
    )

    suspend fun handleRequest(transcript: String, conversationId: String = "default"): String {ionId: String = "default"): String {
        // Get or create agents for this conversation
        val agents = conversationAgents.getOrPut(conversationId) {
            println("[INFO] Creating new agent set for conversation: $conversationId")
            ConversationAgents(
                locAgent = LocationAgent(apiKey),
                medAgent = MedicalAgent(apiKey),
                policeAgent = PoliceAgent(apiKey),
                fireAgent = FireHazmatAgent(apiKey),
                nhsAgent = MedicalAssessmentAgent(apiKey)
            )
        }

        // 2. Create Bridges (Tools) using persistent agents
        val bridges = ToolRegistry.Companion {
            tools(LocationBridge(agents.locAgent).asTools())
            tools(MedicalBridge(agents.medAgent).asTools())
            tools(PoliceBridge(agents.policeAgent).asTools())
            tools(FireHazmatBridge(agents.fireAgent).asTools())
            tools(NHSBridge(agents.nhsAgent).asTools())
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
            - Use 'consultNHS111Specialist' for non-emergency medical assessment and symptom triage to determine appropriate care level.
            
            Service Recognition (CRITICAL - Act immediately):
            
            NHS 111 / Medical Advice (consultNHS111Specialist):
            - Keywords: "NHS", "NHS 111", "medical advice", "health advice", "not sure if emergency", "should I go to hospital"
            - Symptoms mentioned without immediate danger: headache, fever, rash, stomach pain, feeling unwell, tired, dizzy, cough, sore throat
            - Questions about health concerns or what to do about symptoms
            - If caller seems uncertain whether it's an emergency
            → IMMEDIATELY call consultNHS111Specialist with the caller's concern
            
            Emergency Services:
            - "Police" or crime/violence → consultPoliceSpecialist
            - "Ambulance" or life-threatening (not breathing, chest pain, severe bleeding, unconscious) → consultMedicalSpecialist
            - "Fire" → consultFireSpecialist
            
            Protocol:
            1. Check if transcript mentions medical advice, health concerns, symptoms, or NHS.
            2. If YES and NOT immediately life-threatening: IMMEDIATELY call consultNHS111Specialist.
            3. If life-threatening emergency: call appropriate emergency specialist.
            4. If unclear: Ask which service they need.
            5. Synthesize specialist findings into a natural, spoken response.
            
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
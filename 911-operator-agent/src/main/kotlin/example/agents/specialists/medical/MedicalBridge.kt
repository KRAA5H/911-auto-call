package example.agents.specialists.medical

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Interface for contacting the Medical Protocol Department.")
class MedicalBridge(private val medicalAgent: MedicalAgent) : ToolSet {

    @Tool
    @LLMDescription("Delegates a medical emergency question to the Medical Specialist. Use this for ANY medical emergency guidance, first aid instructions, or protocol advice.")
    suspend fun consultMedicalSpecialist(
        @LLMDescription("The full description of the medical emergency (e.g. 'Person not breathing', 'Severe bleeding from arm')")
        utterance: String,

        @LLMDescription("Age group: 'adult', 'child', or 'infant' (optional)")
        ageGroup: String? = null,

        @LLMDescription("Is the patient conscious? true/false (optional)")
        conscious: Boolean? = null,

        @LLMDescription("Is the patient breathing? true/false (optional)")
        breathing: Boolean? = null
    ): String {
        println("Dispatcher is calling Medical Specialist with: '$utterance'")
        val response = medicalAgent.getMedicalGuidance(
            utterance = utterance,
            ageGroup = ageGroup,
            conscious = conscious,
            breathing = breathing
        )
        println("Medical Specialist replied.")
        return response
    }
}
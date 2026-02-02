package example.agents.specialists.nhs111

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Interface for contacting the NHS 111 Medical Assessment Service.")
class NHSBridge(private val nhsAgent: MedicalAssessmentAgent) : ToolSet {

    @Tool
    @LLMDescription("Delegates to the NHS 111 medical triage specialist for symptom assessment and guidance. Use this for non-emergency medical queries where you need to determine appropriate care level (emergency, doctor visit, or self-care).")
    suspend fun consultNHS111Specialist(
        @LLMDescription("The caller's description of their symptoms or medical concern (e.g. 'I have chest pain and feel dizzy', 'My child has a high fever for 2 days')")
        symptomDescription: String
    ): String {
        println("Dispatcher is calling NHS 111 Specialist with: '$symptomDescription'")
        
        // Process the message through the NHS agent
        val response = nhsAgent.processMessage(symptomDescription)
        
        // Check if assessment is complete
        if (nhsAgent.isAssessmentComplete()) {
            val assessment = nhsAgent.getAssessment()
            if (assessment != null) {
                println("NHS 111 Assessment completed: ${assessment.severity}")
                return buildString {
                    appendLine("NHS 111 Assessment Complete:")
                    appendLine("Severity: ${assessment.severity}")
                    appendLine("Symptoms: ${assessment.symptomSummary}")
                    appendLine("Recommendation: ${assessment.recommendation}")
                }
            }
        }
        
        println("NHS 111 Specialist replied.")
        return response
    }
}

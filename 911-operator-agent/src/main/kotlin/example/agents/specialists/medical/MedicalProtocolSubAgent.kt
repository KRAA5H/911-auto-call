package example.agents.specialists.medical

class MedicalProtocolSubAgent(private val apiKey: String) {

    suspend fun getMedicalGuidance(
        utterance: String,
        ageGroup: String?,
        conscious: Boolean?,
        breathing: Boolean?
    ): String {
        val patientContext = PatientContext(
            ageGroup = ageGroup,
            conscious = conscious,
            breathing = breathing
        )

        // Stage 1 & 2: Find protocol
        val protocol = ProtocolRetriever.retrieveProtocol(utterance, patientContext)

        if (protocol == null) {
            return """
                Unable to find specific protocol for this situation.
                General advice: Stay calm, keep the patient comfortable, monitor their breathing and consciousness.
                Emergency services are on the way.
            """.trimIndent()
        }

        // Stage 3: Generate instructions
        val response = ProtocolRetriever.generateInstructions(
            protocol = protocol,
            utterance = utterance,
            patientContext = patientContext
        )

        // Format for supervisor
        return """
            CONDITION: ${response.conditionDetected}
            SEVERITY: ${response.severity}
            
            IMMEDIATE ACTION: ${response.immediateInstruction}
            
            STEP-BY-STEP:
            ${response.stepByStep.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n")}
            
            CRITICAL WARNINGS:
            ${response.criticalPoints.joinToString("\n") { "⚠️ $it" }}
            
            ${response.reassurance}
        """.trimIndent()
    }
}
package example.agents.specialists.nhs111

/**
 * Severity levels for medical assessment
 */
enum class AssessmentSeverity {
    CRITICAL,       // Life-threatening - immediate 999/A&E
    NEEDS_DOCTOR,   // Requires doctor assessment
    SELF_CARE       // Can manage at home
}

/**
 * Medical assessment result from the NHS 111 agent
 */
data class MedicalAssessment(
    val symptomSummary: String,
    val severity: AssessmentSeverity,
    val recommendation: String,
    val conversationTranscript: List<String>
)

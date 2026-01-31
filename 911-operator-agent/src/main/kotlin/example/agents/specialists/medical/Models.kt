package `medical-protocol-agent`

import kotlinx.serialization.Serializable

// ============ Database Models ============

@Serializable
data class Protocol(
    val id: Int? = null,
    val conditionName: String,
    val conditionCategory: String,
    val severityLevel: String,
    val immediateAction: String,
    val stepByStepInstructions: List<String>,
    val criticalWarnings: List<String>,
    val ageGroups: List<String>,
    val timeCritical: Boolean,
    val sourceDocument: String? = null
)

// ============ API Request/Response Models ============

@Serializable
data class MedicalRequest(
    val utterance: String,
    val patientContext: PatientContext
)

@Serializable
data class PatientContext(
    val ageGroup: String? = null,       // "adult", "child", "infant"
    val conscious: Boolean? = null,
    val breathing: Boolean? = null,
    val otherSymptoms: List<String>? = null
)

@Serializable
data class MedicalResponse(
    val immediateInstruction: String,
    val stepByStep: List<String>,
    val criticalPoints: List<String>,
    val reassurance: String,
    val protocolId: Int?,
    val conditionDetected: String,
    val severity: String
)

// ============ Keyword Matching Models ============

@Serializable
data class KeywordMapping(
    val keywords: Map<String, Int>  // keyword → protocol_id
)

// ============ Claude Response Models ============

@Serializable
data class ClaudeSelectionResponse(
    val protocolId: Int,
    val reasoning: String
)

@Serializable
data class ClaudeGenerationResponse(
    val immediateInstruction: String,
    val stepByStep: List<String>,
    val criticalPoints: List<String>,
    val reassurance: String
)
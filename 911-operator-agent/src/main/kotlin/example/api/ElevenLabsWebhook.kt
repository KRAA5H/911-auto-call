package example.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import example.agents.supervisor.SupervisorAgent

// --- TOP LEVEL DATA CLASSES (Must be outside functions) ---

@Serializable
data class ElevenLabsToolCall(
    @SerialName("conversation_id") val conversationId: String,
    val transcript: String
)

@Serializable
data class ElevenLabsPostCallEvent(
    val type: String, // e.g. "post_call_transcription"
    val data: CallData
)

@Serializable
data class CallData(
    @SerialName("conversation_id") val conversationId: String,
    val transcript: List<TranscriptTurn>, // This matches the JSON list
    val analysis: Analysis? = null
)

@Serializable
data class TranscriptTurn(
    val role: String, // "agent" or "user"
    val message: String? = null,
    @SerialName("time_in_call_secs") val timeInCall: Int? = 0
)

@Serializable
data class Analysis(
    @SerialName("transcript_summary") val summary: String? = null
)

// --- EXTENSION FUNCTION ---

fun Application.configureElevenLabsRoutes(
    // We pass the save logic as a lambda function from Main.kt
    saveTranscript: (String, String) -> Unit
) {
    // 1. Install JSON Support
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    val supervisor = SupervisorAgent()

    routing {
        // --- ROUTE 1: Live Tool Call (Supervisor) ---
        post("/consult_supervisor") {
            println("Webhook received request...")

            try {
                val request = call.receive<ElevenLabsToolCall>()

                println("🗣️ ElevenLabs Transcript: '${request.transcript}'")
                println("📞 Conversation ID: '${request.conversation_id}'")

                val response = supervisor.handleRequest(request.transcript, request.conversation_id)

                println("Agent Response: '$response'")

                call.respondText(response)
            } catch (e: Exception) {
                // Catch serialization errors to see why it failed
                println("ERROR: ${e.localizedMessage}")
                e.printStackTrace()
                call.respondText("Server Error: ${e.localizedMessage}")
            }
        }

        // --- ROUTE 2: Post-Call Transcript (Final Summary) ---
        post("/get_transcript") {
            println("📝 Receiving Post-Call Transcript...")

            try {
                // 1. Parse the specific Post-Call structure
                val payload = call.receive<ElevenLabsPostCallEvent>()
                val callId = payload.data.conversationId

                // 2. Build the readable transcript string
                val formattedTranscript = buildString {
                    appendLine("CALL ID: $callId")
                    appendLine("SUMMARY: ${payload.data.analysis?.summary ?: "N/A"}")
                    appendLine("--------------------------------------------------")

                    payload.data.transcript.forEach { turn ->
                        // Only include turns where someone actually spoke (ignore pure tool events)
                        if (!turn.message.isNullOrBlank()) {
                            val speaker = turn.role.uppercase()
                            val time = "${turn.timeInCall}s"
                            appendLine("[$time] $speaker: ${turn.message}")
                        }
                    }
                }

                // 3. Save using the lambda passed from Main.kt
                // DO NOT use 'storage.saveTranscript'. Use the parameter 'saveTranscript' directly.
                saveTranscript(callId, formattedTranscript)

                println("✅ Transcript Saved for ID: $callId")
                call.respond(HttpStatusCode.OK)

            } catch (e: Exception) {
                println("❌ ERROR parsing webhook: ${e.localizedMessage}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Failed to parse post-call webhook")
            }
        }
    }
}
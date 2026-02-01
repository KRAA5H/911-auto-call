package example.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.* // Required for JSON
import io.ktor.serialization.kotlinx.json.* // Required for JSON
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import example.agents.supervisor.SupervisorAgent

@Serializable
data class ElevenLabsToolCall(
    val conversation_id: String,
    val transcript: String
)

fun Application.configureElevenLabsRoutes() {
    // 1. CRITICAL: Install JSON support so the server can understand the CURL request
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    // Initialize the agent (this might take 1-2 seconds on startup)
    val supervisor = SupervisorAgent()

    routing {
        post("/consult_supervisor") {
            println("📞 Webhook received request...") // Debug log 1

            try {
                // 2. This line will fail if ContentNegotiation isn't installed
                val request = call.receive<ElevenLabsToolCall>()

                println("🗣️ ElevenLabs Transcript: '${request.transcript}'")
                println("📞 Conversation ID: '${request.conversation_id}'")

                val response = supervisor.handleRequest(request.transcript, request.conversation_id)

                println("🤖 Agent Response: '$response'") // Debug log 3

                call.respondText(response)
            } catch (e: Exception) {
                // Catch serialization errors to see why it failed
                println("❌ ERROR: ${e.localizedMessage}")
                e.printStackTrace()
                call.respondText("Server Error: ${e.localizedMessage}")
            }
        }
    }
}
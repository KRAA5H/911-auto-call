package org.example.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import example.agents.supervisor.SupervisorAgent
import kotlinx.serialization.Serializable

@Serializable
data class ElevenLabsToolCall(
    val conversation_id: String,
    val transcript: String // "There is a fire at Tower Bridge"
)

fun Application.configureElevenLabsRoutes() {
    // One instance of the supervisor for the app (in production, use a Map of conversation_id -> Agent)
    val supervisor = SupervisorAgent()

    routing {
        // ElevenLabs will POST to this URL when it needs help
        post("/consult_supervisor") {
            val request = call.receive<ElevenLabsToolCall>()

            println("📞 Incoming Call: ${request.transcript}")

            // 1. Run the Main Agent Logic
            // The Main Agent will internally call Location/Medical agents as needed
            val response = supervisor.handleRequest(request.transcript)

            // 2. Return text for ElevenLabs to speak back to the user
            call.respondText(response)
        }
    }
}
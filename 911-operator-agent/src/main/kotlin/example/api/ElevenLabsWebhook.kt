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
    
    // Create test file on startup to verify file saving works
    val testDir = "/Users/andrewrobertson/dev/911-auto-call/to_doctor"
    try {
        val dir = java.io.File(testDir)
        if (!dir.exists()) {
            dir.mkdirs()
            println("✅ Created directory: $testDir")
        }
        val testFile = java.io.File("$testDir/test_${System.currentTimeMillis()}.txt")
        testFile.writeText("""
            ==== TEST FILE ====
            Created at: ${java.time.LocalDateTime.now()}
            This is a test file to verify that file saving works correctly.
            If you can see this file, the export functionality is working!
        """.trimIndent())
        println("✅ Test file created at: ${testFile.absolutePath}")
    } catch (e: Exception) {
        println("❌ Failed to create test file: ${e.message}")
        e.printStackTrace()
    }

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
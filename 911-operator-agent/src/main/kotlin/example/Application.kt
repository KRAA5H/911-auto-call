package example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import example.api.configureElevenLabsRoutes
import org.example.storage.FileStorage // Make sure to import your storage class

fun main() {
    // 1. Initialize your Storage once (Singleton-ish)
    val storage = FileStorage()

    embeddedServer(Netty, port = 8080) {
        // 2. Manually call your module and pass the lambda function
        configureElevenLabsRoutes(
            saveTranscript = { id, transcript ->
                // This connects the API route to your Storage class
                storage.saveTranscript(id, transcript)
            }
        )
    }.start(wait = true)
}
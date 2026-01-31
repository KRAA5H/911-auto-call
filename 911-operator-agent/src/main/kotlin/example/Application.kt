package example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import example.api.Application.configureElevenLabsRoutes

fun main() {
    embeddedServer(Netty, port = 8080, module = {
        configureElevenLabsRoutes()
    }).start(wait = true)
}

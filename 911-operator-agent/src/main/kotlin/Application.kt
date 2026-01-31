package org.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.example.api.configureElevenLabsRoutes

fun main() {
    embeddedServer(Netty, port = 8080) {
        configureElevenLabsRoutes()
    }.start(wait = true)
}
package site.petpic.api

import site.petpic.api.api.picture
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import site.petpic.api.plugins.configureHTTP

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    configureHTTP()
    picture()
}

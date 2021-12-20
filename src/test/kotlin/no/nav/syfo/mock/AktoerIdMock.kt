package no.nav.syfo.mock

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.client.*
import no.nav.syfo.getRandomPort

class AktoerIdMock {
    private val port = getRandomPort()
    private val path = "/identer"
    val url = "http://localhost:$port"

    val name = "aktoerid"
    val server = mockAktoerIdServer(
        port
    )

    private fun mockAktoerIdServer(
        port: Int
    ): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            installContentNegotiation()
            routing {
                get(path) {
                    val list = call.request.headers["Nav-Personidenter"]!!.split(",")
                    var i = 0
                    val responseMap = list.map {
                        it to IdentInfoResult(listOf(IdentInfo("${i++}", "", true)), null)
                    }.toMap()
                    call.respond(responseMap)
                }
            }
        }
    }
}
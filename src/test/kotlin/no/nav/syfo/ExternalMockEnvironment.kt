package no.nav.syfo

import io.ktor.server.netty.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.mock.*
import java.nio.file.Paths

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val embeddedEnvironment: KafkaEnvironment = testKafka()
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()

    val azureAdV2Mock = AzureAdV2Mock()
    val clamAvMock = ClamAvMock()
    val dokarkivMock = DokarkivMock()
    val pdlMock = PdlMock()
    val kuhrsarMock = KuhrSarMock()
    val pdfgenMock = PdfGenMock()
    val syfohelsenettproxyMock = SyfohelsenettproxyMock()
    val legeSuspensjonEndpointMock = LegeSuspensjonEndpointMock()
    val smtssMock = SmtssMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdV2Mock.name to azureAdV2Mock.server,
        clamAvMock.name to clamAvMock.server,
        dokarkivMock.name to dokarkivMock.server,
        pdlMock.name to pdlMock.server,
        kuhrsarMock.name to kuhrsarMock.server,
        pdfgenMock.name to pdfgenMock.server,
        syfohelsenettproxyMock.name to syfohelsenettproxyMock.server,
        legeSuspensjonEndpointMock.name to legeSuspensjonEndpointMock.server,
        smtssMock.name to smtssMock.server,
    )

    var environment = testEnvironment(
        kafkaBootstrapServers = embeddedEnvironment.brokersURL,
        azureTokenEndpoint = azureAdV2Mock.url,
        clamAvURL = clamAvMock.url,
        dokarkivUrl = dokarkivMock.url,
        pdlUrl = pdlMock.url,
        kuhrSarApiUrl = kuhrsarMock.url,
        pdfgenUrl = pdfgenMock.url,
        syfohelsenettproxyEndpointUrl = syfohelsenettproxyMock.url,
        legeSuspensjonEndpointUrl = legeSuspensjonEndpointMock.url,
        smtssUrl = smtssMock.url,
    )

    companion object {
        val instance: ExternalMockEnvironment by lazy {
            ExternalMockEnvironment().also {
                it.startExternalMocks()
            }
        }
    }
}

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
    this.database.start()
    this.embeddedEnvironment.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
    this.embeddedEnvironment.tearDown()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L,
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}

package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.util.*
import no.nav.syfo.util.retry
import no.nav.syfo.model.Dialogmelding
import no.nav.syfo.model.PdfModel
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.model.sanitizeForPdfGen

@KtorExperimentalAPI
class PdfgenClient constructor(
    private val url: String,
    private val httpClient: HttpClient
) {
    suspend fun createPdf(payload: PdfModel): ByteArray = retry("pdfgen") {
        val response: HttpResponse = httpClient.get(url) {
            contentType(ContentType.Application.Json)
            method = HttpMethod.Post
            body = payload.sanitizeForPdfGen()
        }
        if (response.status == HttpStatusCode.OK) {
            response.receive()
        } else {
            throw RuntimeException("Pdfgen returned http status: ${response.status}")
        }
    }
}

fun createPdfPayload(
    dialogmelding: Dialogmelding,
    validationResult: ValidationResult,
    pasientFnr: String,
    pasientNavn: String,
    navnSignerendeLege: String,
    antallVedlegg: Int,
): PdfModel = PdfModel(
    dialogmelding = dialogmelding,
    validationResult = validationResult,
    pasientFnr = pasientFnr,
    pasientNavn = pasientNavn,
    navnSignerendeLege = navnSignerendeLege,
    antallVedlegg,
)

package no.nav.syfo.handlestatus

import javax.jms.MessageProducer
import javax.jms.Session
import net.logstash.logback.argument.StructuredArguments.fields
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.apprecV1.XMLCV
import no.nav.helse.eiFellesformat2.XMLEIFellesformat
import no.nav.syfo.Environment
import no.nav.syfo.apprec.ApprecStatus
import no.nav.syfo.apprec.toApprecCV
import no.nav.syfo.client.IdentInfoResult
import no.nav.syfo.log
import no.nav.syfo.metrics.INVALID_MESSAGE_NO_NOTICE
import no.nav.syfo.metrics.TEST_FNR_IN_PROD
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.services.sendReceipt
import no.nav.syfo.services.updateRedis
import no.nav.syfo.util.LoggingMeta
import redis.clients.jedis.Jedis

fun handleStatusINVALID(
    validationResult: ValidationResult,
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    loggingMeta: LoggingMeta,
    pale2AvvistTopic: String,
    apprecQueueName: String
) {
    sendReceipt(session, receiptProducer, fellesformat, ApprecStatus.avvist,
        validationResult.ruleHits.map { it.toApprecCV() })
    log.info("Apprec Receipt sent to {}, {}", apprecQueueName, fields(loggingMeta))

    /*
    kafkaProducerLegeerklaeringSak.send(
        ProducerRecord(pale2AvvistTopic, legeerklaeringSak)
    )*/

    log.info("Melding sendt til kafka topic {}", pale2AvvistTopic)
}

fun handleDuplicateSM2013Content(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    loggingMeta: LoggingMeta,
    env: Environment,
    redisSha256String: String
) {

    log.warn("Message with {} marked as duplicate, has same redisSha256String {}",
        keyValue("originalEdiLoggId", redisSha256String), fields(loggingMeta))

    sendReceipt(
        session, receiptProducer, fellesformat, ApprecStatus.avvist, listOf(
            createApprecError(
                "Duplikat! - Denne legeerklæringen er mottatt tidligere. " +
                        "Skal ikke sendes på nytt."
            )
        )
    )
    log.info("Apprec Receipt sent to {}, {}", env.apprecQueueName, fields(loggingMeta))
    INVALID_MESSAGE_NO_NOTICE.inc()
}

fun handleDuplicateEdiloggid(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    loggingMeta: LoggingMeta,
    env: Environment,
    redisEdiloggid: String
) {

    log.warn("Message with {} marked as duplicate, has same redisEdiloggid {}",
        keyValue("originalEdiLoggId", redisEdiloggid), fields(loggingMeta))

    sendReceipt(
        session, receiptProducer, fellesformat, ApprecStatus.avvist, listOf(
            createApprecError(
                "Legeerklæringen kan ikke rettes, det må skrives en ny. Grunnet følgende:" +
                        "Denne legeerklæringen har ein identisk identifikator med ein legeerklæring som er mottatt tidligere," +
                        " og er derfor ein duplikat." +
                        " og skal ikke sendes på nytt. Dersom dette ikke stemmer, kontakt din EPJ-leverandør"
            )
        )
    )
    log.info("Apprec Receipt sent to {}, {}", env.apprecQueueName, fields(loggingMeta))
    INVALID_MESSAGE_NO_NOTICE.inc()
}

fun handlePatientNotFoundInAktorRegister(
    patientIdents: IdentInfoResult?,
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    ediLoggId: String,
    jedis: Jedis,
    sha256String: String,
    env: Environment,
    loggingMeta: LoggingMeta
) {
    log.warn("Patient not found i aktorRegister error: {}, {}",
        keyValue("errorMessage", patientIdents?.feilmelding ?: "No response for FNR"),
        fields(loggingMeta))

    sendReceipt(
        session, receiptProducer, fellesformat, ApprecStatus.avvist, listOf(
            createApprecError("Pasienten er ikkje registrert i folkeregisteret")
        )
    )

    log.info("Apprec Receipt sent to {}, {}", env.apprecQueueName, fields(loggingMeta))

    INVALID_MESSAGE_NO_NOTICE.inc()
    updateRedis(jedis, ediLoggId, sha256String)
}

fun handleDoctorNotFoundInAktorRegister(
    doctorIdents: IdentInfoResult?,
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    ediLoggId: String,
    jedis: Jedis,
    sha256String: String,
    env: Environment,
    loggingMeta: LoggingMeta
) {
    log.warn("Doctor not found i aktorRegister error: {}, {}",
        keyValue("errorMessage", doctorIdents?.feilmelding ?: "No response for FNR"),
        fields(loggingMeta))

    sendReceipt(
        session, receiptProducer, fellesformat, ApprecStatus.avvist, listOf(
            createApprecError("Legeerklæringen kan ikke rettes, det må skrives en ny. Grunnet følgende:" +
                    " Behandler er ikkje registrert i folkeregisteret")
        )
    )

    log.info("Apprec Receipt sent to {}, {}", env.apprecQueueName, fields(loggingMeta))

    INVALID_MESSAGE_NO_NOTICE.inc()
    updateRedis(jedis, ediLoggId, sha256String)
}

fun handleTestFnrInProd(
    session: Session,
    receiptProducer: MessageProducer,
    fellesformat: XMLEIFellesformat,
    ediLoggId: String,
    jedis: Jedis,
    sha256String: String,
    env: Environment,
    loggingMeta: LoggingMeta
) {
    log.warn("Test fødselsnummer er kommet inn i produksjon {}", fields(loggingMeta))

    log.warn("Avsender fodselsnummer er registert i Helsepersonellregisteret (HPR), {}",
        fields(loggingMeta))

    sendReceipt(
        session, receiptProducer, fellesformat, ApprecStatus.avvist, listOf(
            createApprecError("Legeerklæringen kan ikke rettes, det må skrives en ny. Grunnet følgende:" +
                        "AnnenFravers Arsakskode V mangler i legeerklæringen. Kontakt din EPJ-leverandør)"
            )
        )
    )
    log.info("Apprec Receipt sent to {}, {}", env.apprecQueueName, fields(loggingMeta))

    INVALID_MESSAGE_NO_NOTICE.inc()
    TEST_FNR_IN_PROD.inc()
    updateRedis(jedis, ediLoggId, sha256String)
}

fun createApprecError(textToTreater: String): XMLCV = XMLCV().apply {
    dn = textToTreater
    v = "2.16.578.1.12.4.1.1.8221"
    s = "X99"
}

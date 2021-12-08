package no.nav.syfo.application.services

import com.ctc.wstx.exc.WstxException
import java.io.ByteArrayOutputStream
import java.io.IOException
import net.logstash.logback.argument.StructuredArguments
import no.nav.emottak.subscription.StartSubscriptionRequest
import no.nav.emottak.subscription.SubscriptionPort
import no.nav.helse.eiFellesformat2.XMLMottakenhetBlokk
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLSender
import no.nav.syfo.client.SamhandlerPraksis
import no.nav.syfo.util.retry
import no.nav.syfo.logger
import no.nav.syfo.util.LoggingMeta
import no.nav.syfo.util.senderMarshaller

// This functionality is only necessary due to sending out dialogMelding and oppfølginsplan to doctor
suspend fun startSubscription(
    subscriptionEmottak: SubscriptionPort,
    samhandlerPraksis: SamhandlerPraksis,
    msgHead: XMLMsgHead,
    receiverBlock: XMLMottakenhetBlokk,
    loggingMeta: LoggingMeta
) {
    logger.info("SamhandlerPraksis is found, name: ${samhandlerPraksis.navn},  {}", StructuredArguments.fields(loggingMeta))
    retry(
        callName = "start_subscription_emottak",
        retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L),
        legalExceptions = *arrayOf(IOException::class, WstxException::class)
    ) {
        subscriptionEmottak.startSubscription(
            StartSubscriptionRequest().apply {
                key = samhandlerPraksis.tss_ident
                data = convertSenderToBase64(msgHead.msgInfo.sender)
                partnerid = receiverBlock.partnerReferanse.toInt()
            }
        )
    }
}

fun isLegevakt(samhandlerPraksis: SamhandlerPraksis): Boolean {
    val kode = samhandlerPraksis.samh_praksis_type_kode
    return !kode.isNullOrEmpty() && (kode == "LEVA" || kode == "LEKO")
}

fun isNotLegevakt(samhandlerPraksis: SamhandlerPraksis): Boolean = !isLegevakt(samhandlerPraksis)

fun convertSenderToBase64(sender: XMLSender): ByteArray =
    ByteArrayOutputStream().use {
        senderMarshaller.marshal(sender, it)
        it
    }.toByteArray()

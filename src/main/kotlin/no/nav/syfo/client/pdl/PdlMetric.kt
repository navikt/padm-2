package no.nav.syfo.client.pdl

import io.prometheus.client.Counter
import no.nav.syfo.metrics.METRICS_NS

const val CALL_PDL_BASE = "call_pdl"
const val CALL_PDL_IDENTER_SUCCESS = "${CALL_PDL_BASE}_success_count"
const val CALL_PDL_IDENTER_FAIL = "${METRICS_NS}_call_pdl_fail_count"

val COUNT_CALL_PDL_IDENTER_SUCCESS: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(CALL_PDL_IDENTER_SUCCESS)
    .help("Counts the number of successful calls to pdl")
    .register()

val COUNT_CALL_PDL_IDENTER_FAIL: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(CALL_PDL_IDENTER_FAIL)
    .help("Counts the number of failed calls to pdl")
    .register()

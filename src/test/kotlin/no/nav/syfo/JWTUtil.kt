package no.nav.syfo

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import no.nav.syfo.application.api.authentication.JWT_CLAIM_AZP
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

const val keyId = "localhost-signer"

// Mock of JWT-token supplied by AzureAD. KeyId must match kid i jwkset.json
fun generateJWT(
    audience: String,
    azp: String,
    issuer: String,
    subject: String? = null,
    expiry: LocalDateTime? = LocalDateTime.now().plusHours(1),
    pid: String? = null,
): String {
    val now = Date()
    val key = getDefaultRSAKey()
    val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

    return JWT.create()
        .withKeyId(keyId)
        .withSubject(subject ?: "subject")
        .withIssuer(issuer)
        .withAudience(audience)
        .withJWTId(UUID.randomUUID().toString())
        .withClaim("ver", "1.0")
        .withClaim("nonce", "myNonce")
        .withClaim("auth_time", now)
        .withClaim("nbf", now)
        .withClaim("iat", now)
        .withClaim("exp", Date.from(expiry?.atZone(ZoneId.systemDefault())?.toInstant()))
        .withClaim("pid", pid)
        .withClaim(JWT_CLAIM_AZP, azp)
        .sign(alg)
}

fun getDefaultRSAKey(): RSAKey {
    return getJWKSet().getKeyByKeyId(keyId) as RSAKey
}

private fun getJWKSet(): JWKSet {
    val jwkSet = getJwtFileAsString("src/test/resources/jwkset.json")
    try {
        return JWKSet.parse(jwkSet)
    } catch (io: IOException) {
        throw RuntimeException(io)
    } catch (io: ParseException) {
        throw RuntimeException(io)
    }
}

private fun getJwtFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)

package com.example.tripzy_backend.auth.service

import com.example.tripzy_backend.auth.dto.GoogleTokenPayload
import com.example.tripzy_backend.common.exception.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GoogleTokenVerifierService(
    @Value("\${app.google.client-id:}")
    private val expectedClientId: String
) {

    private val log = LoggerFactory.getLogger(GoogleTokenVerifierService::class.java)
    private val restClient = RestClient.builder()
        .baseUrl("https://oauth2.googleapis.com")
        .build()

    fun verifyToken(idToken: String): GoogleTokenPayload {
        try {
            @Suppress("UNCHECKED_CAST")
            val response = restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/tokeninfo")
                        .queryParam("id_token", idToken)
                        .build()
                }
                .retrieve()
                .body(Map::class.java) as? Map<String, Any>
                ?: throw UnauthorizedException("Invalid Google ID token response")

            val sub = response["sub"] as? String
                ?: throw UnauthorizedException("Google token missing 'sub' subject claim")
            val email = response["email"] as? String
                ?: throw UnauthorizedException("Google token missing 'email' claim")
            val name = (response["name"] as? String) ?: email.substringBefore("@")
            val picture = response["picture"] as? String
            val emailVerified = (response["email_verified"] as? String)?.toBooleanStrictOrNull()
                ?: (response["email_verified"] as? Boolean)
                ?: true

            if (!emailVerified) {
                throw UnauthorizedException("Google email is not verified")
            }

            if (expectedClientId.isNotBlank()) {
                val aud = response["aud"] as? String
                if (aud != expectedClientId) {
                    log.warn("Google token audience mismatch. Expected: $expectedClientId, Got: $aud")
                    throw UnauthorizedException("Google token client ID mismatch")
                }
            }

            return GoogleTokenPayload(
                sub = sub,
                email = email,
                name = name,
                picture = picture,
                emailVerified = emailVerified
            )
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to verify Google token with Google OAuth API: ${e.message}", e)
            throw UnauthorizedException("Invalid or expired Google ID token")
        }
    }
}

package com.example.tripzy_backend.idempotency.service

import com.example.tripzy_backend.common.exception.ConflictException
import com.example.tripzy_backend.idempotency.model.IdempotencyRecord
import com.example.tripzy_backend.idempotency.repository.IdempotencyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

@Service
class IdempotencyService(
    private val idempotencyRepository: IdempotencyRepository,
    private val objectMapper: ObjectMapper
) {

    fun hashRequest(requestObj: Any): String {
        val json = objectMapper.writeValueAsString(requestObj)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(json.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun findExisting(key: String, tourId: String, currentRequestHash: String): String? {
        val record = idempotencyRepository.findByIdempotencyKeyAndTourId(key, tourId)
        if (record.isPresent) {
            val existing = record.get()
            if (existing.requestHash != currentRequestHash) {
                throw ConflictException("Idempotency key '$key' was already used with different request parameters")
            }
            return existing.cachedResponseBody
        }
        return null
    }

    fun saveResponse(key: String, tourId: String, requestHash: String, responseObj: Any, statusCode: Int = 200) {
        val json = objectMapper.writeValueAsString(responseObj)
        val record = IdempotencyRecord(
            idempotencyKey = key,
            tourId = tourId,
            requestHash = requestHash,
            cachedResponseBody = json,
            statusCode = statusCode,
            createdAt = Instant.now()
        )
        idempotencyRepository.save(record)
    }
}

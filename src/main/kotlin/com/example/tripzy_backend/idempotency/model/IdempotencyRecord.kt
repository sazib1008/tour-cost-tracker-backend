package com.example.tripzy_backend.idempotency.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "idempotency_keys")
@CompoundIndex(name = "key_tour_unique_idx", def = "{'idempotencyKey': 1, 'tourId': 1}", unique = true)
data class IdempotencyRecord(
    @Id
    var id: String? = null,

    var idempotencyKey: String,

    var tourId: String,

    var requestHash: String,

    var cachedResponseBody: String,

    var statusCode: Int = 200,

    @Indexed(expireAfter = "24h")
    var createdAt: Instant = Instant.now()
)

package com.example.tripzy_backend.idempotency.repository

import com.example.tripzy_backend.idempotency.model.IdempotencyRecord
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface IdempotencyRepository : MongoRepository<IdempotencyRecord, String> {
    fun findByIdempotencyKeyAndTourId(idempotencyKey: String, tourId: String): Optional<IdempotencyRecord>
}

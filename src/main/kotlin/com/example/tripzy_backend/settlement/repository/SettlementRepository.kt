package com.example.tripzy_backend.settlement.repository

import com.example.tripzy_backend.settlement.model.Settlement
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SettlementRepository : MongoRepository<Settlement, String> {
    fun findByTourIdOrderBySettledAtDesc(tourId: String): List<Settlement>
    fun deleteByTourId(tourId: String)
}

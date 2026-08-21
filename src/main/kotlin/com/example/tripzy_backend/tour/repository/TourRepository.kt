package com.example.tripzy_backend.tour.repository

import com.example.tripzy_backend.tour.model.Tour
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TourRepository : MongoRepository<Tour, String> {
    fun findByInviteCode(inviteCode: String): Optional<Tour>
    fun existsByInviteCode(inviteCode: String): Boolean
}

package com.example.tripzy_backend.tour.repository

import com.example.tripzy_backend.tour.model.TourMember
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TourMemberRepository : MongoRepository<TourMember, String> {
    fun findByTourId(tourId: String): List<TourMember>
    fun findByUserId(userId: String): List<TourMember>
    fun findByTourIdAndUserId(tourId: String, userId: String): Optional<TourMember>
    fun existsByTourIdAndUserId(tourId: String, userId: String): Boolean
    fun deleteByTourIdAndUserId(tourId: String, userId: String)
    fun countByTourId(tourId: String): Long
}

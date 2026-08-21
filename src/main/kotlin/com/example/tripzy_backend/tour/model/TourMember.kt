package com.example.tripzy_backend.tour.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "tour_members")
@CompoundIndex(name = "tour_user_unique_idx", def = "{'tourId': 1, 'userId': 1}", unique = true)
data class TourMember(
    @Id
    var id: String? = null,

    @Indexed
    var tourId: String,

    @Indexed
    var userId: String,

    var role: TourRole = TourRole.MEMBER,

    var joinedAt: Instant = Instant.now()
)

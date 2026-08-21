package com.example.tripzy_backend.tour.model

import com.example.tripzy_backend.common.domain.BaseEntity
import com.example.tripzy_backend.common.domain.TourStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "tours")
data class Tour(
    @Id
    var id: String? = null,

    var title: String,

    var description: String? = null,

    var baseCurrency: String = "BDT",

    var status: TourStatus = TourStatus.ACTIVE,

    @Indexed(unique = true)
    var inviteCode: String,

    @Indexed
    var createdBy: String,

    var archivedAt: Instant? = null
) : BaseEntity() {

    fun isArchived(): Boolean = status == TourStatus.ARCHIVED

    fun archive() {
        status = TourStatus.ARCHIVED
        archivedAt = Instant.now()
    }
}

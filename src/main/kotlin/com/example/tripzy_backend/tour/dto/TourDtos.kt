package com.example.tripzy_backend.tour.dto

import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.tour.model.TourRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateTourRequest(
    @field:NotBlank(message = "Tour title is required")
    @field:Size(min = 2, max = 100, message = "Tour title must be between 2 and 100 characters")
    val title: String,

    val description: String? = null,

    val baseCurrency: String = "BDT"
)

data class UpdateTourRequest(
    @field:NotBlank(message = "Tour title is required")
    @field:Size(min = 2, max = 100, message = "Tour title must be between 2 and 100 characters")
    val title: String,

    val description: String? = null
)

data class JoinTourRequest(
    @field:NotBlank(message = "Invite code is required")
    val inviteCode: String
)

data class AddMemberRequest(
    val email: String? = null,
    val userId: String? = null,
    val role: TourRole = TourRole.MEMBER
) {
    init {
        require(!email.isNullOrBlank() || !userId.isNullOrBlank()) {
            "Either email or userId must be provided"
        }
    }
}

data class UpdateTourStatusRequest(
    val status: TourStatus
)

data class TourSummaryDto(
    val id: String,
    val title: String,
    val description: String?,
    val baseCurrency: String,
    val status: TourStatus,
    val inviteCode: String,
    val memberCount: Long,
    val createdBy: String,
    val createdAt: Instant,
    val archivedAt: Instant?
)

data class TourMemberDetailDto(
    val id: String,
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val role: TourRole,
    val joinedAt: Instant
)

data class TourDetailDto(
    val id: String,
    val title: String,
    val description: String?,
    val baseCurrency: String,
    val status: TourStatus,
    val inviteCode: String,
    val createdBy: String,
    val members: List<TourMemberDetailDto>,
    val createdAt: Instant,
    val archivedAt: Instant?
)

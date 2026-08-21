package com.example.tripzy_backend.tour.service

import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.ConflictException
import com.example.tripzy_backend.common.exception.ForbiddenException
import com.example.tripzy_backend.common.exception.ResourceNotFoundException
import com.example.tripzy_backend.common.exception.TourArchivedException
import com.example.tripzy_backend.tour.dto.AddMemberRequest
import com.example.tripzy_backend.tour.dto.CreateTourRequest
import com.example.tripzy_backend.tour.dto.JoinTourRequest
import com.example.tripzy_backend.tour.dto.TourDetailDto
import com.example.tripzy_backend.tour.dto.TourMemberDetailDto
import com.example.tripzy_backend.tour.dto.TourSummaryDto
import com.example.tripzy_backend.tour.dto.UpdateTourRequest
import com.example.tripzy_backend.tour.dto.UpdateTourStatusRequest
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.tour.model.TourMember
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

@Service
class TourService(
    private val tourRepository: TourRepository,
    private val tourMemberRepository: TourMemberRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher
) {

    private val random = SecureRandom()
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun createTour(userId: String, request: CreateTourRequest): TourDetailDto {
        val inviteCode = generateUniqueInviteCode()

        val tour = Tour(
            title = request.title.trim(),
            description = request.description?.trim(),
            baseCurrency = request.baseCurrency.uppercase().ifBlank { "BDT" },
            status = TourStatus.ACTIVE,
            inviteCode = inviteCode,
            createdBy = userId
        )
        val savedTour = tourRepository.save(tour)
        val tourId = savedTour.id ?: throw IllegalStateException("Saved tour id cannot be null")

        val creatorMembership = TourMember(
            tourId = tourId,
            userId = userId,
            role = TourRole.ADMIN,
            joinedAt = Instant.now()
        )
        tourMemberRepository.save(creatorMembership)

        return getTourDetails(userId, tourId)
    }

    fun getUserTours(userId: String): List<TourSummaryDto> {
        val memberships = tourMemberRepository.findByUserId(userId)
        val tourIds = memberships.map { it.tourId }
        val tours = tourRepository.findAllById(tourIds)

        return tours.map { tour ->
            val tourId = tour.id ?: ""
            val memberCount = tourMemberRepository.countByTourId(tourId)
            TourSummaryDto(
                id = tourId,
                title = tour.title,
                description = tour.description,
                baseCurrency = tour.baseCurrency,
                status = tour.status,
                inviteCode = tour.inviteCode,
                memberCount = memberCount,
                createdBy = tour.createdBy,
                createdAt = tour.createdAt,
                archivedAt = tour.archivedAt
            )
        }
    }

    fun getTourDetails(userId: String, tourId: String): TourDetailDto {
        assertUserIsMember(userId, tourId)
        val tour = findTourById(tourId)
        val members = tourMemberRepository.findByTourId(tourId)
        val userIds = members.map { it.userId }
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        val memberDtos = members.map { member ->
            val user = usersMap[member.userId]
            TourMemberDetailDto(
                id = member.id ?: "",
                userId = member.userId,
                email = user?.email ?: "",
                name = user?.name ?: "Unknown User",
                avatarUrl = user?.avatarUrl,
                role = member.role,
                joinedAt = member.joinedAt
            )
        }

        return TourDetailDto(
            id = tour.id ?: "",
            title = tour.title,
            description = tour.description,
            baseCurrency = tour.baseCurrency,
            status = tour.status,
            inviteCode = tour.inviteCode,
            createdBy = tour.createdBy,
            members = memberDtos,
            createdAt = tour.createdAt,
            archivedAt = tour.archivedAt
        )
    }

    @Transactional
    fun updateTour(userId: String, tourId: String, request: UpdateTourRequest): TourDetailDto {
        assertTourActive(tourId)
        assertUserIsAdmin(userId, tourId)
        val tour = findTourById(tourId)

        tour.title = request.title.trim()
        tour.description = request.description?.trim()
        tourRepository.save(tour)

        return getTourDetails(userId, tourId)
    }

    @Transactional
    fun joinTourByCode(userId: String, request: JoinTourRequest): TourDetailDto {
        val code = request.inviteCode.trim().uppercase()
        val tour = tourRepository.findByInviteCode(code)
            .orElseThrow { ResourceNotFoundException("Tour not found with invite code: $code") }

        val tourId = tour.id ?: throw IllegalStateException("Tour id cannot be null")
        if (tour.isArchived()) {
            throw TourArchivedException(tourId)
        }

        val existingMember = tourMemberRepository.findByTourIdAndUserId(tourId, userId)
        if (existingMember.isEmpty) {
            val membership = TourMember(
                tourId = tourId,
                userId = userId,
                role = TourRole.MEMBER,
                joinedAt = Instant.now()
            )
            tourMemberRepository.save(membership)

            val joiningUser = userRepository.findById(userId).orElse(null)
            eventPublisher.publishEvent(
                com.example.tripzy_backend.notification.event.MemberJoinedEvent(
                    tourId = tourId,
                    userId = userId,
                    memberName = joiningUser?.name ?: "A new member"
                )
            )
        }

        return getTourDetails(userId, tourId)
    }

    @Transactional
    fun addMember(currentUserId: String, tourId: String, request: AddMemberRequest): TourDetailDto {
        assertTourActive(tourId)
        assertUserIsMember(currentUserId, tourId)

        val targetUser = when {
            !request.userId.isNullOrBlank() -> userRepository.findById(request.userId).orElseThrow {
                ResourceNotFoundException("User not found with id: ${request.userId}")
            }
            !request.email.isNullOrBlank() -> userRepository.findByEmail(request.email.trim()).orElseThrow {
                ResourceNotFoundException("User not found with email: ${request.email}")
            }
            else -> throw IllegalArgumentException("Either userId or email must be provided")
        }

        val targetUserId = targetUser.id ?: throw IllegalStateException("Target user id cannot be null")
        if (tourMemberRepository.existsByTourIdAndUserId(tourId, targetUserId)) {
            throw ConflictException("User '${targetUser.email}' is already a member of this tour")
        }

        val membership = TourMember(
            tourId = tourId,
            userId = targetUserId,
            role = request.role,
            joinedAt = Instant.now()
        )
        tourMemberRepository.save(membership)

        eventPublisher.publishEvent(
            com.example.tripzy_backend.notification.event.MemberJoinedEvent(
                tourId = tourId,
                userId = targetUserId,
                memberName = targetUser.name
            )
        )

        return getTourDetails(currentUserId, tourId)
    }

    @Transactional
    fun removeMember(currentUserId: String, tourId: String, targetUserId: String) {
        assertTourActive(tourId)
        val currentMembership = assertUserIsMember(currentUserId, tourId)

        if (currentUserId != targetUserId && currentMembership.role != TourRole.ADMIN) {
            throw ForbiddenException("Only an admin can remove another member from the tour")
        }

        val targetMembership = tourMemberRepository.findByTourIdAndUserId(tourId, targetUserId)
            .orElseThrow { ResourceNotFoundException("Target user is not a member of this tour") }

        if (targetMembership.role == TourRole.ADMIN) {
            val allMembers = tourMemberRepository.findByTourId(tourId)
            val adminCount = allMembers.count { it.role == TourRole.ADMIN }
            if (adminCount <= 1 && allMembers.size > 1) {
                throw ConflictException("Cannot remove the sole tour admin. Promote another member to admin first.")
            }
        }

        tourMemberRepository.deleteByTourIdAndUserId(tourId, targetUserId)
    }

    @Transactional
    fun updateTourStatus(currentUserId: String, tourId: String, request: UpdateTourStatusRequest): TourDetailDto {
        assertUserIsAdmin(currentUserId, tourId)
        val tour = findTourById(tourId)

        if (tour.status == TourStatus.ARCHIVED && request.status != TourStatus.ARCHIVED) {
            throw ConflictException("Archived tours cannot be unarchived")
        }

        val oldStatus = tour.status
        tour.status = request.status
        if (request.status == TourStatus.ARCHIVED) {
            tour.archivedAt = Instant.now()
        }
        tourRepository.save(tour)

        eventPublisher.publishEvent(
            com.example.tripzy_backend.notification.event.TourStatusChangedEvent(
                tourId = tourId,
                oldStatus = oldStatus,
                newStatus = request.status,
                changedByUserId = currentUserId
            )
        )

        return getTourDetails(currentUserId, tourId)
    }

    fun findTourById(tourId: String): Tour =
        tourRepository.findById(tourId).orElseThrow {
            ResourceNotFoundException("Tour not found with id: $tourId")
        }

    fun assertTourActive(tourId: String): Tour {
        val tour = findTourById(tourId)
        if (tour.isArchived()) {
            throw TourArchivedException(tourId)
        }
        return tour
    }

    fun assertUserIsMember(userId: String, tourId: String): TourMember =
        tourMemberRepository.findByTourIdAndUserId(tourId, userId).orElseThrow {
            ForbiddenException("User is not a member of tour: $tourId")
        }

    fun assertUserIsAdmin(userId: String, tourId: String): TourMember {
        val member = assertUserIsMember(userId, tourId)
        if (member.role != TourRole.ADMIN) {
            throw ForbiddenException("Tour admin privileges required for this action")
        }
        return member
    }

    private fun generateUniqueInviteCode(): String {
        var code: String
        do {
            val sb = java.lang.StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(codeChars[random.nextInt(codeChars.length)])
            }
            code = sb.toString()
        } while (tourRepository.existsByInviteCode(code))
        return code
    }
}

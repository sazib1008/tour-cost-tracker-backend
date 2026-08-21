package com.example.tripzy_backend.tour.service

import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.ConflictException
import com.example.tripzy_backend.common.exception.ForbiddenException
import com.example.tripzy_backend.common.exception.TourArchivedException
import com.example.tripzy_backend.tour.dto.AddMemberRequest
import com.example.tripzy_backend.tour.dto.CreateTourRequest
import com.example.tripzy_backend.tour.dto.JoinTourRequest
import com.example.tripzy_backend.tour.dto.UpdateTourStatusRequest
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.tour.model.TourMember
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class TourServiceTest {

    private val tourRepository: TourRepository = mockk()
    private val tourMemberRepository: TourMemberRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher = mockk(relaxed = true)
    private lateinit var tourService: TourService

    @BeforeEach
    fun setUp() {
        tourService = TourService(tourRepository, tourMemberRepository, userRepository, eventPublisher)
    }

    @Test
    fun `createTour should create tour and register creator as ADMIN`() {
        val request = CreateTourRequest(title = "Cox's Bazar Trip", description = "Fun weekend", baseCurrency = "BDT")
        val savedTour = Tour(
            id = "tour-1",
            title = "Cox's Bazar Trip",
            description = "Fun weekend",
            baseCurrency = "BDT",
            status = TourStatus.ACTIVE,
            inviteCode = "ABC123",
            createdBy = "user-1"
        )
        val user = User(id = "user-1", email = "u1@example.com", name = "User One")
        val membership = TourMember(id = "tm-1", tourId = "tour-1", userId = "user-1", role = TourRole.ADMIN)

        every { tourRepository.existsByInviteCode(any()) } returns false
        every { tourRepository.save(any()) } returns savedTour
        every { tourMemberRepository.save(any()) } returns membership
        every { tourMemberRepository.findByTourIdAndUserId("tour-1", "user-1") } returns Optional.of(membership)
        every { tourRepository.findById("tour-1") } returns Optional.of(savedTour)
        every { tourMemberRepository.findByTourId("tour-1") } returns listOf(membership)
        every { userRepository.findAllById(listOf("user-1")) } returns listOf(user)

        val result = tourService.createTour("user-1", request)

        assertNotNull(result)
        assertEquals("tour-1", result.id)
        assertEquals("Cox's Bazar Trip", result.title)
        assertEquals(1, result.members.size)
        assertEquals(TourRole.ADMIN, result.members[0].role)
        verify { tourMemberRepository.save(match { it.userId == "user-1" && it.role == TourRole.ADMIN }) }
    }

    @Test
    fun `getTourDetails should throw ForbiddenException when user is not a member`() {
        every { tourMemberRepository.findByTourIdAndUserId("tour-1", "stranger") } returns Optional.empty()

        assertThrows(ForbiddenException::class.java) {
            tourService.getTourDetails("stranger", "tour-1")
        }
    }

    @Test
    fun `joinTourByCode should throw TourArchivedException when tour is archived`() {
        val archivedTour = Tour(
            id = "tour-archived",
            title = "Old Trip",
            status = TourStatus.ARCHIVED,
            inviteCode = "ARCH12",
            createdBy = "u1"
        )
        every { tourRepository.findByInviteCode("ARCH12") } returns Optional.of(archivedTour)

        assertThrows(TourArchivedException::class.java) {
            tourService.joinTourByCode("user-2", JoinTourRequest("ARCH12"))
        }
    }

    @Test
    fun `addMember should throw ConflictException if user is already a member`() {
        val activeTour = Tour(id = "t1", title = "Trip", status = TourStatus.ACTIVE, inviteCode = "TRIP01", createdBy = "u1")
        val currentMember = TourMember(tourId = "t1", userId = "u1", role = TourRole.ADMIN)
        val targetUser = User(id = "u2", email = "u2@example.com", name = "User Two")

        every { tourRepository.findById("t1") } returns Optional.of(activeTour)
        every { tourMemberRepository.findByTourIdAndUserId("t1", "u1") } returns Optional.of(currentMember)
        every { userRepository.findByEmail("u2@example.com") } returns Optional.of(targetUser)
        every { tourMemberRepository.existsByTourIdAndUserId("t1", "u2") } returns true

        assertThrows(ConflictException::class.java) {
            tourService.addMember("u1", "t1", AddMemberRequest(email = "u2@example.com"))
        }
    }

    @Test
    fun `updateTourStatus should update status to ARCHIVED and set archivedAt`() {
        val activeTour = Tour(id = "t1", title = "Trip", status = TourStatus.ACTIVE, inviteCode = "TRIP01", createdBy = "u1")
        val adminMember = TourMember(tourId = "t1", userId = "u1", role = TourRole.ADMIN)
        val user = User(id = "u1", email = "u1@example.com", name = "User One")

        every { tourMemberRepository.findByTourIdAndUserId("t1", "u1") } returns Optional.of(adminMember)
        every { tourRepository.findById("t1") } returns Optional.of(activeTour)
        every { tourRepository.save(any()) } answers { firstArg() }
        every { tourMemberRepository.findByTourId("t1") } returns listOf(adminMember)
        every { userRepository.findAllById(listOf("u1")) } returns listOf(user)

        val result = tourService.updateTourStatus("u1", "t1", UpdateTourStatusRequest(status = TourStatus.ARCHIVED))

        assertEquals(TourStatus.ARCHIVED, result.status)
        assertNotNull(result.archivedAt)
    }
}

package com.example.tripzy_backend.notification.listener

import com.example.tripzy_backend.notification.event.ExpenseCreatedEvent
import com.example.tripzy_backend.notification.service.FcmPushNotificationService
import com.example.tripzy_backend.tour.model.Tour
import com.example.tripzy_backend.tour.model.TourMember
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class NotificationEventListenerTest {

    private val fcmPushNotificationService: FcmPushNotificationService = mockk(relaxed = true)
    private val tourMemberRepository: TourMemberRepository = mockk()
    private val tourRepository: TourRepository = mockk()
    private val userRepository: UserRepository = mockk()

    private lateinit var listener: NotificationEventListener

    @BeforeEach
    fun setUp() {
        listener = NotificationEventListener(
            fcmPushNotificationService = fcmPushNotificationService,
            tourMemberRepository = tourMemberRepository,
            tourRepository = tourRepository,
            userRepository = userRepository
        )
    }

    @Test
    fun `handleExpenseCreated should notify all tour members except creator`() {
        val tour = Tour(id = "t1", title = "Sajek Trip", inviteCode = "SJK123", createdBy = "u1")
        val creator = User(id = "u1", email = "u1@example.com", name = "Sazib")
        val members = listOf(
            TourMember(tourId = "t1", userId = "u1"),
            TourMember(tourId = "t1", userId = "u2"),
            TourMember(tourId = "t1", userId = "u3")
        )

        every { tourRepository.findById("t1") } returns Optional.of(tour)
        every { userRepository.findById("u1") } returns Optional.of(creator)
        every { tourMemberRepository.findByTourId("t1") } returns members

        val event = ExpenseCreatedEvent(
            tourId = "t1",
            expenseId = "e1",
            title = "Resort Booking",
            amount = 12000L,
            currency = "BDT",
            createdByUserId = "u1"
        )

        listener.handleExpenseCreated(event)

        verify {
            fcmPushNotificationService.sendNotificationToUsers(
                userIds = listOf("u2", "u3"),
                payload = match { it.title.contains("Sajek Trip") && it.body.contains("Resort Booking") }
            )
        }
    }
}

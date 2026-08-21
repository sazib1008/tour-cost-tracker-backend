package com.example.tripzy_backend.tour.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.tour.dto.CreateTourRequest
import com.example.tripzy_backend.tour.dto.JoinTourRequest
import com.example.tripzy_backend.tour.dto.TourDetailDto
import com.example.tripzy_backend.tour.dto.TourMemberDetailDto
import com.example.tripzy_backend.tour.dto.TourSummaryDto
import com.example.tripzy_backend.tour.dto.UpdateTourStatusRequest
import com.example.tripzy_backend.tour.model.TourRole
import com.example.tripzy_backend.tour.service.TourService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.Instant

class TourControllerTest {

    private val tourService: TourService = mockk()
    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private lateinit var mockMvc: MockMvc

    private val mockPrincipal = UserPrincipal(
        id = "user-123",
        email = "user@example.com",
        fullName = "Test User"
    )

    private val principalResolver = object : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java)
        }

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?
        ): Any {
            return mockPrincipal
        }
    }

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(TourController(tourService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST tours should return 201 with created tour details`() {
        val request = CreateTourRequest(title = "Sylhet Tour", description = "Tea garden tour", baseCurrency = "BDT")
        val detailDto = TourDetailDto(
            id = "tour-sylhet",
            title = "Sylhet Tour",
            description = "Tea garden tour",
            baseCurrency = "BDT",
            status = TourStatus.ACTIVE,
            inviteCode = "SYL123",
            createdBy = "user-123",
            members = listOf(
                TourMemberDetailDto(
                    id = "tm-1",
                    userId = "user-123",
                    email = "user@example.com",
                    name = "Test User",
                    avatarUrl = null,
                    role = TourRole.ADMIN,
                    joinedAt = Instant.now()
                )
            ),
            createdAt = Instant.now(),
            archivedAt = null
        )

        every { tourService.createTour("user-123", request) } returns detailDto

        mockMvc.perform(
            post("/tours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("tour-sylhet"))
            .andExpect(jsonPath("$.data.title").value("Sylhet Tour"))
    }

    @Test
    fun `GET tours should return list of user tours`() {
        val summary = TourSummaryDto(
            id = "tour-sylhet",
            title = "Sylhet Tour",
            description = null,
            baseCurrency = "BDT",
            status = TourStatus.ACTIVE,
            inviteCode = "SYL123",
            memberCount = 3,
            createdBy = "user-123",
            createdAt = Instant.now(),
            archivedAt = null
        )

        every { tourService.getUserTours("user-123") } returns listOf(summary)

        mockMvc.perform(get("/tours").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("tour-sylhet"))
            .andExpect(jsonPath("$.data[0].memberCount").value(3))
    }

    @Test
    fun `POST tours join should join active tour`() {
        val request = JoinTourRequest(inviteCode = "SYL123")
        val detailDto = TourDetailDto(
            id = "tour-sylhet",
            title = "Sylhet Tour",
            description = null,
            baseCurrency = "BDT",
            status = TourStatus.ACTIVE,
            inviteCode = "SYL123",
            createdBy = "creator-user",
            members = emptyList(),
            createdAt = Instant.now(),
            archivedAt = null
        )

        every { tourService.joinTourByCode("user-123", request) } returns detailDto

        mockMvc.perform(
            post("/tours/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("tour-sylhet"))
    }

    @Test
    fun `PATCH tours status should update status`() {
        val request = UpdateTourStatusRequest(status = TourStatus.ARCHIVED)
        val detailDto = TourDetailDto(
            id = "tour-sylhet",
            title = "Sylhet Tour",
            description = null,
            baseCurrency = "BDT",
            status = TourStatus.ARCHIVED,
            inviteCode = "SYL123",
            createdBy = "user-123",
            members = emptyList(),
            createdAt = Instant.now(),
            archivedAt = Instant.now()
        )

        every { tourService.updateTourStatus("user-123", "tour-sylhet", request) } returns detailDto

        mockMvc.perform(
            patch("/tours/tour-sylhet/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
    }
}
